package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.license.LicenseManager
import com.example.license.LicenseValidationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val licenseManager = remember { LicenseManager(context) }
    
    var deviceId by remember { mutableStateOf(licenseManager.getDeviceId()) }
    var status by remember { mutableStateOf(licenseManager.getLicenseStatus()) }
    var activationDate by remember { mutableStateOf(licenseManager.getActivationDate() ?: "No activado") }
    var expirationDate by remember { mutableStateOf(licenseManager.getExpirationDate() ?: "No activado") }
    var remainingDays by remember { mutableStateOf(licenseManager.getRemainingDays()) }
    
    var showDialog by remember { mutableStateOf(false) }
    var licenseInput by remember { mutableStateOf("") }
    
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Licencia y Activación") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estado de Licencia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val statusText = when (status) {
                        LicenseManager.LicenseStatus.ACTIVE -> "Activa"
                        LicenseManager.LicenseStatus.EXPIRED -> "Vencida"
                        LicenseManager.LicenseStatus.UNLICENSED -> "No Licenciado (Modo Prueba)"
                    }
                    val statusColor = when (status) {
                        LicenseManager.LicenseStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                        LicenseManager.LicenseStatus.EXPIRED -> MaterialTheme.colorScheme.error
                        LicenseManager.LicenseStatus.UNLICENSED -> MaterialTheme.colorScheme.secondary
                    }
                    
                    Text("Estado: $statusText", color = statusColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ID del Dispositivo: $deviceId", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (status != LicenseManager.LicenseStatus.UNLICENSED) {
                        Text("Fecha de Activación: $activationDate", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Fecha de Vencimiento: $expirationDate", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        if (status == LicenseManager.LicenseStatus.ACTIVE) {
                            Text("Días Restantes: $remainingDays días", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Text("La aplicación está funcionando en modo de prueba sin licencia.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(onClick = {
                        val clip = ClipData.newPlainText("Device ID", deviceId)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "ID copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Copiar ID del Dispositivo")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Ingresar Licencia")
                    }
                }
            }
            
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Activar Aplicación") },
                    text = {
                        Column {
                            Text("Ingrese la licencia proporcionada por el desarrollador.")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = licenseInput,
                                onValueChange = { licenseInput = it.uppercase() },
                                label = { Text("Clave de Licencia") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val result = licenseManager.activateLicense(licenseInput.trim())
                            when (result) {
                                is LicenseValidationResult.Valid -> {
                                    Toast.makeText(context, "Licencia activada correctamente", Toast.LENGTH_LONG).show()
                                    showDialog = false
                                    // Refresh status
                                    status = licenseManager.getLicenseStatus()
                                    activationDate = licenseManager.getActivationDate() ?: "No activado"
                                    expirationDate = licenseManager.getExpirationDate() ?: "No activado"
                                    remainingDays = licenseManager.getRemainingDays()
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
                        }) {
                            Text("Activar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}
