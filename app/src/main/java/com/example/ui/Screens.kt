package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val allAppointments by viewModel.allAppointments.collectAsState()
    val activeServices by viewModel.activeServices.collectAsState()
    val clients by viewModel.clients.collectAsState()

    var todayIncome = 0.0
    todayAppointments.filter { it.isPaid }.forEach { appt ->
        val service = activeServices.find { it.id == appt.serviceId }
        todayIncome += (service?.price ?: 0.0)
    }

    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val todayEnd = todayStart + 24 * 60 * 60 * 1000L
    
    val absencesList = viewModel.appSettings.absencesList.filter { it.end >= todayStart }
    val extensasCount = absencesList.count { !it.isPartial }
    val parcialesCount = absencesList.count { it.isPartial }
    val hasAttentionAbsence = absencesList.any { it.start <= todayStart + 7 * 24 * 60 * 60 * 1000L }

    val activeTodayCount = todayAppointments.count { it.status != "Cancelado" && it.status != "Eliminado" }
    val activeUpcomingCount = allAppointments.count { it.dateTimestamp >= todayEnd && it.status != "Cancelado" && it.status != "Eliminado" }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Panel de Control", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCard(
                title = "Turnos Hoy / Próximos",
                value = "$activeTodayCount / $activeUpcomingCount",
                icon = Icons.Filled.CalendarToday,
                modifier = Modifier.weight(1f),
                iconTint = androidx.compose.ui.graphics.Color(0xFF2196F3),
                onClick = { navController.navigate("upcoming_appointments") }
            )
            Spacer(modifier = Modifier.width(16.dp))
            StatCard(
                title = "Total Clientes",
                value = clients.count { it.isPermanent }.toString(),
                icon = Icons.Filled.People,
                modifier = Modifier.weight(1f),
                iconTint = androidx.compose.ui.graphics.Color(0xFF4CAF50)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
             StatCard(
                title = "Servicios Activos",
                value = activeServices.size.toString(),
                icon = Icons.Filled.DesignServices,
                modifier = Modifier.weight(1f),
                iconTint = androidx.compose.ui.graphics.Color(0xFF9C27B0)
            )
             Spacer(modifier = Modifier.width(16.dp))
             StatCard(
                title = "Estadísticas",
                value = "$${"%.0f".format(todayIncome)}",
                icon = Icons.Filled.BarChart,
                modifier = Modifier.weight(1f),
                iconTint = androidx.compose.ui.graphics.Color(0xFFFF9800),
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
                            Icon(Icons.Filled.EventBusy, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFFF44336))
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
        
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Notifications section
            NotificationQuickAction(
                icon = Icons.Filled.Notifications,
                label = "Turnos",
                isActive = viewModel.appSettings.turnReminderEnabled,
                activeColor = androidx.compose.ui.graphics.Color(0xFF2196F3),
                onClick = { navController.navigate("settings_notifications") }
            )
            NotificationQuickAction(
                icon = Icons.Filled.WbSunny,
                label = "Jornada",
                isActive = viewModel.appSettings.dailyStartReminderEnabled,
                activeColor = androidx.compose.ui.graphics.Color(0xFFFF9800),
                onClick = { navController.navigate("settings_notifications") }
            )
            NotificationQuickAction(
                icon = Icons.Filled.EventBusy,
                label = "Ausencias",
                isActive = viewModel.appSettings.absenceNotificationsEnabled,
                activeColor = androidx.compose.ui.graphics.Color(0xFFF44336),
                onClick = { navController.navigate("settings_notifications") }
            )
            NotificationQuickAction(
                icon = Icons.Filled.ListAlt,
                label = "Resumen",
                isActive = viewModel.appSettings.dailySummaryEnabled,
                activeColor = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                onClick = { navController.navigate("settings_notifications") }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("Accesos Rápidos", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            QuickActionButton(
                icon = Icons.Filled.People,
                label = "Clientes",
                color = androidx.compose.ui.graphics.Color(0xFF2196F3)
            ) {
                navController.navigate("clients")
            }
            QuickActionButton(
                icon = Icons.Filled.ContentCut,
                label = "Servicios",
                color = androidx.compose.ui.graphics.Color(0xFF9C27B0)
            ) {
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

    val upcomingAppointments = allAppointments.filter { it.dateTimestamp >= today && it.status != "Eliminado" }.sortedBy { it.dateTimestamp }

    val dateFormatForColor = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val dateToColorMap = remember(upcomingAppointments) {
        val uniqueDates = upcomingAppointments.map { dateFormatForColor.format(Date(it.dateTimestamp)) }.distinct()
        val pastelColors = listOf(
            Color(0xFFE3F2FD), // Celeste pastel
            Color(0xFFE8F5E9), // Verde pastel
            Color(0xFFFFF9C4), // Amarillo pastel
            Color(0xFFF3E5F5), // Lila pastel
            Color(0xFFFCE4EC), // Rosa pastel
            Color(0xFFFFF3E0)  // Durazno pastel
        )
        uniqueDates.mapIndexed { index, dateStr ->
            dateStr to pastelColors[index % pastelColors.size]
        }.toMap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Turnos de Hoy y Próximos", color = Color(0xFF1976D2))
                    }
                },
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
                    
                    val dateStr = dateFormatForColor.format(Date(appt.dateTimestamp))
                    val cardColor = dateToColorMap[dateStr] ?: MaterialTheme.colorScheme.surfaceVariant

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
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
fun NotificationQuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, activeColor: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) activeColor else androidx.compose.ui.graphics.Color.Gray,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .width(140.dp)
            .height(80.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, iconTint: androidx.compose.ui.graphics.Color? = null, onClick: (() -> Unit)? = null) {
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Card(modifier = modifier.then(clickModifier), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = iconTint ?: MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
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
    var deleteConfirmAppt by remember { mutableStateOf<com.example.data.Appointment?>(null) }
    var showWhatsAppConfirmAppt by remember { mutableStateOf<com.example.data.Appointment?>(null) }
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

    Column(modifier = Modifier.fillMaxSize()) {
            
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 4.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(Color(0xFFE8F5E9))
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = "Agenda",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Agenda",
                        color = Color(0xFF4CAF50),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

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

                        val toleranceMillis = viewModel.appSettings.agendaToleranceMinutes * 60 * 1000L
                        val currentMillis = Calendar.getInstance().timeInMillis
                        val isPastSlot = (slotStart.timeInMillis + toleranceMillis) < currentMillis
                        val isByTolerance = slotStart.timeInMillis < currentMillis && !isPastSlot

                        val overlappingBlocked = blockedBlocks.find { block -> 
                            block.first < slotEnd.timeInMillis && block.second > slotStart.timeInMillis 
                        }

                        // An appointment occupies this slot if its start time + duration > slotStart
                        // AND its start time < slotEnd
                        val apptsInSlot = appointments.filter { appt ->
                            val service = services.find { it.id == appt.serviceId }
                            val duration = service?.durationMinutes ?: 30
                            val apptEnd = appt.dateTimestamp + (duration * 60 * 1000)
                            
                            appt.dateTimestamp < slotEnd.timeInMillis && apptEnd > slotStart.timeInMillis && appt.status != "Eliminado"
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
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isPastSlot) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = if (isPastSlot) "No Disponible" else "+ Disponible", 
                                                    color = if (isPastSlot) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                )
                                                if (isByTolerance) {
                                                    Text(
                                                        text = "⚠ Disponible por tolerancia",
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
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
                                                    val lastSent = viewModel.appSettings.getWhatsAppSentTime(appt.id)
                                                    if (lastSent != null) {
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm")
                                                        Text("Último WhatsApp enviado: ${sdf.format(java.util.Date(lastSent))}", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.8f))
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
                            deleteConfirmAppt = appt
                            showOptionsForAppt = null
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                        
                        if (client != null) {
                            IconButton(onClick = {
                                if (client.phone.isBlank()) {
                                    android.widget.Toast.makeText(context, "No es posible realizar el envío porque el cliente no posee un número registrado.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    val lastSent = viewModel.appSettings.getWhatsAppSentTime(appt.id)
                                    if (lastSent != null) {
                                        showWhatsAppConfirmAppt = appt
                                        showOptionsForAppt = null
                                    } else {
                                        viewModel.appSettings.markWhatsAppSent(appt.id)
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

        if (deleteConfirmAppt != null) {
            val deleteAppt = deleteConfirmAppt!!
            if (deleteAppt.isPaid) {
                AlertDialog(
                    onDismissRequest = { deleteConfirmAppt = null },
                    title = { Text("Eliminar Servicio") },
                    text = { Text("Este servicio ya fue registrado como Pagado.\n\nSi elimina este registro:\n• Se eliminará de la Agenda.\n• Se eliminará de Pagos y Facturación.\n• Los ingresos y estadísticas ya contabilizados NO serán modificados.\n\n¿Desea continuar?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteAppointment(deleteAppt)
                                deleteConfirmAppt = null
                            }
                        ) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteConfirmAppt = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            } else {
                AlertDialog(
                    onDismissRequest = { deleteConfirmAppt = null },
                    title = { Text("Confirmar Eliminación") },
                    text = { Text("¿Estás seguro que deseas eliminar este turno? Esta acción no se puede deshacer y lo eliminará del calendario.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteAppointment(deleteAppt)
                                deleteConfirmAppt = null
                            }
                        ) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteConfirmAppt = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }

        if (showWhatsAppConfirmAppt != null) {
            val appt = showWhatsAppConfirmAppt!!
            val client = clients.find { it.id == appt.clientId }
            AlertDialog(
                onDismissRequest = { showWhatsAppConfirmAppt = null },
                title = { Text("WhatsApp ya enviado") },
                text = { Text("Ya se envió un WhatsApp para este turno.\n\n¿Desea enviarlo nuevamente?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.appSettings.markWhatsAppSent(appt.id)
                        val template = viewModel.appSettings.whatsappMessageTemplate
                        val dateStr = dateFormat.format(Date(appt.dateTimestamp))
                        val timeStr = timeFormat.format(Date(appt.dateTimestamp))
                        val businessName = viewModel.appSettings.businessName
                        
                        val message = template
                            .replace("{nombre}", client?.fullName ?: "")
                            .replace("{fecha}", dateStr)
                            .replace("{hora}", timeStr)
                            .replace("{negocio}", businessName)
                        
                        sendWhatsAppMessage(context, client?.phone ?: "", message)
                        showWhatsAppConfirmAppt = null
                    }) {
                        Text("Sí, enviar nuevamente")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWhatsAppConfirmAppt = null }) {
                        Text("Cancelar")
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
