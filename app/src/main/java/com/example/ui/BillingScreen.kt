package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
        if (appt.status == "Eliminado") return@filter false
        when (selectedTab) {
            "Pagados" -> appt.isPaid
            "Deudores" -> !appt.isPaid
            else -> true
        }
    }.sortedByDescending { it.dateTimestamp }

    Surface(color = Color(0xFFF9F9F9), modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFF3E0))
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("$", color = Color(0xFFFF9800), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Pagos y Facturación", color = Color(0xFFFF9800), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Top Cards (horizontal scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard("Hoy", "$${"%.2f".format(todayIncome)}", Icons.Filled.CalendarToday, Color(0xFFE8EAF6), Color(0xFF1A1A1A))
                SummaryCard("Esta Semana", "$${"%.2f".format(weekIncome)}", Icons.Filled.ViewWeek, Color(0xFFFCE4EC), Color(0xFF1A1A1A))
                SummaryCard("Este Mes", "$${"%.2f".format(monthIncome)}", Icons.AutoMirrored.Filled.TrendingUp, Color(0xFFE3F2FD), Color(0xFF1A1A1A))
                MethodsCard(cash = monthCash, card = monthCard, trans = monthTrans)
            }

            // Tab row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val tabs = listOf("Todos", "Pagados", "Deudores")
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFE8EAF6)
                ) {
                    Row {
                        tabs.forEach { tab ->
                            val isSelected = selectedTab == tab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(if (isSelected) Color(0xFF5C6BC0) else Color.Transparent)
                                    .clickable { selectedTab = tab }
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = tab,
                                    color = if (isSelected) Color.White else Color(0xFF5C6BC0),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Table Header
            val columnWidths = listOf(110.dp, 130.dp, 120.dp, 90.dp, 90.dp, 130.dp, 90.dp, 100.dp)
            val headerScrollState = rememberScrollState()
            
            Surface(color = Color(0xFFE8EAF6)) {
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(headerScrollState).padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Text("FECHA", modifier = Modifier.width(columnWidths[0]), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1A1A1A))
                    Text("CLIENTE", modifier = Modifier.width(columnWidths[1]), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1A1A1A))
                    Text("SERVICIO", modifier = Modifier.width(columnWidths[2]), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1A1A1A))
                    Text("MÉTODO", modifier = Modifier.width(columnWidths[3]), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1A1A1A))
                    Text("ESTADO", modifier = Modifier.width(columnWidths[4]), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1A1A1A))
                    Text("FECHA T.", modifier = Modifier.width(columnWidths[5]), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1A1A1A))
                    Text("MONTO", modifier = Modifier.width(columnWidths[6]), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1A1A1A))
                    Text("", modifier = Modifier.width(columnWidths[7]), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1A1A1A))
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
                    
                    val dateFormatted = dateFormat.format(Date(if (appt.createdAt > 0) appt.createdAt else appt.dateTimestamp))
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

                        Text(dateFormatted, modifier = Modifier.width(columnWidths[0]), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF333333))
                        Text(client?.fullName ?: "Desconocido", modifier = Modifier.width(columnWidths[1]), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF333333))
                        Text(service?.name ?: "Servicio", modifier = Modifier.width(columnWidths[2]), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF333333))
                        
                        Text(displayMethod, modifier = Modifier.width(columnWidths[3]), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF333333))
                        
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

                    if (selectedMethod == "Transferencia") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                viewModel.updateAppointment(appt.copy(
                                    isPaid = true,
                                    paymentMethod = selectedMethod,
                                    status = "Pagado"
                                ))
                                paymentDialogAppt = null
                            }, modifier = Modifier.weight(0.8f), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                                Text("Rápido", fontSize = 13.sp, maxLines = 1)
                            }
                            Button(onClick = {
                                paymentDialogAppt = null
                                navController.navigate("collect_qr/${appt.id}")
                            }, modifier = Modifier.weight(1.2f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Cobrar", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("QR / CVU / Alias", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TextButton(onClick = { paymentDialogAppt = null }) {
                                Text("Cancelar", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
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
}

@Composable
fun SummaryCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bgColor: Color, textColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = textColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = textColor)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun MethodsCard(cash: Double, card: Double, trans: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Métodos de Pago",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF444444),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Efe.", fontSize = 14.sp, color = Color(0xFF555555))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$${if (cash == cash.toLong().toDouble()) cash.toLong() else "%.2f".format(cash)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF444444))
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFCCCCCC)))
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tarj.", fontSize = 14.sp, color = Color(0xFF555555))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$${if (card == card.toLong().toDouble()) card.toLong() else "%.2f".format(card)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF444444))
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFCCCCCC)))
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Transf.", fontSize = 14.sp, color = Color(0xFF555555))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$${if (trans == trans.toLong().toDouble()) trans.toLong() else "%.2f".format(trans)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF444444))
                }
            }
        }
    }
}
