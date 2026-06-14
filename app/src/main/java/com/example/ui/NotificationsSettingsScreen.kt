package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.Manifest
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.clickable
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(viewModel: MainViewModel, navController: NavController) {
    val appSettings = viewModel.appSettings
    val context = LocalContext.current

    var turnReminderEnabled by remember { mutableStateOf(appSettings.turnReminderEnabled) }
    var turnReminderMinutesText by remember { mutableStateOf(TextFieldValue(appSettings.turnReminderMinutes.toString())) }
    var dailyStartReminderEnabled by remember { mutableStateOf(appSettings.dailyStartReminderEnabled) }
    var dailyStartReminderTime by remember { mutableStateOf(appSettings.dailyStartReminderTime) }
    var absenceReminderDaysText by remember { mutableStateOf(TextFieldValue(appSettings.absenceReminderDays.toString())) }
    var absenceReminderTimeType by remember { mutableStateOf(appSettings.absenceReminderTimeType) }
    var dailySummaryEnabled by remember { mutableStateOf(appSettings.dailySummaryEnabled) }
    var dailySummaryTime by remember { mutableStateOf(appSettings.dailySummaryTime) }
    var soundEnabled by remember { mutableStateOf(appSettings.soundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(appSettings.vibrationEnabled) }

    var showStartDialog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    val timePickerStateStart = rememberTimePickerState(
        initialHour = dailyStartReminderTime.split(":").getOrNull(0)?.toIntOrNull() ?: 8,
        initialMinute = dailyStartReminderTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = true
    )
    val timePickerStateSummary = rememberTimePickerState(
        initialHour = dailySummaryTime.split(":").getOrNull(0)?.toIntOrNull() ?: 20,
        initialMinute = dailySummaryTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = true
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                turnReminderEnabled = false
                dailyStartReminderEnabled = false
                dailySummaryEnabled = false
            }
        }
    )

    LaunchedEffect(turnReminderEnabled, dailyStartReminderEnabled, dailySummaryEnabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
            (turnReminderEnabled || dailyStartReminderEnabled || dailySummaryEnabled)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Turnos
            Text("Recordatorios de Turnos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            ListItem(
                headlineContent = { Text("Activar recordatorios") },
                trailingContent = {
                    Switch(
                        checked = turnReminderEnabled,
                        onCheckedChange = { 
                            turnReminderEnabled = it
                            appSettings.turnReminderEnabled = it 
                        }
                    )
                }
            )
            if (turnReminderEnabled) {
                ListItem(
                    headlineContent = { Text("Tiempo de anticipación") },
                    supportingContent = { Text("Minutos") },
                    trailingContent = {
                        OutlinedTextField(
                            value = turnReminderMinutesText,
                            onValueChange = { 
                                turnReminderMinutesText = it
                                val parsed = it.text.toIntOrNull()
                                if (parsed != null && parsed >= 0) {
                                    appSettings.turnReminderMinutes = parsed
                                    com.example.NotificationHelper.scheduleAll(context)
                                }
                            },
                            modifier = Modifier.width(100.dp).onFocusChanged { state ->
                                if (state.isFocused) {
                                    turnReminderMinutesText = turnReminderMinutesText.copy(selection = TextRange(0, turnReminderMinutesText.text.length))
                                }
                            },
                            singleLine = true
                        )
                    }
                )
            }
            HorizontalDivider()

            // Inicio de Jornada
            Text("Inicio de Jornada", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            ListItem(
                headlineContent = { Text("Aviso matutino") },
                supportingContent = { Text("Te avisa cuántos turnos tienes hoy.") },
                trailingContent = {
                    Switch(
                        checked = dailyStartReminderEnabled,
                        onCheckedChange = { 
                            dailyStartReminderEnabled = it
                            appSettings.dailyStartReminderEnabled = it 
                        }
                    )
                }
            )
            if (dailyStartReminderEnabled) {
                ListItem(
                    headlineContent = { Text("Horario de aviso") },
                    trailingContent = {
                        Text(
                            text = dailyStartReminderTime,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showStartDialog = true }.padding(8.dp)
                        )
                    }
                )
            }
            HorizontalDivider()

            // Ausencias
            Text("Ausencias y Bloqueos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            ListItem(
                headlineContent = { Text("Días de anticipación para ausencias") },
                trailingContent = {
                    OutlinedTextField(
                        value = absenceReminderDaysText,
                        onValueChange = { 
                            absenceReminderDaysText = it
                            val parsed = it.text.toIntOrNull()
                            if (parsed != null && parsed >= 0) {
                                appSettings.absenceReminderDays = parsed
                                com.example.NotificationHelper.scheduleAll(context)
                            }
                        },
                        modifier = Modifier.width(100.dp).onFocusChanged { state ->
                            if (state.isFocused) {
                                absenceReminderDaysText = absenceReminderDaysText.copy(selection = TextRange(0, absenceReminderDaysText.text.length))
                            }
                        },
                        singleLine = true
                    )
                }
            )
            
            var timeTypeExpanded by remember { mutableStateOf(false) }
            val timeTypeMap = mapOf(
                "00:00" to "A las 00:00 del día",
                "InicioJornada" to "A la hora de inicio de jornada"
            )

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Horario de la notificación", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Define cuándo se enviarán los avisos de Ausencias y Bloqueos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = timeTypeExpanded,
                    onExpandedChange = { timeTypeExpanded = !timeTypeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = timeTypeMap[absenceReminderTimeType] ?: timeTypeMap["00:00"]!!,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeTypeExpanded) },
                        modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = timeTypeExpanded,
                        onDismissRequest = { timeTypeExpanded = false }
                    ) {
                        timeTypeMap.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    absenceReminderTimeType = key
                                    appSettings.absenceReminderTimeType = key
                                    timeTypeExpanded = false
                                    com.example.NotificationHelper.scheduleAll(context)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Si el horario calculado ya pasó al crear la ausencia o bloqueo, el aviso se enviará inmediatamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            HorizontalDivider()

            // Resumen Diario
            Text("Resumen Diario", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            ListItem(
                headlineContent = { Text("Activar resumen") },
                trailingContent = {
                    Switch(
                        checked = dailySummaryEnabled,
                        onCheckedChange = { 
                            dailySummaryEnabled = it
                            appSettings.dailySummaryEnabled = it 
                        }
                    )
                }
            )
            if (dailySummaryEnabled) {
                ListItem(
                    headlineContent = { Text("Horario") },
                    trailingContent = {
                        Text(
                            text = dailySummaryTime,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showSummaryDialog = true }.padding(8.dp)
                        )
                    }
                )
            }
            HorizontalDivider()

            // Sonido y Vibración
            Text("Ajustes del Sistema", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            ListItem(
                headlineContent = { Text("Sonido") },
                trailingContent = {
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { 
                            soundEnabled = it
                            appSettings.soundEnabled = it 
                        }
                    )
                }
            )
            ListItem(
                headlineContent = { Text("Vibración") },
                trailingContent = {
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { 
                            vibrationEnabled = it
                            appSettings.vibrationEnabled = it 
                        }
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { 
                    val intent = Intent(context, com.example.NotificationReceiver::class.java).apply {
                        putExtra("title", "Prueba de Notificación")
                        putExtra("message", "Esta es una prueba del sistema de alertas.")
                        putExtra("notificationId", 999)
                    }
                    context.sendBroadcast(intent)
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text("Probar Notificación")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showStartDialog) {
            AlertDialog(
                onDismissRequest = { showStartDialog = false },
                title = { Text("Horario de aviso matutino") },
                text = { TimePicker(state = timePickerStateStart) },
                confirmButton = {
                    TextButton(onClick = {
                        val newTime = String.format("%02d:%02d", timePickerStateStart.hour, timePickerStateStart.minute)
                        dailyStartReminderTime = newTime
                        appSettings.dailyStartReminderTime = newTime
                        com.example.NotificationHelper.scheduleAll(context)
                        showStartDialog = false
                    }) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { showStartDialog = false }) { Text("Cancelar") } }
            )
        }

        if (showSummaryDialog) {
            AlertDialog(
                onDismissRequest = { showSummaryDialog = false },
                title = { Text("Horario de resumen diario") },
                text = { TimePicker(state = timePickerStateSummary) },
                confirmButton = {
                    TextButton(onClick = {
                        val newTime = String.format("%02d:%02d", timePickerStateSummary.hour, timePickerStateSummary.minute)
                        dailySummaryTime = newTime
                        appSettings.dailySummaryTime = newTime
                        com.example.NotificationHelper.scheduleAll(context)
                        showSummaryDialog = false
                    }) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { showSummaryDialog = false }) { Text("Cancelar") } }
            )
        }
    }
}
