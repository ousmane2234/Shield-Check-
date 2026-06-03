package com.shieldcheck.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shieldcheck.app.service.DeviceMonitorService
import com.shieldcheck.app.util.DeviceIdentifier

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startMonitoringService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                ShieldCheckApp()
            }
        }

        // Demander les permissions
        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
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
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            startMonitoringService()
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, DeviceMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@Composable
fun ShieldCheckApp() {
    var imei by remember { mutableStateOf("") }
    var manualIMEI by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Initialisation...") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // Vérifier l'IMEI au démarrage
        imei = DeviceIdentifier.getStoredIMEI(context) ?: "Non défini"
        showManualInput = imei == "Non défini"
        statusMessage = if (imei != "Non défini") "IMEI détecté: $imei" else "Veuillez entrer votre IMEI"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ShieldCheck",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "État de l'Appareil",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (imei != "Non défini") {
                    Text(
                        text = "IMEI: $imei",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        if (showManualInput) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enregistrement Manuel de l'IMEI",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = manualIMEI,
                        onValueChange = { manualIMEI = it },
                        label = { Text("Entrez votre IMEI") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (manualIMEI.isNotEmpty()) {
                                DeviceIdentifier.saveIMEI(context, manualIMEI)
                                DeviceIdentifier.markIMEIAsManuallySet(context)
                                imei = manualIMEI
                                showManualInput = false
                                statusMessage = "IMEI enregistré avec succès"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enregistrer l'IMEI")
                    }
                }
            }
        }

        Button(
            onClick = { /* Action supplémentaire */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            enabled = !showManualInput
        ) {
            Text("Surveillance Active")
        }
    }
}