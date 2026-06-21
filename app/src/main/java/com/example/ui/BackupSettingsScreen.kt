package com.example.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.BackupHelper
import com.example.MainActivity
import com.example.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupInfo by remember { mutableStateOf<BackupHelper.BackupInfo?>(null) }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    
    val appSettings = viewModel.appSettings
    var backupLocationStr by remember { mutableStateOf(appSettings.backupLocation) }
    var backupsDir = File(backupLocationStr)
    if (!backupsDir.exists()) backupsDir.mkdirs()
    
    var localBackups by remember { mutableStateOf(backupsDir.listFiles()?.toList() ?: emptyList()) }

    fun refreshBackups(dir: File = backupsDir) {
        localBackups = dir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {}
    )

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                selectedRestoreUri = uri
                scope.launch {
                    val info = BackupHelper.examineDb(context, uri)
                    if (info != null) {
                        backupInfo = info
                    } else {
                        Toast.makeText(context, "El archivo seleccionado no es válido o está dañado.", Toast.LENGTH_LONG).show()
                        selectedRestoreUri = null
                    }
                }
            }
        }
    )

    if (backupInfo != null && selectedRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { 
                backupInfo = null 
                selectedRestoreUri = null
            },
            title = { Text("Confirmar Restauración") },
            text = {
                Column {
                    Text("Detalles del respaldo:", fontWeight = FontWeight.Bold)
                    Text("Archivo: ${backupInfo!!.fileName}")
                    Text("Fecha: ${backupInfo!!.date}")
                    Text("Versión: ${backupInfo!!.version}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Contenido:", fontWeight = FontWeight.Bold)
                    Text("- ${backupInfo!!.clientCount} Clientes")
                    Text("- ${backupInfo!!.apptCount} Turnos")
                    Text("- ${backupInfo!!.paymentCount} Pagos")
                    Text("- ${backupInfo!!.serviceCount} Servicios")
                    Text("- ${backupInfo!!.absenceCount} Ausencias")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Esta acción reemplazará todos los datos actuales de la aplicación por los contenidos en el respaldo seleccionado. Los datos actuales no podrán recuperarse una vez completada la restauración. ¿Desea continuar?",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            val success = BackupHelper.restoreDb(context, selectedRestoreUri!!)
                            withContext(Dispatchers.Main) {
                                if (success) {
                                    Toast.makeText(context, "Restauración completada correctamente.", Toast.LENGTH_LONG).show()
                                    // Restart App
                                    val restartIntent = Intent(context, MainActivity::class.java)
                                    restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    context.startActivity(restartIntent)
                                    (context as? Activity)?.finish()
                                    Runtime.getRuntime().exit(0)
                                } else {
                                    Toast.makeText(context, "Error al restaurar. Archivo dañado.", Toast.LENGTH_LONG).show()
                                }
                                backupInfo = null
                                selectedRestoreUri = null
                            }
                        }
                }) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    backupInfo = null
                    selectedRestoreUri = null
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Respaldo y Restauración") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            item {
                Text("Exportar Respaldo Manual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val file = BackupHelper.createBackup(context)
                            withContext(Dispatchers.Main) {
                                if (file != null) {
                                    val successToast = Toast.makeText(context, "Respaldo creado correctamente en:\n${file.absolutePath}", Toast.LENGTH_LONG)
                                    // Make the toast show longer or center it? Standard is fine.
                                    successToast.show()
                                    refreshBackups()
                                } else {
                                    Toast.makeText(context, "Error al crear respaldo.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Crear Respaldo")
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Restaurar Respaldo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        restoreLauncher.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Restaurar Respaldo")
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Información del último respaldo", fontWeight = FontWeight.Bold)
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val lastDate = localBackups.firstOrNull()?.lastModified()
                if (lastDate != null) {
                    Text("Último respaldo realizado: ${formatter.format(Date(lastDate))}")
                } else {
                    Text("Último respaldo realizado: Ninguno")
                }
                Text("Cantidad total de respaldos almacenados: ${localBackups.size}")
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // For now UI only for automations
            item {
                Text("Respaldo Automático", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                var autoBackup by remember { mutableStateOf(appSettings.autoBackupFrequency) }
                var autoBackupTime by remember { mutableStateOf(appSettings.autoBackupTime) }
                var showTimeDialog by remember { mutableStateOf(false) }

                val timePickerState = rememberTimePickerState(
                    initialHour = autoBackupTime.split(":").getOrNull(0)?.toIntOrNull() ?: 2,
                    initialMinute = autoBackupTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
                    is24Hour = true
                )

                if (showTimeDialog) {
                    AlertDialog(
                        onDismissRequest = { showTimeDialog = false },
                        title = { Text("Hora programada") },
                        text = { TimePicker(state = timePickerState) },
                        confirmButton = {
                            TextButton(onClick = {
                                val newTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                                autoBackupTime = newTime
                                appSettings.autoBackupTime = newTime
                                com.example.NotificationHelper.scheduleAll(context)
                                showTimeDialog = false
                            }) { Text("Guardar") }
                        },
                        dismissButton = { TextButton(onClick = { showTimeDialog = false }) { Text("Cancelar") } }
                    )
                }

                val options = listOf("Desactivado", "Diario", "Semanal", "Mensual")
                options.forEach { opt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = autoBackup == opt,
                            onClick = { 
                                autoBackup = opt
                                appSettings.autoBackupFrequency = opt
                                com.example.NotificationHelper.scheduleAll(context)
                            }
                        )
                        Text(opt, modifier = Modifier.clickable { 
                            autoBackup = opt 
                            appSettings.autoBackupFrequency = opt
                            com.example.NotificationHelper.scheduleAll(context)
                        })
                    }
                }
                
                if (autoBackup != "Desactivado") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Hora programada: $autoBackupTime", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { showTimeDialog = true }.padding(vertical = 8.dp))
                    Text("La próxima ejecución dependerá de la frecuencia y la hora programada.", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Ubicación de respaldos", fontWeight = FontWeight.Bold)
                Text("Ubicación actual de respaldos:")
                val docsPath = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS).absolutePath + "/BarberApp/Backups"
                val downsPath = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath + "/BarberApp/Backups"
                
                val displayLocation = when (backupLocationStr) {
                    docsPath -> "Documentos/BarberApp/Backups"
                    downsPath -> "Descargas/BarberApp/Backups"
                    else -> backupLocationStr
                }
                Text(displayLocation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                var showLocationDialog by remember { mutableStateOf(false) }
                TextButton(onClick = { showLocationDialog = true }) {
                    Text("Cambiar Ubicación")
                }
                
                if (showLocationDialog) {
                    val locationOptions = listOf(
                        "Documentos/BarberApp/Backups" to android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS).absolutePath + "/BarberApp/Backups",
                        "Descargas/BarberApp/Backups" to android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath + "/BarberApp/Backups"
                    )
                    AlertDialog(
                        onDismissRequest = { showLocationDialog = false },
                        title = { Text("Elegir ubicación") },
                        text = {
                            Column {
                                locationOptions.forEach { opt ->
                                    TextButton(onClick = {
                                        appSettings.backupLocation = opt.second
                                        backupLocationStr = opt.second
                                        val newDir = File(opt.second)
                                        if (!newDir.exists()) newDir.mkdirs()
                                        refreshBackups(newDir)
                                        showLocationDialog = false
                                    }) { Text(opt.first) }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLocationDialog = false }) { Text("Cerrar") }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Limpieza Automática", fontWeight = FontWeight.Bold)
                var maxBackups by remember { mutableStateOf(if (appSettings.maxAutoBackups == -1) "Sin límite" else appSettings.maxAutoBackups.toString()) }
                val limitOptions = listOf("5", "10", "20", "Sin límite")
                var limitExpanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = limitExpanded,
                    onExpandedChange = { limitExpanded = !limitExpanded }
                ) {
                    OutlinedTextField(
                        value = maxBackups,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cantidad máxima de respaldos automáticos almacenados") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = limitExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = limitExpanded,
                        onDismissRequest = { limitExpanded = false }
                    ) {
                        limitOptions.forEach { limitStr ->
                            DropdownMenuItem(
                                text = { Text(limitStr) },
                                onClick = {
                                    maxBackups = limitStr
                                    limitExpanded = false
                                    appSettings.maxAutoBackups = if (limitStr == "Sin límite") -1 else limitStr.toInt()
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                val isBackupAllowed = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
                
                Text("Respaldo de Android", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (isBackupAllowed) {
                    Text("Estado actual: Activado", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text("Android puede realizar copias de seguridad automáticas y restaurar datos desde la cuenta de Google si dicha función está habilitada en el dispositivo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Estado actual: Desactivado", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
                    Text("BarberApp tiene deshabilitado el sistema Auto Backup de Android. Android no realizará copias de seguridad automáticas ni restaurará datos desde la cuenta de Google. La migración de información debe realizarse mediante los respaldos manuales de BarberApp.\n\nNota: Los archivos data_extraction_rules.xml y backup_rules.xml presentes en el proyecto no tienen efecto para copias en la nube mientras esta función esté desactivada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Información de Respaldo Local", fontWeight = FontWeight.Bold)
                Text("Carpeta actual: $displayLocation", style = MaterialTheme.typography.bodySmall)
                Text("Cantidad de respaldos existentes: ${localBackups.size}", style = MaterialTheme.typography.bodySmall)
                val lastBackupTime = if (localBackups.isNotEmpty()) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(localBackups.first().lastModified())) else "Ninguno"
                Text("Último respaldo: $lastBackupTime", style = MaterialTheme.typography.bodySmall)
                val totalSize = localBackups.sumOf { it.length() }
                Text("Tamaño total ocupado: ${totalSize / 1024} KB", style = MaterialTheme.typography.bodySmall)
                Text("Estado automático: ${if (autoBackup == "Desactivado") "Inactivo" else "Activo ($autoBackup)"}", style = MaterialTheme.typography.bodySmall)
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Herramientas de Diagnóstico", fontWeight = FontWeight.Bold)
                
                var showDiagnosisDialog by remember { mutableStateOf(false) }
                Button(onClick = { showDiagnosisDialog = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("Verificar Datos Restaurados")
                }
                
                if (showDiagnosisDialog) {
                    AlertDialog(
                        onDismissRequest = { showDiagnosisDialog = false },
                        title = { Text("Diagnóstico de Datos") },
                        text = {
                            Column {
                                Text("Instalación Limpia: ${if (appSettings.cleanInstallDetected) "No" else "Sí"}", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Origen probable de los datos:", fontWeight = FontWeight.Bold)
                                Text(appSettings.restoredDataOrigin)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (appSettings.cleanInstallDetected) {
                                    Text("Nivel de confianza: Alto", color = MaterialTheme.colorScheme.primary)
                                } else {
                                    Text("La aplicación inició como una instalación limpia.", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDiagnosisDialog = false }) { Text("Cerrar") }
                        }
                    )
                }
                
                var showWipeConfirmDialog by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showWipeConfirmDialog = true }, 
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Modo Instalación Limpia")
                }
                
                if (showWipeConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showWipeConfirmDialog = false },
                        title = { Text("Modo Instalación Limpia") },
                        text = { Text("¿Estás seguro que deseas eliminar TODOS los clientes, turnos, servicios, estadísticas y configuraciones? Esta herramienta es sólo para pruebas.") },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val db = com.example.data.AppDatabase.getDatabase(context)
                                    db.clearAllTables()
                                    
                                    appSettings.absencesList = emptyList()
                                    appSettings.notificationLogsList = emptyList()
                                    appSettings.cleanInstallDetected = false
                                    appSettings.firstRunCompleted = false
                                    appSettings.restoredDataOrigin = "Datos creados manualmente"
                                    
                                    backupsDir.listFiles()?.forEach { it.delete() }
                                    
                                    withContext(Dispatchers.Main) {
                                        refreshBackups()
                                        showWipeConfirmDialog = false
                                        Toast.makeText(context, "Instalación limpia completada.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }) {
                                Text("Eliminar Todo", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showWipeConfirmDialog = false }) { Text("Cancelar") }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Historial de respaldos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (localBackups.isEmpty()) {
                    Text("No hay respaldos locales guardados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            items(localBackups) { file ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(file.name, fontWeight = FontWeight.Bold)
                        val dt = SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date(file.lastModified()))
                        val sizeRaw = file.length()
                        val sizeStr = if (sizeRaw > 1024 * 1024) "${sizeRaw / (1024 * 1024)} MB" else "${sizeRaw / 1024} KB"
                        Text("Fecha: $dt • Tamaño: $sizeStr", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { /* Restore */
                                selectedRestoreUri = Uri.fromFile(file)
                                scope.launch {
                                    backupInfo = BackupHelper.examineDb(context, selectedRestoreUri!!)
                                }
                            }) { Text("Restaurar") }
                            TextButton(onClick = { /* Share */
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/octet-stream"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                exportLauncher.launch(Intent.createChooser(shareIntent, "Compartir Respaldo"))
                            }) { Text("Compartir") }
                            TextButton(onClick = { 
                                file.delete()
                                refreshBackups()
                            }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}
