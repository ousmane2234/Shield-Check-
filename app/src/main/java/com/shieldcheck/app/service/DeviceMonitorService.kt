package com.shieldcheck.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.shieldcheck.app.MainActivity
import com.shieldcheck.app.R
import com.shieldcheck.app.data.repository.StolenObjectRepository
import com.shieldcheck.app.receiver.DeviceAdminReceiver
import com.shieldcheck.app.util.DeviceIdentifier
import io.github.supabase.supabase_kt.createSupabaseClient
import io.github.supabase.supabase_kt.SupabaseClient
import kotlinx.coroutines.launch

class DeviceMonitorService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "shield_check_monitor"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "DeviceMonitorService"
    }

    private lateinit var supabaseClient: SupabaseClient
    private lateinit var repository: StolenObjectRepository
    private lateinit var devicePolicyManager: DevicePolicyManager
    private var currentIMEI: String = ""

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service créé")
        
        // Initialiser Supabase
        initSupabase()
        
        // Initialiser le gestionnaire de politiques de périphérique
        devicePolicyManager = getSystemService<DevicePolicyManager>() ?: return
        
        // Créer le canal de notification
        createNotificationChannel()
        
        // Obtenir l'IMEI du périphérique
        currentIMEI = DeviceIdentifier.getDeviceIdentifier(this)
        Log.d(TAG, "IMEI actuel: $currentIMEI")
        
        // Démarrer la surveillance en temps réel
        startMonitoring()
    }

    private fun initSupabase() {
        try {
            val supabaseUrl = "https://frsvuwpidxsxuczgwmfh.supabase.co"
            val supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZyc3Z1d3BpZHhzeHVjemd3bWZoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzMyODM0MzAsImV4cCI6MjA4ODg1OTQzMH0.ZHSIrJ5zysauA3bcMQKEJt_rKMWic8VM54HXsB7tW_I"
            
            supabaseClient = createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = supabaseKey
            )
            repository = StolenObjectRepository(supabaseClient)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'initialisation de Supabase", e)
        }
    }

    private fun startMonitoring() {
        lifecycleScope.launch {
            try {
                // Écouter les mises à jour en temps réel
                repository.startListening()
                
                repository.observeStolenObjects().collect { stolenObject ->
                    Log.d(TAG, "Objet volé détecté: ${stolenObject.imei} - Statut: ${stolenObject.status}")
                    
                    // Vérifier si cet objet correspond à ce téléphone
                    if (stolenObject.imei == currentIMEI && stolenObject.status == "vole") {
                        Log.w(TAG, "ALERTE: Ce téléphone est marqué comme volé!")
                        handleStolenDevice(stolenObject)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la surveillance", e)
            }
        }
    }

    private fun handleStolenDevice(stolenObject: com.shieldcheck.app.data.model.StolenObject) {
        // Envoyer une notification
        showStolenDeviceNotification(stolenObject)
        
        // Verrouiller le téléphone
        lockDevice()
    }

    private fun lockDevice() {
        try {
            val admin = com.shieldcheck.app.receiver.DeviceAdminReceiver::class.java
            val adminComponent = android.content.ComponentName(this, admin)
            
            if (devicePolicyManager.isAdminActive(adminComponent)) {
                Log.d(TAG, "Verrouillage du périphérique...")
                devicePolicyManager.lockNow()
            } else {
                Log.w(TAG, "Device Admin n'est pas activé")
                // Demander l'activation du Device Admin
                requestDeviceAdminActivation()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du verrouillage", e)
        }
    }

    private fun requestDeviceAdminActivation() {
        try {
            val admin = DeviceAdminReceiver::class.java
            val adminComponent = android.content.ComponentName(this, admin)
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "ShieldCheck a besoin de l'accès Device Admin pour verrouiller votre appareil en cas de vol."
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la demande d'activation Device Admin", e)
        }
    }

    private fun showStolenDeviceNotification(stolenObject: com.shieldcheck.app.data.model.StolenObject) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Appareil Marqué comme Volé")
            .setContentText("Propriétaire: ${stolenObject.owner_name}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService<NotificationManager>()
        notificationManager?.notify(NOTIFICATION_ID + 1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        Log.d(TAG, "Service démarré avec onStartCommand")
        
        // Afficher la notification de premier plan
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY
    }

    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ShieldCheck en Surveillance")
            .setContentText("Surveillance active du téléphone")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, "ShieldCheck Monitor", importance).apply {
                description = "Notifications de surveillance ShieldCheck"
            }
            val notificationManager = getSystemService<NotificationManager>()
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service détruit")
    }
}
