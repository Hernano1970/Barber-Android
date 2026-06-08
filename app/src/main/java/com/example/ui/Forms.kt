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
                        viewModel.addClient(name, phone, obs)
                        navController.popBackStack()
                    } else {
                        showError = true
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
            var showError by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; showError = false },
                label = { Text("Nombre del Servicio *") },
                isError = showError,
                modifier = Modifier.fillMaxWidth()
            )
            if (showError) {
                Text("El nombre es requerido", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = priceStr, onValueChange = { priceStr = it }, label = { Text("Precio ($)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = durationStr, onValueChange = { durationStr = it }, label = { Text("Duración (minutos)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val duration = durationStr.toIntOrNull() ?: 0
                    if (name.isNotBlank()) {
                        viewModel.addService(name, price, duration, desc)
                        navController.popBackStack()
                    } else {
                        showError = true
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
                    clients.forEach { client ->
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
            var showSnackbar by remember { mutableStateOf(false) }
            
            Button(onClick = {
                if (isPastDate) {
                    showSnackbar = true
                    return@Button
                }
                if (isCasualClient && casualClientName.isNotBlank() && selectedServiceId != null) {
                    viewModel.addAppointmentWithNewClient(
                        clientName = casualClientName,
                        clientPhone = casualClientPhone,
                        clientObs = casualClientObs,
                        serviceId = selectedServiceId!!,
                        date = selectedDate,
                        apptObs = observations
                    )
                    navController.popBackStack()
                } else if (selectedClientId != null && selectedServiceId != null) {
                    viewModel.addAppointment(selectedClientId!!, selectedServiceId!!, selectedDate, observations)
                    navController.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Confirmar Turno")
            }
            if (showSnackbar) {
                Snackbar(
                    modifier = Modifier.padding(top = 8.dp),
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text("Error: El horario seleccionado ya ha pasado.")
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
                        clients.forEach { client ->
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

                OutlinedTextField(value = observations, onValueChange = { observations = it }, label = { Text("Observaciones/Agenda") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                
                Spacer(modifier = Modifier.height(24.dp))
                var showSnackbar by remember { mutableStateOf(false) }

                Button(onClick = {
                    if (isPastDate) {
                        showSnackbar = true
                        return@Button
                    }
                    if (selectedClientId != null && selectedServiceId != null) {
                        viewModel.updateAppointment(
                            appt.copy(
                                clientId = selectedClientId!!,
                                serviceId = selectedServiceId!!,
                                dateTimestamp = selectedDate,
                                observations = observations
                            )
                        )
                        navController.popBackStack()
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Actualizar Turno")
                }
                if (showSnackbar) {
                    Snackbar(
                        modifier = Modifier.padding(top = 8.dp),
                        action = {
                            TextButton(onClick = { showSnackbar = false }) {
                                Text("OK")
                            }
                        }
                    ) {
                        Text("Error: El horario seleccionado ya ha pasado.")
                    }
                }
            }
        }
    }
}
