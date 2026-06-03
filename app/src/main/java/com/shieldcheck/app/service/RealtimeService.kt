package com.shieldcheck.app.service

import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import io.github.supabase.supabase_kt.createSupabaseClient
import io.github.supabase.realtime_kt.PostgresAction
import kotlinx.coroutines.*
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import com.shieldcheck.app.MainActivity
import com.shieldcheck.app.receiver.DeviceAdminReceiver

class RealtimeService : Service() {

    companion object {
        private const val CHANNEL_ID = "shield_check_service"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "RealtimeService"
        private const val PREFS_NAME = "shield_check_prefs"
        private const val IMEI_KEY = "device_imei"
    }

    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var devicePolicyManager: DevicePolicyManager
    private var currentIMEI: String = ""
    private var isConnected = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RealtimeService créé")
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        devicePolicyManager = getSystemService<DevicePolicyManager>() ?: run {
            Log.e(TAG, "Impossible d'obtenir DevicePolicyManager")
            return
        }
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        
        currentIMEI = getDeviceIdentifier()
        Log.d(TAG, "IMEI du service: $currentIMEI")
        
        startRealtimeMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "RealtimeService onStartCommand - Service persistant")
        return START_STICKY
    }

    private fun getDeviceIdentifier(): String {
        val storedIMEI = sharedPreferences.getString(IMEI_KEY, "")
        if (!storedIMEI.isNullOrEmpty()) {
            Log.d(TAG, "IMEI récupéré du stockage: $storedIMEI")
            return storedIMEI
        }

        return try {
            val telephonyManager = getSystemService<TelephonyManager>()
            if (telephonyManager != null && !telephonyManager.imei.isNullOrEmpty()) {
                Log.d(TAG, "IMEI récupéré via TelephonyManager")
                telephonyManager.imei
            } else {
                throw Exception("TelephonyManager indisponible")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallback: génération UUID - ${e.message}")
            UUID.randomUUID().toString()
        }
    }

    private fun startRealtimeMonitoring() {
        serviceJob = serviceScope.launch {
            try {
                Log.d(TAG, "Connexion à Supabase Realtime en cours...")
                
                val supabaseUrl = "YOUR_SUPABASE_URL"
                val supabaseKey = "YOUR_SUPABASE_ANON_KEY"
                
                if (supabaseUrl == "YOUR_SUPABASE_URL") {
                    Log.e(TAG, "⚠ Configuration Supabase manquante - veuillez configurer les identifiants")
                    return@launch
                }

                val client = createSupabaseClient(
                    supabaseUrl = supabaseUrl,
                    supabaseKey = supabaseKey
                )

                val channel = client.realtime.createChannel("public:objets_voles")

                channel.onPostgresChange(
                    event = PostgresAction.ALL,
                    schema = "public",
                    table = "objets_voles"
                ) { message ->
                    val payload = message.payload.new?.jsonObject
                    if (payload != null) {
                        try {
                            val receivedIMEI = payload["imei"].toString().trim('"')
                            val status = payload["status"].toString().trim('"')
                            val ownerName = payload["owner_name"].toString().trim('"')

                            Log.d(TAG, "📡 Données reçues: IMEI=$receivedIMEI, Status=$status")

                            if (receivedIMEI == currentIMEI && status == "vole") {
                                Log.w(TAG, "🚨 ALERTE CRITIQUE: CE TÉLÉPHONE EST MARQUÉ COMME VOLÉ!")
                                handleStolenAlert(ownerName, receivedIMEI)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Erreur lors du traitement Realtime: ${e.message}")
                        }
                    }
                }

                channel.subscribe { status ->
                    Log.d(TAG, "État du canal Realtime: $status")
                    isConnected = status.toString().contains("SUBSCRIBED")
                }

                isConnected = true
                Log.d(TAG, "✓ Écoute Realtime établie")

            } catch (e: Exception) {
                Log.e(TAG, "✗ Erreur Realtime: ${e.message}")
                isConnected = false
                delay(5000)
                startRealtimeMonitoring()
            }
        }
    }

    private fun handleStolenAlert(ownerName: String, imei: String) {
        Log.w(TAG, "🔒 Exécution de l'alerte vol pour IMEI: $imei")
        showAlertNotification(ownerName, imei)
        lockDevice()
    }

    private fun lockDevice() {
        try {
            val admin = DeviceAdminReceiver::class.java
            val adminComponent = ComponentName(this, admin)

            if (devicePolicyManager.isAdminActive(adminComponent)) {
                Log.w(TAG, "🔐 VERROUILLAGE IMMÉDIAT DU TÉLÉPHONE")
                devicePolicyManager.lockNow()
            } else {
                Log.w(TAG, "⚠ Device Admin non activé - activation requise")
                requestDeviceAdminActivation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du verrouillage: ${e.message}")
        }
    }

    private fun requestDeviceAdminActivation() {
        try {
            val admin = DeviceAdminReceiver::class.java
            val adminComponent = ComponentName(this, admin)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "ShieldCheck nécessite l'accès administrateur pour protéger votre appareil en cas de vol."
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            Log.d(TAG, "Demande d'activation Device Admin envoyée")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la demande Device Admin: ${e.message}")
        }
    }

    private fun showAlertNotification(ownerName: String, imei: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚨 ALERTE SÉCURITÉ - APPAREIL VOLÉ")
            .setContentText("Propriétaire: $ownerName - Contactez-le immédiatement")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        val notificationManager = getSystemService<NotificationManager>()
        notificationManager?.notify(NOTIFICATION_ID + 1, notification)
        Log.d(TAG, "Notification d'alerte affichée")
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ShieldCheck - Surveillance Active")
            .setContentText("Écoute en temps réel: IMEI=$currentIMEI")
            .setSmallIcon(android.R.drawable.ic_notification_clear_all)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ShieldCheck Surveillance",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications de surveillance ShieldCheck"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(channel)
            Log.d(TAG, "Canal de notification créé")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "RealtimeService détruit")
        serviceJob?.cancel()
        serviceScope.cancel()
    }
}