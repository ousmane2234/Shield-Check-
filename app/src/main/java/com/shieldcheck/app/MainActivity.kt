package com.shieldcheck.app

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shieldcheck.app.service.RealtimeService

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PREFS_NAME = "shield_check_prefs"
        private const val IMEI_KEY = "device_imei"
    }

    private lateinit var sharedPreferences: SharedPreferences

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Log.d(TAG, "✓ Toutes les permissions accordées")
            startRealtimeService()
        } else {
            Log.w(TAG, "⚠ Certaines permissions refusées")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        
        setContent {
            MaterialTheme {
                ShieldCheckUI(this)
            }
        }

        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.BIND_DEVICE_ADMIN,
            Manifest.permission.ACCESS_NETWORK_STATE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Demande de ${permissionsToRequest.size} permissions")
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d(TAG, "Permissions déjà accordées")
            startRealtimeService()
        }
    }

    private fun startRealtimeService() {
        val intent = Intent(this, RealtimeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Log.d(TAG, "Service RealtimeService démarré")
    }

    fun saveIMEI(imei: String) {
        sharedPreferences.edit().putString(IMEI_KEY, imei).apply()
        Log.d(TAG, "IMEI sauvegardé: $imei")
    }

    fun getStoredIMEI(): String? {
        return sharedPreferences.getString(IMEI_KEY, null)
    }
}

@Composable
fun ShieldCheckUI(activity: MainActivity) {
    var imei by remember { mutableStateOf("") }
    var manualIMEI by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Initialisation...") }
    var isServiceRunning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val storedIMEI = activity.getStoredIMEI()
        if (storedIMEI != null && storedIMEI.isNotEmpty()) {
            imei = storedIMEI
            statusMessage = "✓ IMEI détecté: $imei"
            showManualInput = false
            isServiceRunning = true
        } else {
            statusMessage = "⚠ Entrez votre IMEI manuellement"
            showManualInput = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ShieldCheck",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "État de Surveillance",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (imei.isNotEmpty()) {
                    Text(
                        text = "IMEI: $imei",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (isServiceRunning) {
                    Text(
                        text = "✓ Service en surveillance active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (showManualInput) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enregistrement de l'IMEI",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Entrez l'IMEI de votre téléphone (15 chiffres) pour activer la surveillance.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = manualIMEI,
                        onValueChange = { manualIMEI = it },
                        label = { Text("IMEI") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (manualIMEI.isNotEmpty()) {
                                activity.saveIMEI(manualIMEI)
                                imei = manualIMEI
                                showManualInput = false
                                statusMessage = "✓ IMEI enregistré - Service activé"
                                isServiceRunning = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = manualIMEI.isNotEmpty()
                    ) {
                        Text("Enregistrer et Démarrer")
                    }
                }
            }
        }

        if (!showManualInput && imei.isNotEmpty()) {
            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = false
            ) {
                Text("✓ Surveillance Active (Arrière-plan)")
            }
        }
    }
}