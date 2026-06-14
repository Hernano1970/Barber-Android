package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.data.Appointment
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(viewModel: MainViewModel, navController: NavController) {
    val clients by viewModel.clients.collectAsState()
    val services by viewModel.activeServices.collectAsState()
    val allAppointments by viewModel.allAppointments.collectAsState()

    var selectedTab by remember { mutableStateOf("Todos") } // Todos, Pagados, Deudores
    var paymentDialogAppt by remember { mutableStateOf<Appointment?>(null) }
    var deleteConfirmAppt by remember { mutableStateOf<Appointment?>(null) }

    // Stats calculations
    val now = Calendar.getInstance()
    
    val todayStart = now.clone() as Calendar
    todayStart.set(Calendar.HOUR_OF_DAY, 0); todayStart.set(Calendar.MINUTE, 0); todayStart.set(Calendar.SECOND, 0)
    
    val weekStart = now.clone() as Calendar
    weekStart.set(Calendar.DAY_OF_WEEK, weekStart.firstDayOfWeek)
    weekStart.set(Calendar.HOUR_OF_DAY, 0); weekStart.set(Calendar.MINUTE, 0); weekStart.set(Calendar.SECOND, 0)

    val monthStart = now.clone() as Calendar
    monthStart.set(Calendar.DAY_OF_MONTH, 1)
    monthStart.set(Calendar.HOUR_OF_DAY, 0); monthStart.set(Calendar.MINUTE, 0); monthStart.set(Calendar.SECOND, 0)

    var todayIncome = 0.0
    var weekIncome = 0.0
    var monthIncome = 0.0
    
    var monthCash = 0.0
    var monthCard = 0.0
    var monthTrans = 0.0

    allAppointments.filter { it.isPaid }.forEach { appt ->
        val service = services.find { it.id == appt.serviceId }
        val price = service?.price ?: 0.0
        
        if (appt.dateTimestamp >= todayStart.timeInMillis) todayIncome += price
        if (appt.dateTimestamp >= weekStart.timeInMillis) weekIncome += price
        if (appt.dateTimestamp >= monthStart.timeInMillis) {
            monthIncome += price
            when (appt.paymentMethod) {
                "Efectivo" -> monthCash += price
                "Tarjeta" -> monthCard += price
                "Transferencia" -> monthTrans += price
            }
        }
    }

    val filteredList = allAppointments.filter { appt ->
        when (selectedTab) {
            "Pagados" -> appt.isPaid
            "Deudores" -> !appt.isPaid
            else -> true
        }
    }.sortedByDescending { it.dateTimestamp }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pagos y Facturación") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Top Cards (horizontal scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard("Hoy", "$${"%.2f".format(todayIncome)}", Icons.Filled.CalendarToday, MaterialTheme.colorScheme.secondaryContainer)
                SummaryCard("Esta Semana", "$${"%.2f".format(weekIncome)}", Icons.Filled.ViewWeek, MaterialTheme.colorScheme.tertiaryContainer)
                SummaryCard("Este Mes", "$${"%.2f".format(monthIncome)}", Icons.Filled.TrendingUp, MaterialTheme.colorScheme.primaryContainer)
                
                // Method Breakdown
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Métodos (Mensual)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.width(120.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Efe", style = MaterialTheme.typography.bodySmall); Text("$${"%.0f".format(monthCash)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.width(120.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tarj", style = MaterialTheme.typography.bodySmall); Text("$${"%.0f".format(monthCard)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.width(120.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Transf.", style = MaterialTheme.typography.bodySmall); Text("$${"%.0f".format(monthTrans)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Tab row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                val tabs = listOf("Todos", "Pagados", "Deudores")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row {
                        tabs.forEach { tab ->
                            val isSelected = selectedTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { selectedTab = tab }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = tab,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Table Header
            val columnWidths = listOf(100.dp, 130.dp, 100.dp, 90.dp, 90.dp, 130.dp, 90.dp, 100.dp)
            val headerScrollState = rememberScrollState()
            
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(headerScrollState).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("FECHA", modifier = Modifier.width(columnWidths[0]), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("CLIENTE", modifier = Modifier.width(columnWidths[1]), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("SERVICIO", modifier = Modifier.width(columnWidths[2]), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("MÉTODO", modifier = Modifier.width(columnWidths[3]), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("ESTADO", modifier = Modifier.width(columnWidths[4]), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("FECHA T.", modifier = Modifier.width(columnWidths[5]), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("MONTO", modifier = Modifier.width(columnWidths[6]), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("", modifier = Modifier.width(columnWidths[7]), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Table Content
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredList) { appt ->
                    val client = clients.find { it.id == appt.clientId }
                    val service = services.find { it.id == appt.serviceId }
                    val price = service?.price ?: 0.0
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    val dateTimeFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                    
                    val dateFormatted = dateFormat.format(Date(appt.dateTimestamp))
                    val turnFormatted = dateTimeFormat.format(Date(appt.dateTimestamp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (!appt.isPaid) paymentDialogAppt = appt }
                            .horizontalScroll(headerScrollState) // sync scroll. NOTE: keeping scroll state in sync for multiple items is tricky, but this applies to each row individually usually. Better approach is horizontalScroll on a parent Column containing the list, but let's just make the rows scrollable for now or wrap the LazyColumn in a horizontal scroll.
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayMethod = if (appt.isPaid) {
                            if (appt.paymentMethod == "Transferencia") "Transf." else appt.paymentMethod
                        } else "-"

                        Text(dateFormatted, modifier = Modifier.width(columnWidths[0]), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(client?.fullName ?: "Desconocido", modifier = Modifier.width(columnWidths[1]), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(service?.name ?: "Servicio", modifier = Modifier.width(columnWidths[2]), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        
                        Text(displayMethod, modifier = Modifier.width(columnWidths[3]), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if(appt.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        
                        // Estado chip
                        Box(
                            modifier = Modifier
                                .width(columnWidths[4])
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (appt.isPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (appt.isPaid) "Pagado" else "Pendiente", 
                                fontSize = 11.sp, 
                                color = if (appt.isPaid) Color(0xFF2E7D32) else Color(0xFFE65100),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(turnFormatted, modifier = Modifier.width(columnWidths[5]), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$${"%.2f".format(price)}", modifier = Modifier.width(columnWidths[6]), fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        
                        Row(
                            modifier = Modifier.width(columnWidths[7]),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (appt.isPaid) {
                                IconButton(
                                    onClick = {
                                        viewModel.updateAppointment(appt.copy(
                                            isPaid = false, 
                                            paymentMethod = "", 
                                            status = "Pendiente"
                                        ))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Marcar Pendiente", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            IconButton(
                                onClick = { deleteConfirmAppt = appt },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (deleteConfirmAppt != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmAppt = null },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro que deseas eliminar este turno? Esta acción no se puede deshacer y lo eliminará del calendario.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAppointment(deleteConfirmAppt!!)
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

    // Payment Dialog
    if (paymentDialogAppt != null) {
        val appt = paymentDialogAppt!!
        val client = clients.find { it.id == appt.clientId }
        val service = services.find { it.id == appt.serviceId }
        val price = service?.price ?: 0.0

        var selectedMethod by remember { mutableStateOf("Efectivo") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { paymentDialogAppt = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Confirmar Pago", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { paymentDialogAppt = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Monto a cobrar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${"%.2f".format(price)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Cliente:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(client?.fullName ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Servicio:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(service?.name ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Método de Pago", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedMethod,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            listOf("Efectivo", "Tarjeta", "Transferencia").forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        selectedMethod = method
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { paymentDialogAppt = null }) {
                            Text("Cancelar", color = MaterialTheme.colorScheme.error)
                        }
                        Button(onClick = {
                            viewModel.updateAppointment(appt.copy(
                                isPaid = true,
                                paymentMethod = selectedMethod,
                                status = "Pagado" // Also update text status just in case
                            ))
                            paymentDialogAppt = null
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Marcar Pagado")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bgColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
