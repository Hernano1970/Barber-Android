package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: MainViewModel, navController: NavController) {
    val totalClients by viewModel.totalClientCount.collectAsState()
    val todayAppointments by viewModel.todayAppointments.collectAsState()
    val activeServices by viewModel.activeServices.collectAsState()
    val clients by viewModel.clients.collectAsState()

    var todayIncome = 0.0
    todayAppointments.filter { it.isPaid }.forEach { appt ->
        val service = activeServices.find { it.id == appt.serviceId }
        todayIncome += (service?.price ?: 0.0)
    }

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    val absencesList = viewModel.appSettings.absencesList.filter { it.end >= todayStart }
    val extensasCount = absencesList.count { !it.isPartial }
    val parcialesCount = absencesList.count { it.isPartial }
    val hasAttentionAbsence = absencesList.any { it.start <= todayStart + 7 * 24 * 60 * 60 * 1000L }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Panel de Control", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCard(
                title = "Turnos Hoy / Próximos",
                value = todayAppointments.size.toString(),
                icon = Icons.Filled.CalendarToday,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("upcoming_appointments") }
            )
            Spacer(modifier = Modifier.width(16.dp))
            StatCard(
                title = "Total Clientes",
                value = clients.count { it.isPermanent }.toString(),
                icon = Icons.Filled.People,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
             StatCard(
                title = "Servicios Activos",
                value = activeServices.size.toString(),
                icon = Icons.Filled.DesignServices,
                modifier = Modifier.weight(1f)
            )
             Spacer(modifier = Modifier.width(16.dp))
             StatCard(
                title = "Estadísticas",
                value = "$${"%.0f".format(todayIncome)}",
                icon = Icons.Filled.BarChart,
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("statistics") }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val titleText = "Ausencias Registradas"
            val valueText = "$extensasCount Extensas\n$parcialesCount Parciales"
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { navController.navigate("ausencias_read_only") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(titleText, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (hasAttentionAbsence) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(valueText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Accesos Rápidos", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            QuickActionButton(icon = Icons.Filled.People, label = "Clientes") {
                navController.navigate("clients")
            }
            QuickActionButton(icon = Icons.Filled.ContentCut, label = "Servicios") {
                navController.navigate("services")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingAppointmentsScreen(viewModel: MainViewModel, navController: NavController) {
    val allAppointments by viewModel.allAppointments.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val services by viewModel.activeServices.collectAsState()

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val upcomingAppointments = allAppointments.filter { it.dateTimestamp >= today }.sortedBy { it.dateTimestamp }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Próximos Turnos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (upcomingAppointments.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay próximos turnos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(upcomingAppointments) { appt ->
                    val client = clients.find { it.id == appt.clientId }
                    val service = services.find { it.id == appt.serviceId }
                    val dateFormat = SimpleDateFormat("EEEE, dd MMM HH:mm", Locale("es", "ES"))
                    val isPending = !appt.isPaid

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dateFormat.format(Date(appt.dateTimestamp)).replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(client?.fullName ?: "Desconocido", style = MaterialTheme.typography.bodyMedium)
                                Text(service?.name ?: "Servicio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                    .background(if (!isPending) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (!isPending) "Pagado" else "Pendiente", 
                                    fontSize = 11.sp, 
                                    color = if (!isPending) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        FloatingActionButton(onClick = onClick, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Card(modifier = modifier.then(clickModifier), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(viewModel: MainViewModel, navController: NavController) {
    val clients by viewModel.clients.collectAsState()
    var showOptionsForClient by remember { mutableStateOf<com.example.data.Client?>(null) }
    var clientToDelete by remember { mutableStateOf<com.example.data.Client?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clientes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = { navController.navigate("import_contacts") }) {
                        Text("Importar Contacto", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_client") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Client")
            }
        }
    ) { padding ->
        val permanentClients = clients.filter { it.isPermanent }
        LazyColumn(contentPadding = padding) {
            items(permanentClients) { client ->
                ListItem(
                    headlineContent = { Text(client.fullName, fontWeight = FontWeight.Bold) },
                    supportingContent = {
                        Column {
                            if (client.phone.isBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Warning, contentDescription = "Sin Teléfono", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sin teléfono registrado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            } else {
                                Text(client.phone)
                            }
                            if (client.observations.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Observaciones/Cliente: ${client.observations}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    leadingContent = {
                        Icon(Icons.Filled.Person, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showOptionsForClient = client }
                )
                HorizontalDivider()
            }
        }

        if (showOptionsForClient != null) {
            val client = showOptionsForClient!!
            AlertDialog(
                onDismissRequest = { showOptionsForClient = null },
                title = { Text("Gestionar Cliente") },
                text = { Text("¿Qué deseas hacer con ${client.fullName}?") },
                confirmButton = {
                    TextButton(onClick = {
                        showOptionsForClient = null
                        navController.navigate("edit_client/${client.id}")
                    }) {
                        Text("Editar", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            clientToDelete = client
                            showOptionsForClient = null
                        }) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { showOptionsForClient = null }) {
                            Text("Cancelar")
                        }
                    }
                }
            )
        }

        if (clientToDelete != null) {
            AlertDialog(
                onDismissRequest = { clientToDelete = null },
                title = { Text("Confirmar Eliminación") },
                text = { Text("¿Está seguro que desea eliminar este cliente?") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteClient(clientToDelete!!)
                        clientToDelete = null
                    }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { clientToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(viewModel: MainViewModel, navController: NavController) {
    val services by viewModel.activeServices.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Servicios") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_service") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Service")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(services) { service ->
                ListItem(
                    headlineContent = { Text(service.name, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Duración: ${service.durationMinutes} min - ${service.description}") },
                    trailingContent = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$${service.price}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { navController.navigate("edit_service/${service.id}") }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteService(service) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    leadingContent = {
                        Icon(Icons.Filled.ContentCut, contentDescription = null)
                    }
                )
                Divider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(viewModel: MainViewModel, navController: NavController) {
    val appointments by viewModel.allAppointments.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val services by viewModel.activeServices.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedDate by remember { mutableStateOf(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }) }

    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("es", "ES"))
    val timeFormat = SimpleDateFormat("HH:mm", Locale("es", "ES"))

    var showOptionsForAppt by remember { mutableStateOf<com.example.data.Appointment?>(null) }
    var showNoServicesDialog by remember { mutableStateOf(false) }

    val dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK)
    val workingHours = viewModel.appSettings.getWorkingHours(dayOfWeek)
    val absenceDay = viewModel.appSettings.getFullDayAbsenceForDate(selectedDate.timeInMillis)
    val isAbsence = absenceDay != null

    val partialAbsences = viewModel.appSettings.getAbsencesForDate(selectedDate.timeInMillis).filter { it.isPartial }
    val blockedBlocks = partialAbsences.mapNotNull { partial ->
        try {
            val startParts = partial.startTime.split(":")
            val endParts = partial.endTime.split(":")
            if (startParts.size == 2 && endParts.size == 2) {
                val startCal = selectedDate.clone() as Calendar
                startCal.set(Calendar.HOUR_OF_DAY, startParts[0].toInt())
                startCal.set(Calendar.MINUTE, startParts[1].toInt())
                startCal.set(Calendar.SECOND, 0)
                
                val endCal = selectedDate.clone() as Calendar
                endCal.set(Calendar.HOUR_OF_DAY, endParts[0].toInt())
                endCal.set(Calendar.MINUTE, endParts[1].toInt())
                endCal.set(Calendar.SECOND, 0)
                
                Triple(startCal.timeInMillis, endCal.timeInMillis, partial)
            } else null
        } catch (e: Exception) { null }
    }

    val startHour = workingHours?.first ?: 8
    val endHour = workingHours?.second ?: 20
    val slots = mutableListOf<Pair<Int, Int>>()
    for (h in startHour..endHour) {
        slots.add(h to 0)
        if (h != endHour) {
            slots.add(h to 15)
            slots.add(h to 30)
            slots.add(h to 45)
        }
    }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Date Selector
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    val newDate = selectedDate.clone() as Calendar
                    newDate.add(Calendar.DAY_OF_MONTH, -1)
                    selectedDate = newDate 
                }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Previous Day")
                }
                Text(
                    text = dateFormat.format(selectedDate.time).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { 
                    val newDate = selectedDate.clone() as Calendar
                    newDate.add(Calendar.DAY_OF_MONTH, 1)
                    selectedDate = newDate 
                }) {
                    Icon(Icons.Filled.ArrowForward, contentDescription = "Next Day")
                }
            }

            Divider()

            if (workingHours == null || isAbsence) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isAbsence) {
                                val type = absenceDay?.type ?: "Día Libre"
                                val note = absenceDay?.note ?: ""
                                if (note.isNotBlank()) "$type\n$note" else type
                            } else {
                                "Cerrado en este día"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Time Slots
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                
                LaunchedEffect(selectedDate, slots) {
                    val today = Calendar.getInstance()
                    val isToday = selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                  selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                    
                    if (isToday) {
                        val currentTime = System.currentTimeMillis()
                        val targetIndex = slots.indexOfFirst { slot ->
                            val slotStart = selectedDate.clone() as Calendar
                            slotStart.set(Calendar.HOUR_OF_DAY, slot.first)
                            slotStart.set(Calendar.MINUTE, slot.second)
                            slotStart.timeInMillis >= currentTime
                        }
                        
                        if (targetIndex != -1) {
                            listState.scrollToItem(targetIndex)
                        } else if (slots.isNotEmpty()) {
                            listState.scrollToItem(slots.size - 1)
                        }
                    } else {
                        if (slots.isNotEmpty()) {
                            listState.scrollToItem(0)
                        }
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                    items(slots) { slot ->
                        val slotStart = selectedDate.clone() as Calendar
                        slotStart.set(Calendar.HOUR_OF_DAY, slot.first)
                        slotStart.set(Calendar.MINUTE, slot.second)
                        
                        val slotEnd = slotStart.clone() as Calendar
                        slotEnd.add(Calendar.MINUTE, 15) // Check if the 15-min slot is occupied

                        val isPastSlot = slotStart.timeInMillis < Calendar.getInstance().timeInMillis

                        val overlappingBlocked = blockedBlocks.find { block -> 
                            block.first < slotEnd.timeInMillis && block.second > slotStart.timeInMillis 
                        }

                        // An appointment occupies this slot if its start time + duration > slotStart
                        // AND its start time < slotEnd
                        val apptsInSlot = appointments.filter { appt ->
                            val service = services.find { it.id == appt.serviceId }
                            val duration = service?.durationMinutes ?: 30
                            val apptEnd = appt.dateTimestamp + (duration * 60 * 1000)
                            
                            appt.dateTimestamp < slotEnd.timeInMillis && apptEnd > slotStart.timeInMillis
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = apptsInSlot.isEmpty() && !isPastSlot && overlappingBlocked == null) { 
                                    if (services.isEmpty()) {
                                        showNoServicesDialog = true
                                    } else {
                                        navController.navigate("add_appointment?timestamp=${slotStart.timeInMillis}")
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = String.format("%02d:%02d", slot.first, slot.second),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(60.dp).padding(top = 8.dp)
                            )
                            
                            Column(modifier = Modifier.weight(1f)) {
                                if (overlappingBlocked != null) {
                                    val partial = overlappingBlocked.third
                                    Card(
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
                                            Text("Ausencia Parcial: ${partial.type}", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            if (partial.note.isNotBlank()) {
                                                Text(partial.note, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                } else if (apptsInSlot.isEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isPastSlot) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (isPastSlot) "No Disponible" else "+ Disponible", 
                                                color = if (isPastSlot) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                } else {
                                    apptsInSlot.forEach { appt ->
                                        val client = clients.find { it.id == appt.clientId }
                                        val service = services.find { it.id == appt.serviceId }
                                        val isOngoing = appt.dateTimestamp < slotStart.timeInMillis
                                        
                                        val pastelColors = listOf(
                                            androidx.compose.ui.graphics.Color(0xFFE3F2FD), 
                                            androidx.compose.ui.graphics.Color(0xFFF3E5F5), 
                                            androidx.compose.ui.graphics.Color(0xFFE8F5E9), 
                                            androidx.compose.ui.graphics.Color(0xFFFFF3E0), 
                                            androidx.compose.ui.graphics.Color(0xFFFFEBEE), 
                                            androidx.compose.ui.graphics.Color(0xFFE0F7FA), 
                                            androidx.compose.ui.graphics.Color(0xFFFCE4EC)
                                        )
                                        val apptColor = pastelColors[appt.id % pastelColors.size]
                                        
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).clickable { showOptionsForAppt = appt },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isOngoing) apptColor.copy(alpha = 0.6f) 
                                                                 else apptColor
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                val textColor = androidx.compose.ui.graphics.Color(0xFF1E1E1E)
                                                if (isOngoing) {
                                                    Text("Continuación: ${client?.fullName ?: "Desconocido"} (${service?.name ?: "Servicio"})", style = MaterialTheme.typography.bodyMedium, color = textColor)
                                                } else {
                                                    Text("${client?.fullName ?: "Desconocido"} - ${service?.name ?: "Servicio"} (${service?.durationMinutes ?: 0} min)", fontWeight = FontWeight.Bold, color = textColor)
                                                    Text(timeFormat.format(java.util.Date(appt.dateTimestamp)) + " • " + appt.status, style = MaterialTheme.typography.bodySmall, color = textColor)
                                                    if (appt.observations.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Observaciones/Agenda: ${appt.observations}", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.8f))
                                                    }
                                                    if (!client?.observations.isNullOrBlank()) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text("Observaciones/Cliente: ${client?.observations}", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.8f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
            }
        }

        if (showOptionsForAppt != null) {
            val appt = showOptionsForAppt!!
            val client = clients.find { it.id == appt.clientId }
            
            AlertDialog(
                onDismissRequest = { showOptionsForAppt = null },
                title = { Text("Gestionar Turno") },
                text = { Text("¿Qué deseas hacer con el turno de ${client?.fullName ?: "Desconocido"}?") },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            viewModel.deleteAppointment(appt)
                            showOptionsForAppt = null
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                        
                        if (client != null) {
                            IconButton(onClick = {
                                if (client.phone.isBlank()) {
                                    android.widget.Toast.makeText(context, "No es posible realizar el envío porque el cliente no posee un número registrado.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    val template = viewModel.appSettings.whatsappMessageTemplate
                                    val dateStr = dateFormat.format(Date(appt.dateTimestamp))
                                    val timeStr = timeFormat.format(Date(appt.dateTimestamp))
                                    val businessName = viewModel.appSettings.businessName
                                    
                                    val message = template
                                        .replace("{nombre}", client.fullName)
                                        .replace("{fecha}", dateStr)
                                        .replace("{hora}", timeStr)
                                        .replace("{negocio}", businessName)
                                    
                                    sendWhatsAppMessage(context, client.phone, message)
                                    showOptionsForAppt = null
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                            }
                        }

                        IconButton(onClick = {
                            showOptionsForAppt = null
                            navController.navigate("edit_appointment/${appt.id}")
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                        }

                        TextButton(onClick = { showOptionsForAppt = null }) {
                            Text("Cancelar")
                        }
                    }
                }
            )
        }

        if (showNoServicesDialog) {
            AlertDialog(
                onDismissRequest = { showNoServicesDialog = false },
                title = { Text("Atención") },
                text = { Text("Tiene que cargar un Servicio.") },
                confirmButton = {
                    TextButton(onClick = {
                        showNoServicesDialog = false
                        navController.navigate("services") // Redirect to services screen directly
                    }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
