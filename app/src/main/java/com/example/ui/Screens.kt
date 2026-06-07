package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Resumen de Hoy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCard(
                title = "Turnos Hoy",
                value = todayAppointments.size.toString(),
                icon = Icons.Filled.CalendarToday,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            StatCard(
                title = "Total Clientes",
                value = totalClients.toString(),
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
                title = "Ingresos Hoy",
                value = "$${"%.0f".format(todayIncome)}",
                icon = Icons.Filled.AttachMoney,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Próximos Turnos", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (todayAppointments.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No hay turnos para hoy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(todayAppointments.sortedBy { it.dateTimestamp }) { appt ->
                    val client = clients.find { it.id == appt.clientId }
                    val service = activeServices.find { it.id == appt.serviceId }
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val isPending = !appt.isPaid

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(timeFormat.format(Date(appt.dateTimestamp)), fontWeight = FontWeight.Bold)
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
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}



@Composable
fun ClientsScreen(viewModel: MainViewModel, navController: NavController) {
    val clients by viewModel.clients.collectAsState()
    var showOptionsForClient by remember { mutableStateOf<com.example.data.Client?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_client") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Client")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(clients) { client ->
                ListItem(
                    headlineContent = { Text(client.fullName, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(client.phone) },
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
                            viewModel.deleteClient(client)
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
    }
}

@Composable
fun ServicesScreen(viewModel: MainViewModel, navController: NavController) {
    val services by viewModel.activeServices.collectAsState()

    Scaffold(
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
                    trailingContent = { Text("$${service.price}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
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

    var selectedDate by remember { mutableStateOf(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }) }

    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("es", "ES"))
    val timeFormat = SimpleDateFormat("HH:mm", Locale("es", "ES"))

    var showOptionsForAppt by remember { mutableStateOf<com.example.data.Appointment?>(null) }

    val dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK)
    val workingHours = viewModel.appSettings.getWorkingHours(dayOfWeek)
    val absenceDay = viewModel.appSettings.getAbsenceForDate(selectedDate.timeInMillis)
    val isAbsence = absenceDay != null

    val startHour = workingHours?.first ?: 8
    val endHour = workingHours?.second ?: 20
    val hours = (startHour..endHour).toList()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_appointment") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Appointment")
            }
        }
    ) { padding ->
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
                                if (note.isNotBlank()) "$type / $note" else type
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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(hours) { hour ->
                        val slotStart = selectedDate.clone() as Calendar
                        slotStart.set(Calendar.HOUR_OF_DAY, hour)
                        
                        val slotEnd = slotStart.clone() as Calendar
                        slotEnd.add(Calendar.HOUR_OF_DAY, 1)

                        val apptsInSlot = appointments.filter { 
                            it.dateTimestamp >= slotStart.timeInMillis && it.dateTimestamp < slotEnd.timeInMillis 
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    navController.navigate("add_appointment?timestamp=${slotStart.timeInMillis}")
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = String.format("%02d:00", hour),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(60.dp).padding(top = 8.dp)
                            )
                            
                            Column(modifier = Modifier.weight(1f)) {
                                if (apptsInSlot.isEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().height(60.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("+ Disponible", color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                } else {
                                    apptsInSlot.forEach { appt ->
                                        val client = clients.find { it.id == appt.clientId }
                                        val service = services.find { it.id == appt.serviceId }
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).clickable { showOptionsForAppt = appt },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text("${client?.fullName ?: "Desconocido"} - ${service?.name ?: "Servicio"}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                Text(timeFormat.format(Date(appt.dateTimestamp)) + " • " + appt.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                if (appt.observations.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Nota turno: ${appt.observations}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                                }
                                                if (!client?.observations.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Nota cliente: ${client?.observations}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
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
                    TextButton(onClick = {
                        showOptionsForAppt = null
                        navController.navigate("edit_appointment/${appt.id}")
                    }) {
                        Text("Editar", color = MaterialTheme.colorScheme.primary)
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            viewModel.deleteAppointment(appt)
                            showOptionsForAppt = null
                        }) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                        
                        val context = LocalContext.current
                        if (client != null && client.phone.isNotBlank()) {
                            TextButton(onClick = {
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
                            }) {
                                Text("WhatsApp", color = androidx.compose.ui.graphics.Color(0xFF25D366))
                            }
                        }

                        TextButton(onClick = { showOptionsForAppt = null }) {
                            Text("Cancelar")
                        }
                    }
                }
            )
        }
    }
}
