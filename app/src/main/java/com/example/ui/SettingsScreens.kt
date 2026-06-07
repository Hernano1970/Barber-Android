package com.example.ui

import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, navController: NavController) {
    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text("Perfil del Negocio") },
                supportingContent = { Text("Nombre, dirección, teléfono") },
                leadingContent = { Icon(Icons.Filled.Store, contentDescription = null) },
                modifier = Modifier.clickable { navController.navigate("settings_business") }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Días y Horarios Laborales") },
                supportingContent = { Text("Apertura, cierre y días de trabajo") },
                leadingContent = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                modifier = Modifier.clickable { navController.navigate("settings_hours") }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Vacaciones y Ausencias") },
                supportingContent = { Text("Feriados y días libres") },
                leadingContent = { Icon(Icons.Filled.EventBusy, contentDescription = null) },
                modifier = Modifier.clickable { navController.navigate("settings_vacations") }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Integración de WhatsApp") },
                supportingContent = { Text("Recordatorios de turnos") },
                leadingContent = { Icon(Icons.Filled.Chat, contentDescription = null) }, // Add Chat icon fallback
                modifier = Modifier.clickable { navController.navigate("settings_whatsapp") }
            )
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(viewModel: MainViewModel, navController: NavController) {
    var name by remember { mutableStateOf(viewModel.appSettings.businessName) }
    var address by remember { mutableStateOf(viewModel.appSettings.businessAddress) }
    var phone by remember { mutableStateOf(viewModel.appSettings.businessPhone) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil del Negocio") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth()) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre de la Barbería") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono de Contacto") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                viewModel.updateBusinessName(name)
                viewModel.appSettings.businessAddress = address
                viewModel.appSettings.businessPhone = phone
                navController.popBackStack()
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar Cambios")
            }
        }
    }
}

data class DayScheduleState(
    val index: Int,
    val name: String,
    var isEnabled: MutableState<Boolean>,
    var startTime: MutableState<String>,
    var endTime: MutableState<String>
)

@Composable
fun TimeChip(time: String, isEnabled: Boolean, onTimeSelected: (String) -> Unit) {
    val context = LocalContext.current
    Surface(
        color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.clickable(enabled = isEnabled) {
            val parts = time.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            android.app.TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute ->
                    onTimeSelected(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute))
                },
                hour,
                minute,
                true
            ).show()
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(time, style = MaterialTheme.typography.bodyMedium, color = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkingHoursScreen(viewModel: MainViewModel, navController: NavController) {
    val defaultMap = "1|true|09:00|19:00,2|true|09:00|19:00,3|true|09:00|19:00,4|true|09:00|19:00,5|true|09:00|19:00,6|true|09:00|19:00,0|false|09:00|19:00"
    var currentMap = viewModel.appSettings.workingHoursMap
    if (!currentMap.contains("|")) {
        currentMap = defaultMap
    }
    
    val entries = currentMap.split(",").associate {
        val parts = it.split("|")
        parts[0].toInt() to parts
    }
    
    val days = listOf(
        Pair(1, "Lunes"),
        Pair(2, "Martes"),
        Pair(3, "Miércoles"),
        Pair(4, "Jueves"),
        Pair(5, "Viernes"),
        Pair(6, "Sábado"),
        Pair(0, "Domingo")
    )
    
    val schedules = remember {
        days.map { (index, name) ->
            val parts = entries[index]
            val isEnabled = parts?.getOrNull(1)?.toBooleanStrictOrNull() ?: (index != 0) // Default all except Sunday
            val start = parts?.getOrNull(2) ?: "09:00"
            val end = parts?.getOrNull(3) ?: "19:00"
            
            DayScheduleState(
                index = index,
                name = name,
                isEnabled = mutableStateOf(isEnabled),
                startTime = mutableStateOf(start),
                endTime = mutableStateOf(end)
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Días y Horarios Laborales") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val newMap = schedules.joinToString(",") { 
                    "${it.index}|${it.isEnabled.value}|${it.startTime.value}|${it.endTime.value}"
                }
                viewModel.appSettings.workingHoursMap = newMap
                navController.popBackStack()
            }) {
                Icon(Icons.Filled.Save, contentDescription = "Guardar")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Configura los días de la semana que se trabaja, horarios de apertura y cierre.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            items(schedules.size) { i ->
                val schedule = schedules[i]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (schedule.isEnabled.value) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = schedule.isEnabled.value,
                            onCheckedChange = { schedule.isEnabled.value = it }
                        )
                        Text(
                            text = schedule.name,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            color = if (schedule.isEnabled.value) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TimeChip(time = schedule.startTime.value, isEnabled = schedule.isEnabled.value) { newTime ->
                                schedule.startTime.value = newTime
                            }
                            Text(" - ", modifier = Modifier.padding(horizontal = 4.dp), color = if (schedule.isEnabled.value) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            TimeChip(time = schedule.endTime.value, isEnabled = schedule.isEnabled.value) { newTime ->
                                schedule.endTime.value = newTime
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationsScreen(viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    var absences by remember { mutableStateOf(viewModel.appSettings.absencesList) }

    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    
    var typeExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("Vacaciones") }
    val types = listOf("Vacaciones", "Feriado", "Franco")

    val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val showDatePicker = { onDateSelected: (Long) -> Unit ->
        val cal = Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val c = Calendar.getInstance()
                c.set(year, month, dayOfMonth)
                onDateSelected(c.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vacaciones y Ausencias") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth()) {
            item {
                Text(
                    text = "Configura días festivos, vacaciones o francos en los que no se podrán agendar turnos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Desde
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Desde", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = startDate?.let { formatter.format(Date(it)) } ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = { Text("dd/mm/aaaa") },
                                    trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker { date -> startDate = date } },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                            // Hasta
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hasta", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = endDate?.let { formatter.format(Date(it)) } ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    placeholder = { Text("dd/mm/aaaa") },
                                    trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker { date -> endDate = date } },
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tipo
                        Text("Tipo", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = !typeExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedType,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false }
                            ) {
                                types.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t) },
                                        onClick = {
                                            selectedType = t
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Nota
                        Text("Nota / Detalles (Opcional)", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            placeholder = { Text("Ej: Viaje familiar") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (startDate != null && endDate != null && startDate!! <= endDate!!) {
                                    val newAbsence = com.example.data.Absence(
                                        start = startDate!!,
                                        end = endDate!!,
                                        type = selectedType,
                                        note = note
                                    )
                                    val newList = absences.toMutableList().apply { add(newAbsence) }
                                    viewModel.appSettings.absencesList = newList
                                    absences = newList
                                    
                                    // Reset fields
                                    startDate = null
                                    endDate = null
                                    note = ""
                                } else {
                                    android.widget.Toast.makeText(context, "Fechas inválidas", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = startDate != null && endDate != null
                        ) {
                            Text("Agregar Ausencia")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text("Ausencias Registradas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (absences.isEmpty()) {
                item {
                    Text("No hay ausencias registradas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(absences.size) { index ->
                    val absence = absences[index]
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(absence.type, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                val ds = formatter.format(Date(absence.start))
                                val de = formatter.format(Date(absence.end))
                                Text(if (ds == de) ds else "$ds - $de", style = MaterialTheme.typography.bodyMedium)
                                if (absence.note.isNotBlank()) {
                                    Text(absence.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = {
                                val newList = absences.toMutableList().apply { remove(absence) }
                                viewModel.appSettings.absencesList = newList
                                absences = newList
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppSettingsScreen(viewModel: MainViewModel, navController: NavController) {
    var template by remember { mutableStateOf(viewModel.appSettings.whatsappMessageTemplate) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Integración de WhatsApp") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text("Plantilla del mensaje. Puedes usar las variables: {nombre}, {fecha}, {hora}, {negocio}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = template, 
                onValueChange = { template = it }, 
                label = { Text("Plantilla de Mensaje") }, 
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                viewModel.appSettings.whatsappMessageTemplate = template
                navController.popBackStack()
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar Cambios")
            }
        }
    }
}

fun sendWhatsAppMessage(context: android.content.Context, phone: String, message: String) {
    val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
    val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${URLEncoder.encode(message, "UTF-8")}"
    
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
    }
}
