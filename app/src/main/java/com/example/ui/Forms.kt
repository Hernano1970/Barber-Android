package com.example.ui

import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClientScreen(viewModel: MainViewModel, navController: NavController) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Cliente") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            var showError by remember { mutableStateOf(false) }
            var showPhoneWarning by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; showError = false },
                label = { Text("Nombre Completo *") },
                isError = showError,
                modifier = Modifier.fillMaxWidth()
            )
            if (showError) {
                Text("El nombre es requerido", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = obs, onValueChange = { obs = it }, label = { Text("Observaciones/Cliente") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(onClick = {
                    if (name.isNotBlank()) {
                        if (phone.isBlank() && !showPhoneWarning) {
                            showPhoneWarning = true
                        } else {
                            viewModel.addClient(name, phone, obs)
                            navController.popBackStack()
                        }
                    } else {
                        showError = true
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text("Guardar")
                }
            }
            
            if (showPhoneWarning) {
                AlertDialog(
                    onDismissRequest = { showPhoneWarning = false },
                    title = { Text("ATENCIÓN") },
                    text = { Text("Este cliente no tiene un número de teléfono registrado. Si continúa, no será posible enviar notificaciones ni recordatorios de turnos mediante WhatsApp. ¿Desea guardar el cliente de todas formas?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.addClient(name, phone, obs)
                            showPhoneWarning = false
                            navController.popBackStack()
                        }) { Text("Guardar de Todas Formas", color = MaterialTheme.colorScheme.primary) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPhoneWarning = false }) { Text("Volver y Completar Teléfono") }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditClientScreen(viewModel: MainViewModel, navController: NavController, clientId: Int) {
    val clients by viewModel.clients.collectAsState()
    val clientToEdit = clients.find { it.id == clientId }

    var name by remember(clientToEdit) { mutableStateOf(clientToEdit?.fullName ?: "") }
    var phone by remember(clientToEdit) { mutableStateOf(clientToEdit?.phone ?: "") }
    var obs by remember(clientToEdit) { mutableStateOf(clientToEdit?.observations ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Cliente") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (clientToEdit == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Cliente no encontrado")
            }
        } else {
            Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
                var showError by remember(clientToEdit) { mutableStateOf(false) }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showError = false },
                    label = { Text("Nombre Completo *") },
                    isError = showError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text("El nombre es requerido", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = obs, onValueChange = { obs = it }, label = { Text("Observaciones/Cliente") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(onClick = {
                        if (name.isNotBlank()) {
                            viewModel.updateClient(clientToEdit.copy(fullName = name, phone = phone, observations = obs))
                            navController.popBackStack()
                        } else {
                            showError = true
                        }
                    }, modifier = Modifier.weight(1f)) {
                        Text("Actualizar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceScreen(viewModel: MainViewModel, navController: NavController) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var durationStr by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Servicio") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val validDurations = listOf(15, 30, 45, 60)
            var expandedDuration by remember { mutableStateOf(false) }

            if (errorMessage != null) {
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    title = { Text("Atención") },
                    text = { Text(errorMessage!!) },
                    confirmButton = {
                        TextButton(onClick = { errorMessage = null }) { Text("OK") }
                    }
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del Servicio *") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = it },
                label = { Text("Precio ($) *") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expandedDuration,
                onExpandedChange = { expandedDuration = !expandedDuration }
            ) {
                OutlinedTextField(
                    value = durationStr.takeIf { it.isNotBlank() }?.let { "$it minutos" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Duración (minutos) *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDuration) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = expandedDuration,
                    onDismissRequest = { expandedDuration = false }
                ) {
                    validDurations.forEach { d ->
                        DropdownMenuItem(
                            text = { Text("$d minutos") },
                            onClick = {
                                durationStr = d.toString()
                                expandedDuration = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(onClick = {
                    val price = priceStr.toDoubleOrNull()
                    val duration = durationStr.toIntOrNull()
                    if (name.isBlank()) {
                        errorMessage = "Debe ingresar un Nombre de Servicio."
                    } else if (price == null) {
                        errorMessage = "Debe ingresar un Precio."
                    } else if (price <= 0) {
                        errorMessage = "El Precio debe ser mayor a cero."
                    } else if (duration == null) {
                        errorMessage = "Debe ingresar una Duración."
                    } else if (!validDurations.contains(duration)) {
                        errorMessage = "Debe seleccionar una duración válida para el servicio."
                    } else {
                        viewModel.addService(name, price, duration, desc)
                        navController.popBackStack()
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text("Guardar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServiceScreen(viewModel: MainViewModel, navController: NavController, serviceId: Int) {
    val services by viewModel.activeServices.collectAsState()
    val serviceToEdit = services.find { it.id == serviceId }
    
    if (serviceToEdit == null) {
        // Handle error or loading
        return
    }

    var name by remember { mutableStateOf(serviceToEdit.name) }
    var priceStr by remember { mutableStateOf(serviceToEdit.price.toString()) }
    var durationStr by remember { mutableStateOf(serviceToEdit.durationMinutes.toString()) }
    var desc by remember { mutableStateOf(serviceToEdit.description) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Servicio") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            var errorMessage by remember { mutableStateOf<String?>(null) }
            val validDurations = listOf(15, 30, 45, 60)
            var expandedDuration by remember { mutableStateOf(false) }

            if (errorMessage != null) {
                AlertDialog(
                    onDismissRequest = { errorMessage = null },
                    title = { Text("Atención") },
                    text = { Text(errorMessage!!) },
                    confirmButton = {
                        TextButton(onClick = { errorMessage = null }) { Text("OK") }
                    }
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del Servicio *") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = priceStr,
                onValueChange = { priceStr = it },
                label = { Text("Precio ($) *") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expandedDuration,
                onExpandedChange = { expandedDuration = !expandedDuration }
            ) {
                OutlinedTextField(
                    value = durationStr.takeIf { it.isNotBlank() }?.let { "$it minutos" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Duración (minutos) *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDuration) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable, true)
                )
                ExposedDropdownMenu(
                    expanded = expandedDuration,
                    onDismissRequest = { expandedDuration = false }
                ) {
                    validDurations.forEach { d ->
                        DropdownMenuItem(
                            text = { Text("$d minutos") },
                            onClick = {
                                durationStr = d.toString()
                                expandedDuration = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(onClick = {
                    val price = priceStr.toDoubleOrNull()
                    val duration = durationStr.toIntOrNull()
                    if (name.isBlank()) {
                        errorMessage = "Debe ingresar un Nombre de Servicio."
                    } else if (price == null) {
                        errorMessage = "Debe ingresar un Precio."
                    } else if (price <= 0) {
                        errorMessage = "El Precio debe ser mayor a cero."
                    } else if (duration == null) {
                        errorMessage = "Debe ingresar una Duración."
                    } else if (!validDurations.contains(duration)) {
                        errorMessage = "Debe seleccionar una duración válida para el servicio."
                    } else {
                        viewModel.updateService(serviceToEdit.copy(name = name, price = price, durationMinutes = duration, description = desc))
                        navController.popBackStack()
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text("Guardar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentScreen(viewModel: MainViewModel, navController: NavController, initialTimestamp: Long = -1L) {
    val allAppointments by viewModel.allAppointments.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val services by viewModel.activeServices.collectAsState()
    val context = LocalContext.current

    var selectedClientId by remember { mutableStateOf<Int?>(null) }
    var selectedServiceId by remember { mutableStateOf<Int?>(null) }
    var observations by remember { mutableStateOf("") }
    
    var isCasualClient by remember { mutableStateOf(false) }
    var casualClientName by remember { mutableStateOf("") }
    var casualClientPhone by remember { mutableStateOf("") }
    var casualClientObs by remember { mutableStateOf("") }
    var saveAsPermanent by remember { mutableStateOf(true) }
    
    var clientExpanded by remember { mutableStateOf(false) }
    var serviceExpanded by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    if (initialTimestamp > 0) {
        calendar.timeInMillis = initialTimestamp
    }
    var selectedDate by remember { mutableStateOf(calendar.timeInMillis) }
    
    // Derived UI states
    val selectedClientName = if (isCasualClient) "-- Cliente Casual / Manual --" else clients.find { it.id == selectedClientId }?.fullName ?: "Seleccionar Cliente"
    val selectedService = services.find { it.id == selectedServiceId }
    val selectedServiceName = selectedService?.let { "${it.name} (${it.durationMinutes} min)" } ?: "Seleccionar Servicio"

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(year, month, dayOfMonth)
            val timePickerDialog = TimePickerDialog(
                context,
                { _: TimePicker, hourOfDay: Int, minute: Int ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    selectedDate = calendar.timeInMillis
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agendar Turno") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
            
            // Client Dropdown
            ExposedDropdownMenuBox(
                expanded = clientExpanded,
                onExpandedChange = { clientExpanded = !clientExpanded }
            ) {
                OutlinedTextField(
                    value = selectedClientName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cliente") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = clientExpanded,
                    onDismissRequest = { clientExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("-- Cliente Casual / Manual --", fontWeight = FontWeight.Bold) },
                        onClick = {
                            isCasualClient = true
                            selectedClientId = null
                            clientExpanded = false
                        }
                    )
                    val permClients = clients.filter { it.isPermanent }
                    permClients.forEach { client ->
                        DropdownMenuItem(
                            text = { Text(client.fullName) },
                            onClick = {
                                isCasualClient = false
                                selectedClientId = client.id
                                clientExpanded = false
                            }
                        )
                    }
                }
            }

            if (isCasualClient) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = casualClientName,
                    onValueChange = { casualClientName = it },
                    label = { Text("Nombre (Nuevo Cliente) *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = casualClientPhone,
                    onValueChange = { casualClientPhone = it },
                    label = { Text("Teléfono (Opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = casualClientObs,
                    onValueChange = { casualClientObs = it },
                    label = { Text("Observaciones/Cliente") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { saveAsPermanent = !saveAsPermanent }.padding(vertical = 8.dp)) {
                    Checkbox(checked = saveAsPermanent, onCheckedChange = { saveAsPermanent = it })
                    Text("Guardar como cliente permanente en la agenda")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Service Dropdown
            ExposedDropdownMenuBox(
                expanded = serviceExpanded,
                onExpandedChange = { serviceExpanded = !serviceExpanded }
            ) {
                OutlinedTextField(
                    value = selectedServiceName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Servicio") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = serviceExpanded,
                    onDismissRequest = { serviceExpanded = false }
                ) {
                    services.forEach { service ->
                        DropdownMenuItem(
                            text = { Text("${service.name} (${service.durationMinutes} min)") },
                            onClick = {
                                selectedServiceId = service.id
                                serviceExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES"))
            val isPastDate = selectedDate < (System.currentTimeMillis() - 60_000) // 1 minute leeway

            OutlinedTextField(
                value = sdf.format(Date(selectedDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha y Hora") },
                isError = isPastDate,
                modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = if (isPastDate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = if (isPastDate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    disabledLabelColor = if (isPastDate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            if (isPastDate) {
                Text(
                    text = "No se pueden agendar turnos en fechas u horarios pasados.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = observations, onValueChange = { observations = it }, label = { Text("Observaciones/Agenda") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            
            Spacer(modifier = Modifier.height(24.dp))
            var snackbarMessage by remember { mutableStateOf<String?>(null) }
            var duplicateApptWarning by remember { mutableStateOf<com.example.data.Appointment?>(null) }
            var duplicateClientWarning by remember { mutableStateOf(false) }
            var showPhoneWarning by remember { mutableStateOf(false) }

            val saveAppointment = {
                if (isCasualClient && casualClientName.isNotBlank() && selectedServiceId != null) {
                    viewModel.addAppointmentWithNewClient(
                        clientName = casualClientName,
                        clientPhone = casualClientPhone,
                        clientObs = casualClientObs,
                        serviceId = selectedServiceId!!,
                        date = selectedDate,
                        apptObs = observations,
                        isPermanent = saveAsPermanent
                    )
                    navController.popBackStack()
                } else if (selectedClientId != null && selectedServiceId != null) {
                    viewModel.addAppointment(selectedClientId!!, selectedServiceId!!, selectedDate, observations)
                    navController.popBackStack()
                }
            }
            
            Button(onClick = {
                if (isPastDate) {
                    snackbarMessage = "Error: El horario seleccionado ya ha pasado."
                    return@Button
                }
                if (selectedServiceId == null) {
                    snackbarMessage = "Error: Debes seleccionar un servicio."
                    return@Button
                }
                if (!isCasualClient && selectedClientId == null) {
                    snackbarMessage = "Error: Debes seleccionar un cliente."
                    return@Button
                }

                val durationMins = selectedService?.durationMinutes ?: 30
                val requestedStart = selectedDate
                val requestedEnd = requestedStart + (durationMins * 60 * 1000)

                val overlappingAppt = allAppointments.find { a ->
                    val s = services.find { it.id == a.serviceId }
                    val dur = s?.durationMinutes ?: 30
                    val aStart = a.dateTimestamp
                    val aEnd = aStart + (dur * 60 * 1000)
                    requestedStart < aEnd && requestedEnd > aStart
                }

                if (overlappingAppt != null) {
                    val nextAppt = allAppointments.filter { it.dateTimestamp >= requestedStart }
                                                  .minByOrNull { it.dateTimestamp }
                    if (nextAppt != null && nextAppt.dateTimestamp > requestedStart) {
                        val availableMins = (nextAppt.dateTimestamp - requestedStart) / (60 * 1000)
                        snackbarMessage = "Solamente tienes $availableMins min disponibles antes del próximo turno. (El servicio dura $durationMins min)"
                    } else {
                        snackbarMessage = "El servicio se superpone con otro turno programado."
                    }
                    return@Button
                }

                val todayMidnight = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis

                val duplicate = if (!isCasualClient && selectedClientId != null) {
                    allAppointments.find { it.clientId == selectedClientId && (it.dateTimestamp >= todayMidnight || !it.isPaid) }
                } else null

                if (duplicate != null) {
                    duplicateApptWarning = duplicate
                } else if (isCasualClient && saveAsPermanent && casualClientPhone.isNotBlank() && clients.any { it.phone == casualClientPhone && it.isPermanent }) {
                    duplicateClientWarning = true
                } else if (isCasualClient && casualClientPhone.isBlank() && !showPhoneWarning) {
                    showPhoneWarning = true
                } else {
                    saveAppointment()
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Confirmar Turno")
            }

            if (showPhoneWarning) {
                AlertDialog(
                    onDismissRequest = { showPhoneWarning = false },
                    title = { Text("ATENCIÓN") },
                    text = { Text("Este cliente no tiene un número de teléfono registrado. Si continúa, no será posible enviar notificaciones ni recordatorios de turnos mediante WhatsApp. ¿Desea guardar el cliente de todas formas?") },
                    confirmButton = {
                        TextButton(onClick = {
                            saveAppointment()
                            showPhoneWarning = false
                        }) { Text("Guardar de Todas Formas", color = MaterialTheme.colorScheme.primary) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPhoneWarning = false }) { Text("Volver y Completar Teléfono") }
                    }
                )
            }

            if (duplicateClientWarning) {
                AlertDialog(
                    onDismissRequest = { duplicateClientWarning = false },
                    title = { Text("Atención") },
                    text = { Text("Este cliente ya se encuentra registrado (mismo número de teléfono).") },
                    confirmButton = {
                        TextButton(onClick = { duplicateClientWarning = false }) { Text("OK") }
                    }
                )
            }

            if (duplicateApptWarning != null) {
                AlertDialog(
                    onDismissRequest = { duplicateApptWarning = null },
                    title = { Text("Atención") },
                    text = {
                        val apptDate = java.text.SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(Date(duplicateApptWarning!!.dateTimestamp))
                        val apptTime = java.text.SimpleDateFormat("HH:mm", Locale("es", "ES")).format(Date(duplicateApptWarning!!.dateTimestamp))
                        Text("Este cliente ya tiene un turno registrado el día $apptDate a las $apptTime. ¿Desea continuar?")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            duplicateApptWarning = null
                            saveAppointment()
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { duplicateApptWarning = null }) { Text("Cancelar") }
                    }
                )
            }

            if (snackbarMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(top = 8.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(snackbarMessage!!)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppointmentScreen(viewModel: MainViewModel, navController: NavController, appointmentId: Int) {
    val appointments by viewModel.allAppointments.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val services by viewModel.activeServices.collectAsState()
    val context = LocalContext.current

    val appt = appointments.find { it.id == appointmentId }

    var selectedClientId by remember(appt) { mutableStateOf(appt?.clientId) }
    var selectedServiceId by remember(appt) { mutableStateOf(appt?.serviceId) }
    var observations by remember(appt) { mutableStateOf(appt?.observations ?: "") }
    
    var clientExpanded by remember { mutableStateOf(false) }
    var serviceExpanded by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    if (appt != null) {
        calendar.timeInMillis = appt.dateTimestamp
    }
    var selectedDate by remember(appt) { mutableStateOf(appt?.dateTimestamp ?: calendar.timeInMillis) }
    
    // Derived UI states
    val selectedClientName = clients.find { it.id == selectedClientId }?.fullName ?: "Seleccionar Cliente"
    val selectedService = services.find { it.id == selectedServiceId }
    val selectedServiceName = selectedService?.let { "${it.name} (${it.durationMinutes} min)" } ?: "Seleccionar Servicio"

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            calendar.set(year, month, dayOfMonth)
            val timePickerDialog = TimePickerDialog(
                context,
                { _: TimePicker, hourOfDay: Int, minute: Int ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    selectedDate = calendar.timeInMillis
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Turno") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (appt == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Turno no encontrado")
            }
        } else {
            Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxWidth().verticalScroll(rememberScrollState())) {
                
                // Client Dropdown
                ExposedDropdownMenuBox(
                    expanded = clientExpanded,
                    onExpandedChange = { clientExpanded = !clientExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClientName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cliente") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = clientExpanded) },
                        modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = clientExpanded,
                        onDismissRequest = { clientExpanded = false }
                    ) {
                    val permClients = clients.filter { it.isPermanent }
                    permClients.forEach { client ->
                        DropdownMenuItem(
                            text = { Text(client.fullName) },
                            onClick = {
                                selectedClientId = client.id
                                clientExpanded = false
                            }
                        )
                    }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Service Dropdown
                ExposedDropdownMenuBox(
                    expanded = serviceExpanded,
                    onExpandedChange = { serviceExpanded = !serviceExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedServiceName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Servicio") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) },
                        modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = serviceExpanded,
                        onDismissRequest = { serviceExpanded = false }
                    ) {
                        services.forEach { service ->
                            DropdownMenuItem(
                                text = { Text("${service.name} (${service.durationMinutes} min)") },
                                onClick = {
                                    selectedServiceId = service.id
                                    serviceExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES"))
                val isPastDate = selectedDate < (System.currentTimeMillis() - 60_000) // 1 minute leeway

                OutlinedTextField(
                    value = sdf.format(Date(selectedDate)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha y Hora") },
                    isError = isPastDate,
                    modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (isPastDate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = if (isPastDate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        disabledLabelColor = if (isPastDate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                if (isPastDate) {
                    Text(
                        text = "No se pueden agendar turnos en fechas u horarios pasados.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                var statusExpanded by remember { mutableStateOf(false) }
                var currentStatus by remember(appt) { mutableStateOf(appt?.status ?: "Pendiente") }
                
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded }
                ) {
                    OutlinedTextField(
                        value = currentStatus,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        listOf("Pendiente", "Completado", "Cancelado", "Pagado").forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s) },
                                onClick = {
                                    currentStatus = s
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = observations, onValueChange = { observations = it }, label = { Text("Observaciones/Agenda") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                
                Spacer(modifier = Modifier.height(24.dp))
                var snackbarMessage by remember { mutableStateOf<String?>(null) }
                var duplicateApptWarning by remember { mutableStateOf<com.example.data.Appointment?>(null) }

                val updateAppointment = {
                    if (selectedClientId != null && selectedServiceId != null) {
                        viewModel.updateAppointment(
                            appt.copy(
                                clientId = selectedClientId!!,
                                serviceId = selectedServiceId!!,
                                dateTimestamp = selectedDate,
                                observations = observations,
                                status = currentStatus,
                                isPaid = if (currentStatus == "Pagado") true else (if (currentStatus == "Pendiente") false else appt.isPaid)
                            )
                        )
                        navController.popBackStack()
                    }
                }

                Button(onClick = {
                    if (isPastDate) {
                        snackbarMessage = "Error: El horario seleccionado ya ha pasado."
                        return@Button
                    }
                    if (selectedServiceId == null || selectedClientId == null) {
                        snackbarMessage = "Error: Verifica cliente y servicio."
                        return@Button
                    }
                    
                    val durationMins = selectedService?.durationMinutes ?: 30
                    val requestedStart = selectedDate
                    val requestedEnd = requestedStart + (durationMins * 60 * 1000)

                    val overlappingAppt = appointments.find { a ->
                        if (a.id == appt.id) return@find false // ignora el mismo turno
                        val s = services.find { it.id == a.serviceId }
                        val dur = s?.durationMinutes ?: 30
                        val aStart = a.dateTimestamp
                        val aEnd = aStart + (dur * 60 * 1000)
                        requestedStart < aEnd && requestedEnd > aStart
                    }

                    if (overlappingAppt != null) {
                        val nextAppt = appointments.filter { it.id != appt.id && it.dateTimestamp >= requestedStart }
                                                      .minByOrNull { it.dateTimestamp }
                        if (nextAppt != null && nextAppt.dateTimestamp > requestedStart) {
                            val availableMins = (nextAppt.dateTimestamp - requestedStart) / (60 * 1000)
                            snackbarMessage = "Solamente tienes $availableMins min disponibles antes del próximo turno. (El servicio dura $durationMins min)"
                        } else {
                            snackbarMessage = "El servicio se superpone con otro turno programado."
                        }
                        return@Button
                    }

                    val todayMidnight = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                    }.timeInMillis

                    val duplicate = if (selectedClientId != null) {
                        appointments.find { it.id != appt.id && it.clientId == selectedClientId && (it.dateTimestamp >= todayMidnight || !it.isPaid) }
                    } else null

                    if (duplicate != null) {
                        duplicateApptWarning = duplicate
                    } else {
                        updateAppointment()
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Actualizar Turno")
                }

                if (duplicateApptWarning != null) {
                    AlertDialog(
                        onDismissRequest = { duplicateApptWarning = null },
                        title = { Text("Atención") },
                        text = {
                            val apptDate = java.text.SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(Date(duplicateApptWarning!!.dateTimestamp))
                            val apptTime = java.text.SimpleDateFormat("HH:mm", Locale("es", "ES")).format(Date(duplicateApptWarning!!.dateTimestamp))
                            Text("Este cliente ya tiene un turno registrado el día $apptDate a las $apptTime. ¿Desea continuar?")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                duplicateApptWarning = null
                                updateAppointment()
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { duplicateApptWarning = null }) { Text("Cancelar") }
                        }
                    )
                }

                if (snackbarMessage != null) {
                    Snackbar(
                        modifier = Modifier.padding(top = 8.dp),
                        action = {
                            TextButton(onClick = { snackbarMessage = null }) {
                                Text("OK")
                            }
                        }
                    ) {
                        Text(snackbarMessage!!)
                    }
                }
            }
        }
    }
}
