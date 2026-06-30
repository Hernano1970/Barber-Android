package com.example.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.license.LicenseManager
import com.example.license.LicenseValidationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseBlockedScreen(onLicenseActivated: () -> Unit) {
    val context = LocalContext.current
    val licenseManager = remember { LicenseManager(context) }
    
    var deviceId by remember { mutableStateOf(licenseManager.getDeviceId()) }
    var expirationDate by remember { mutableStateOf(licenseManager.getExpirationDate() ?: "Desconocida") }
    var status by remember { mutableStateOf(licenseManager.getLicenseStatus()) }
    
    var licenseInput by remember { mutableStateOf("") }
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    BackHandler {
        (context as? Activity)?.finish()
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aplicación Bloqueada",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val descriptionText = if (status == LicenseManager.LicenseStatus.UNLICENSED) {
                "La aplicación no tiene una licencia activa."
            } else {
                "Su licencia ha vencido el $expirationDate."
            }
            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ID del Dispositivo", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(deviceId, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val clip = ClipData.newPlainText("Device ID", deviceId)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "ID copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Copiar ID")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = licenseInput,
                onValueChange = { licenseInput = it.uppercase() },
                label = { Text("Clave de Licencia") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val result = licenseManager.activateLicense(licenseInput.trim())
                    when (result) {
                        is LicenseValidationResult.Valid -> {
                            Toast.makeText(context, "Licencia activada correctamente", Toast.LENGTH_LONG).show()
                            onLicenseActivated()
                        }
                        LicenseValidationResult.DeviceMismatch -> {
                            Toast.makeText(context, "Error: La licencia pertenece a otro dispositivo.", Toast.LENGTH_LONG).show()
                        }
                        LicenseValidationResult.InvalidDate -> {
                            Toast.makeText(context, "Error: Fecha de vencimiento inválida.", Toast.LENGTH_LONG).show()
                        }
                        LicenseValidationResult.InvalidFormat -> {
                            Toast.makeText(context, "Error: Formato de licencia incorrecto.", Toast.LENGTH_LONG).show()
                        }
                        LicenseValidationResult.InvalidSignature -> {
                            Toast.makeText(context, "Error: Firma de licencia inválida.", Toast.LENGTH_LONG).show()
                        }
                        LicenseValidationResult.AlreadyInstalled -> {
                            Toast.makeText(context, "Esta licencia ya se encuentra instalada.", Toast.LENGTH_LONG).show()
                        }
                        LicenseValidationResult.OlderLicense -> {
                            Toast.makeText(context, "La licencia ingresada corresponde a una renovación anterior y no puede reemplazar la licencia actualmente instalada.", Toast.LENGTH_LONG).show()
                        }
                        LicenseValidationResult.LicenseExpired -> {
                            Toast.makeText(context, "La licencia ingresada ya se encuentra vencida.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Activar Licencia")
            }
        }
    }
}
