package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(viewModel: MainViewModel, navController: NavController) {
    val appSettings = viewModel.appSettings
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var turnReminderEnabled by remember { mutableStateOf(appSettings.turnReminderEnabled) }
    var turnReminderMinutesText by remember { mutableStateOf(TextFieldValue(appSettings.turnReminderMinutes.toString())) }
    var turnReminderSelectedOption by remember {
        mutableStateOf(
            when (appSettings.turnReminderMinutes) {
                5 -> "5 minutos"
                10 -> "10 minutos"
                15 -> "15 minutos"
                30 -> "30 minutos"
                else -> "Personalizado"
            }
        )
    }
    var turnReminderExpanded by remember { mutableStateOf(false) }

    var dailyStartReminderEnabled by remember { mutableStateOf(appSettings.dailyStartReminderEnabled) }
    var dailyStartReminderTime by remember { mutableStateOf(appSettings.dailyStartReminderTime) }
    
    var absenceNotificationsEnabled by remember { mutableStateOf(appSettings.absenceNotificationsEnabled) }
    var absenceReminderDaysText by remember { mutableStateOf(TextFieldValue(appSettings.absenceReminderDays.toString())) }
    var absenceReminderSelectedOption by remember {
        mutableStateOf(
            when (appSettings.absenceReminderDays) {
                1 -> "1 día"
                2 -> "2 días"
                4 -> "4 días"
                6 -> "6 días"
                else -> "Personalizado"
            }
        )
    }
    var absenceReminderExpanded by remember { mutableStateOf(false) }
    var absenceReminderTimeType by remember { mutableStateOf(appSettings.absenceReminderTimeType) }
    var dailySummaryEnabled by remember { mutableStateOf(appSettings.dailySummaryEnabled) }
    var dailySummaryTime by remember { mutableStateOf(appSettings.dailySummaryTime) }
    var soundEnabled by remember { mutableStateOf(appSettings.soundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(appSettings.vibrationEnabled) }

    var showOnDialog by remember { mutableStateOf(false) }
    var onDialogMessage by remember { mutableStateOf("") }
    var showOffDialog by remember { mutableStateOf(false) }
    var offDialogMessage by remember { mutableStateOf("") }

    var hasNotificationPermission by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        ) 
    }

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
            hasNotificationPermission = isGranted
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showOnDialog) {
        AlertDialog(
            onDismissRequest = { showOnDialog = false },
            title = { Text("Notificaciones Activadas") },
            text = { Text(onDialogMessage) },
            confirmButton = {
                TextButton(onClick = { showOnDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (showOffDialog) {
        AlertDialog(
            onDismissRequest = { showOffDialog = false },
            title = { Text("Notificaciones Desactivadas") },
            text = { Text(offDialogMessage) },
            confirmButton = {
                TextButton(onClick = { showOffDialog = false }) {
                    Text("Entendido")
                }
            }
        )
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
            // Estado Global
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estado General de Notificaciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Permiso Android: ${if (hasNotificationPermission) "✅ Concedido" else "❌ No concedido"}")
                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Text("Solicitar Permiso")
                        }
                    }
                }
            }
            HorizontalDivider()

            // Turnos
            Text("Recordatorios de Turnos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            ListItem(
                headlineContent = { Text("Activar recordatorios") },
                trailingContent = {
                    Switch(
                        checked = turnReminderEnabled,
                        onCheckedChange = { checked ->
                            turnReminderEnabled = checked
                            appSettings.turnReminderEnabled = checked
                            
                            if (checked) {
                                coroutineScope.launch {
                                    val db = com.example.data.AppDatabase.getDatabase(context)
                                    val appointments = db.appointmentDao().getAllAppointments().first()
                                    val now = System.currentTimeMillis()
                                    
                                    var reprogrammedCount = 0
                                    var skippedCount = 0
                                    
                                    appointments.filter { it.dateTimestamp > now && it.status == "Pendiente" }
                                        .forEach { appt ->
                                            val reminderTime = appt.dateTimestamp - (appSettings.turnReminderMinutes * 60 * 1000L)
                                            if (reminderTime > now) {
                                                reprogrammedCount++
                                            } else {
                                                skippedCount++
                                            }
                                        }
                                    
                                    onDialogMessage = "Los Recordatorios de Turnos han sido reactivados.\n\n" +
                                        "• ${reprogrammedCount} turno(s) reprogramado(s).\n" +
                                        "• ${skippedCount} turno(s) omitido(s) ya que su horario de aviso (anticipación incluída) ya transcurrió."
                                    showOnDialog = true
                                }
                            } else {
                                offDialogMessage = "Los Recordatorios de Turnos han sido desactivados.\n\nSe han cancelado todos los avisos futuros para tus turnos agendados."
                                showOffDialog = true
                            }
                            com.example.NotificationHelper.scheduleAll(context)
                        }
                    )
                }
            )
            if (turnReminderEnabled) {
                val turnReminderOptions = listOf("5 minutos", "10 minutos", "15 minutos", "30 minutos", "Personalizado")
                ListItem(
                    headlineContent = { Text("Tiempo de anticipación") },
                    supportingContent = { Text("Minutos antes del turno") },
                    trailingContent = {
                        ExposedDropdownMenuBox(
                            expanded = turnReminderExpanded,
                            onExpandedChange = { turnReminderExpanded = !turnReminderExpanded },
                            modifier = Modifier.width(150.dp)
                        ) {
                            OutlinedTextField(
                                value = turnReminderSelectedOption,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = turnReminderExpanded) },
                                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                            )
                            ExposedDropdownMenu(
                                expanded = turnReminderExpanded,
                                onDismissRequest = { turnReminderExpanded = false }
                            ) {
                                turnReminderOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            turnReminderSelectedOption = option
                                            turnReminderExpanded = false
                                            if (option != "Personalizado") {
                                                val minutes = option.split(" ")[0].toIntOrNull()
                                                if (minutes != null) {
                                                    appSettings.turnReminderMinutes = minutes
                                                    turnReminderMinutesText = TextFieldValue(minutes.toString())
                                                    com.example.NotificationHelper.scheduleAll(context)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                if (turnReminderSelectedOption == "Personalizado") {
                    ListItem(
                        headlineContent = { Text("Minutos personalizados") },
                        trailingContent = {
                            OutlinedTextField(
                                value = turnReminderMinutesText,
                                onValueChange = { 
                                    turnReminderMinutesText = it
                                    val parsed = it.text.toIntOrNull()
                                    if (parsed != null && parsed > 0) {
                                        appSettings.turnReminderMinutes = parsed
                                        com.example.NotificationHelper.scheduleAll(context)
                                    }
                                },
                                modifier = Modifier.width(100.dp).onFocusChanged { state ->
                                    if (state.isFocused) {
                                        turnReminderMinutesText = turnReminderMinutesText.copy(selection = TextRange(0, turnReminderMinutesText.text.length))
                                    }
                                },
                                isError = turnReminderMinutesText.text.toIntOrNull()?.let { it <= 0 } ?: true,
                                singleLine = true
                            )
                        }
                    )
                }
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
                        onCheckedChange = { checked ->
                            dailyStartReminderEnabled = checked
                            appSettings.dailyStartReminderEnabled = checked
                            
                            if (checked) {
                                val now = System.currentTimeMillis()
                                val parts = appSettings.dailyStartReminderTime.split(":")
                                val cal = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 8)
                                    set(java.util.Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                                    set(java.util.Calendar.SECOND, 0)
                                }
                                
                                if (cal.timeInMillis < now) {
                                    onDialogMessage = "El aviso de Inicio de Jornada ha sido reactivado.\n\nYa que el horario de aviso estipulado (${appSettings.dailyStartReminderTime}) ya transcurrió en el día de hoy, el primer aviso lo recibirás en el día de mañana."
                                } else {
                                    onDialogMessage = "El aviso de Inicio de Jornada ha sido reactivado.\n\nRecibirás tu próximo aviso hoy a las ${appSettings.dailyStartReminderTime} hs."
                                }
                                showOnDialog = true
                            } else {
                                offDialogMessage = "El aviso de Inicio de Jornada ha sido desactivado."
                                showOffDialog = true
                            }
                            com.example.NotificationHelper.scheduleAll(context)
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ausencias y Bloqueos", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = absenceNotificationsEnabled,
                    onCheckedChange = { checked ->
                        absenceNotificationsEnabled = checked
                        appSettings.absenceNotificationsEnabled = checked
                        
                        if (checked) {
                            val now = System.currentTimeMillis()
                            var reprogrammedCount = 0
                            var skippedCount = 0
                            
                            appSettings.absencesList.filter { it.end + 86400000L > now }.forEach { absence ->
                                var targetTime = absence.start - (appSettings.absenceReminderDays * 86400000L)
                                val cal = java.util.Calendar.getInstance().apply { timeInMillis = targetTime }
                                
                                if (appSettings.absenceReminderTimeType == "InicioJornada") {
                                    val parts = appSettings.dailyStartReminderTime.split(":")
                                    if (parts.size >= 2) {
                                        cal.set(java.util.Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 8)
                                        cal.set(java.util.Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                                        cal.set(java.util.Calendar.SECOND, 0)
                                        cal.set(java.util.Calendar.MILLISECOND, 0)
                                        targetTime = cal.timeInMillis
                                    }
                                } else {
                                    val parts = appSettings.absenceReminderTimeType.split(":")
                                    if (parts.size >= 2) {
                                        cal.set(java.util.Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 0)
                                        cal.set(java.util.Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                                        cal.set(java.util.Calendar.SECOND, 0)
                                        cal.set(java.util.Calendar.MILLISECOND, 0)
                                        targetTime = cal.timeInMillis
                                    }
                                }
                                
                                var eventReprogrammed = false
                                var eventSkipped = false
                                
                                if (targetTime > now) {
                                    eventReprogrammed = true
                                } else if (!appSettings.isAbsenceNotified(absence.id)) {
                                    eventSkipped = true
                                }
                                
                                if (absence.isPartial) {
                                    val parts = absence.startTime.split(":")
                                    if (parts.size == 2) {
                                        val calP = java.util.Calendar.getInstance().apply {
                                            timeInMillis = absence.start
                                            set(java.util.Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 0)
                                            set(java.util.Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }
                                        if (calP.timeInMillis - (60 * 60 * 1000L) > now) {
                                            eventReprogrammed = true
                                        } else if (now < absence.end && !eventReprogrammed) {
                                            // Evaluado como omitido si aún no terminó pero la alarma ya pasó
                                            eventSkipped = true
                                        }
                                    }
                                }
                                
                                if (eventReprogrammed) reprogrammedCount++
                                else if (eventSkipped) skippedCount++
                            }
                            
                            onDialogMessage = "Las notificaciones han sido reactivadas.\n\n" +
                                "• ${reprogrammedCount} evento(s) reprogramado(s).\n" +
                                "• ${skippedCount} evento(s) omitido(s) ya que su horario de aviso ya transcurrió (no recibirás alertas tardías)."
                            showOnDialog = true
                        } else {
                            offDialogMessage = "Las notificaciones de Ausencias y Bloqueos han sido desactivadas.\n\nSe han cancelado todos los avisos futuros. Sin embargo, tus Ausencias y Bloqueos seguirán funcionando normalmente en tu Agenda."
                            showOffDialog = true
                        }
                        com.example.NotificationHelper.scheduleAll(context)
                    }
                )
            }
            if (absenceNotificationsEnabled) {
                val absenceReminderOptions = listOf("1 día", "2 días", "4 días", "6 días", "Personalizado")
                ListItem(
                    headlineContent = { Text("Anticipación para ausencias") },
                    supportingContent = { Text("Días antes de la ausencia") },
                    trailingContent = {
                        ExposedDropdownMenuBox(
                            expanded = absenceReminderExpanded,
                            onExpandedChange = { absenceReminderExpanded = !absenceReminderExpanded },
                            modifier = Modifier.width(150.dp)
                        ) {
                            OutlinedTextField(
                                value = absenceReminderSelectedOption,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = absenceReminderExpanded) },
                                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true)
                            )
                            ExposedDropdownMenu(
                                expanded = absenceReminderExpanded,
                                onDismissRequest = { absenceReminderExpanded = false }
                            ) {
                                absenceReminderOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            absenceReminderSelectedOption = option
                                            absenceReminderExpanded = false
                                            if (option != "Personalizado") {
                                                val days = option.split(" ")[0].toIntOrNull()
                                                if (days != null) {
                                                    appSettings.absenceReminderDays = days
                                                    absenceReminderDaysText = TextFieldValue(days.toString())
                                                    com.example.NotificationHelper.scheduleAll(context)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                if (absenceReminderSelectedOption == "Personalizado") {
                    ListItem(
                        headlineContent = { Text("Días personalizados") },
                        trailingContent = {
                            OutlinedTextField(
                                value = absenceReminderDaysText,
                                onValueChange = { 
                                    absenceReminderDaysText = it
                                    val parsed = it.text.toIntOrNull()
                                    if (parsed != null && parsed > 0) {
                                        appSettings.absenceReminderDays = parsed
                                        com.example.NotificationHelper.scheduleAll(context)
                                    }
                                },
                                modifier = Modifier.width(100.dp).onFocusChanged { state ->
                                    if (state.isFocused) {
                                        absenceReminderDaysText = absenceReminderDaysText.copy(selection = TextRange(0, absenceReminderDaysText.text.length))
                                    }
                                },
                                isError = absenceReminderDaysText.text.toIntOrNull()?.let { it <= 0 } ?: true,
                                singleLine = true
                            )
                        }
                    )
                }
                
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
                        "Si el horario calculado ya pasó, el aviso no será notificado para evitar falsas alarmas inmediatas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            } else {
                Text(
                    "Las notificaciones de Ausencias y Bloqueos están desactivadas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
                        onCheckedChange = { checked ->
                            dailySummaryEnabled = checked
                            appSettings.dailySummaryEnabled = checked
                            
                            if (checked) {
                                val now = System.currentTimeMillis()
                                val parts = appSettings.dailySummaryTime.split(":")
                                val cal = java.util.Calendar.getInstance().apply {
                                    set(java.util.Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 20)
                                    set(java.util.Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
                                    set(java.util.Calendar.SECOND, 0)
                                }
                                
                                if (cal.timeInMillis < now) {
                                    onDialogMessage = "El Resumen Diario ha sido reactivado.\n\nYa que el horario estipulado (${appSettings.dailySummaryTime}) ya transcurrió en el día de hoy, el primer aviso lo recibirás en el día de mañana."
                                } else {
                                    onDialogMessage = "El Resumen Diario ha sido reactivado.\n\nRecibirás tu próximo resumen hoy a las ${appSettings.dailySummaryTime} hs."
                                }
                                showOnDialog = true
                            } else {
                                offDialogMessage = "El Resumen Diario ha sido desactivado."
                                showOffDialog = true
                            }
                            com.example.NotificationHelper.scheduleAll(context)
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
            
            // Pruebas y Diagnóstico
            Text("Pruebas de Notificaciones", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                var testStatusMsg by remember { mutableStateOf("") }
                
                Button(
                    onClick = {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                        val canSchedule = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true
                        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        val notifEnabled = notifManager.areNotificationsEnabled()
                        
                        if (!notifEnabled) {
                            Toast.makeText(context, "ADVERTENCIA: Las notificaciones están desactivadas en los ajustes de Android.", Toast.LENGTH_LONG).show()
                        }
                        if (!canSchedule) {
                            Toast.makeText(context, "ADVERTENCIA: Faltan permisos de alarma exacta.", Toast.LENGTH_LONG).show()
                        }
                        
                        val scheduleTime = System.currentTimeMillis() + 60_000L
                        val intent = Intent(context, com.example.NotificationReceiver::class.java).apply {
                            action = "ABSENCE_REMINDER_TEST1"
                            putExtra("title", "Prueba: Vacaciones programadas")
                            putExtra("message", "A partir del ${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())}.")
                            putExtra("notificationId", 99991)
                            putExtra("absenceId", "test1")
                            putExtra("isTest", true)
                            putExtra("scheduledTime", scheduleTime)
                        }
                        
                        val pendingIntent = android.app.PendingIntent.getBroadcast(
                            context, 99991, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                        )
                        
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, scheduleTime, pendingIntent)
                            } else {
                                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, scheduleTime, pendingIntent)
                            }
                            appSettings.notificationLogsList = appSettings.notificationLogsList + com.example.data.NotificationLog(
                                scheduledTime = scheduleTime, type = "Prueba Ausencia Extensa", status = "Programada", actionId = "ABSENCE_REMINDER_TEST1"
                            )
                            testStatusMsg = "Prueba de Ausencia Extensa en 60s."
                            Toast.makeText(context, testStatusMsg, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("Probar Ausencia Extensa")
                }
                
                Button(
                    onClick = {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                        val canSchedule = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true
                        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        val notifEnabled = notifManager.areNotificationsEnabled()
                        
                        if (!notifEnabled) {
                            Toast.makeText(context, "ADVERTENCIA: Las notificaciones están desactivadas en los ajustes de Android.", Toast.LENGTH_LONG).show()
                        }
                        if (!canSchedule) {
                            Toast.makeText(context, "ADVERTENCIA: Faltan permisos de alarma exacta.", Toast.LENGTH_LONG).show()
                        }
                        
                        val scheduleTime1 = System.currentTimeMillis() + 60_000L
                        val scheduleTime2 = System.currentTimeMillis() + 120_000L
                        
                        val intent1 = Intent(context, com.example.NotificationReceiver::class.java).apply {
                            action = "ABSENCE_REMINDER_TEST2"
                            putExtra("title", "Prueba: Bloqueo parcial programado")
                            putExtra("message", "A partir del ${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())}.")
                            putExtra("notificationId", 99992)
                            putExtra("absenceId", "test2")
                            putExtra("isTest", true)
                            putExtra("scheduledTime", scheduleTime1)
                        }
                        val intent2 = Intent(context, com.example.NotificationReceiver::class.java).apply {
                            action = "PARTIAL_ABSENCE_TEST2"
                            putExtra("title", "Prueba: Bloqueo Próximo")
                            putExtra("message", "Tienes un bloqueo parcial en 1 hora.")
                            putExtra("notificationId", 99993)
                            putExtra("absenceId", "test2")
                            putExtra("isTest", true)
                            putExtra("scheduledTime", scheduleTime2)
                        }
                        
                        val p1 = android.app.PendingIntent.getBroadcast(context, 99992, intent1, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                        val p2 = android.app.PendingIntent.getBroadcast(context, 99993, intent2, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
                        
                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, scheduleTime1, p1)
                                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, scheduleTime2, p2)
                            } else {
                                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, scheduleTime1, p1)
                                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, scheduleTime2, p2)
                            }
                            appSettings.notificationLogsList = appSettings.notificationLogsList + com.example.data.NotificationLog(
                                scheduledTime = scheduleTime1, type = "Prueba Bloqueo Parcial 1", status = "Programada", actionId = "ABSENCE_REMINDER_TEST2"
                            )
                            appSettings.notificationLogsList = appSettings.notificationLogsList + com.example.data.NotificationLog(
                                scheduledTime = scheduleTime2, type = "Prueba Bloqueo Parcial 2", status = "Programada", actionId = "PARTIAL_ABSENCE_TEST2"
                            )
                            testStatusMsg = "Pruebas Ausencia Parcial en 60s/120s."
                            Toast.makeText(context, testStatusMsg, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("Probar Ausencia Parcial")
                }
                
                if (testStatusMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(testStatusMsg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                var showHistory by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { showHistory = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver Historial de Notificaciones y Diagnóstico")
                }
                
                if (showHistory) {
                    var logs by remember { mutableStateOf(appSettings.notificationLogsList) }
                    AlertDialog(
                        onDismissRequest = { showHistory = false },
                        title = { Text("Historial de Notificaciones") },
                        text = {
                            if (logs.isEmpty()) {
                                Text("No hay registros.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    items(logs.reversed()) { log ->
                                        val df = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
                                        val timeStr = if (log.executedTime > 0) df.format(Date(log.executedTime)) else df.format(Date(log.scheduledTime))
                                        Column(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                                            Text(log.type, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("${log.status} - $timeStr", style = MaterialTheme.typography.bodySmall)
                                            if (log.errorMessage.isNotBlank()) {
                                                Text(log.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Cerrar") } },
                        dismissButton = {
                            if (logs.isNotEmpty()) {
                                TextButton(onClick = { 
                                    appSettings.notificationLogsList = emptyList()
                                    logs = emptyList()
                                }) { 
                                    Text("Limpiar") 
                                }
                            }
                        }
                    )
                }
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
