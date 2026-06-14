package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: MainViewModel, navController: NavController) {
    val allAppointments by viewModel.allAppointments.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val services by viewModel.activeServices.collectAsState()
    val appSettings = viewModel.appSettings
    val statisticsStartDate = appSettings.statisticsStartDate

    // Filter selections
    var periodFilter by remember { mutableStateOf("Hoy") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Period Filter Header
            ScrollableTabRow(
                selectedTabIndex = listOf("Hoy", "7 Días", "Este Mes", "Este Año").indexOf(periodFilter),
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Hoy", "7 Días", "Este Mes", "Este Año").forEach { tab ->
                    Tab(
                        selected = periodFilter == tab,
                        onClick = { periodFilter = tab },
                        text = { Text(tab) }
                    )
                }
            }

            // Calculation Logic
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val startTime: Long
            val endTime: Long = System.currentTimeMillis() + 86400000L // Ensure today is fully covered? Wait, if we use End of Today it's better
            
            val todayEndCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val currentEndTime = todayEndCal.timeInMillis
            
            var absencesEndTime = currentEndTime

            when (periodFilter) {
                "Hoy" -> {
                    startTime = cal.timeInMillis
                    absencesEndTime = currentEndTime
                }
                "7 Días" -> {
                    val back7 = Calendar.getInstance()
                    back7.add(Calendar.DAY_OF_YEAR, -6) // Included today is 7 days
                    back7.set(Calendar.HOUR_OF_DAY, 0)
                    back7.set(Calendar.MINUTE, 0)
                    back7.set(Calendar.SECOND, 0)
                    back7.set(Calendar.MILLISECOND, 0)
                    startTime = back7.timeInMillis
                    absencesEndTime = currentEndTime
                }
                "Este Mes" -> {
                    val backMonth = Calendar.getInstance()
                    backMonth.set(Calendar.DAY_OF_MONTH, 1)
                    backMonth.set(Calendar.HOUR_OF_DAY, 0)
                    backMonth.set(Calendar.MINUTE, 0)
                    backMonth.set(Calendar.SECOND, 0)
                    backMonth.set(Calendar.MILLISECOND, 0)
                    startTime = backMonth.timeInMillis
                    
                    val endMonth = Calendar.getInstance()
                    endMonth.set(Calendar.DAY_OF_MONTH, endMonth.getActualMaximum(Calendar.DAY_OF_MONTH))
                    endMonth.set(Calendar.HOUR_OF_DAY, 23)
                    endMonth.set(Calendar.MINUTE, 59)
                    endMonth.set(Calendar.SECOND, 59)
                    absencesEndTime = endMonth.timeInMillis
                }
                "Este Año" -> {
                    val backYear = Calendar.getInstance()
                    backYear.set(Calendar.DAY_OF_YEAR, 1)
                    backYear.set(Calendar.HOUR_OF_DAY, 0)
                    backYear.set(Calendar.MINUTE, 0)
                    backYear.set(Calendar.SECOND, 0)
                    backYear.set(Calendar.MILLISECOND, 0)
                    startTime = backYear.timeInMillis
                    
                    val endYear = Calendar.getInstance()
                    endYear.set(Calendar.DAY_OF_YEAR, endYear.getActualMaximum(Calendar.DAY_OF_YEAR))
                    endYear.set(Calendar.HOUR_OF_DAY, 23)
                    endYear.set(Calendar.MINUTE, 59)
                    endYear.set(Calendar.SECOND, 59)
                    absencesEndTime = endYear.timeInMillis
                }
                else -> startTime = 0L
            }
            
            // Adjust to respect statisticsStartDate
            val effectiveStartTime = maxOf(startTime, statisticsStartDate)

            val filteredAppts = allAppointments.filter { it.dateTimestamp in effectiveStartTime..currentEndTime && it.status != "Cancelado" }
            val paidAppts = filteredAppts.filter { it.isPaid }

            val totalTurnos = filteredAppts.size
            val clientsAtendidos = filteredAppts.map { it.clientId }.distinct().size
            
            var ingresosTotales = 0.0
            paidAppts.forEach { appt ->
                val service = services.find { it.id == appt.serviceId }
                ingresosTotales += (service?.price ?: 0.0)
            }
            val promServicio = if (paidAppts.isNotEmpty()) ingresosTotales / paidAppts.size else 0.0

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjetas Resumen
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCardBasic(title = "Total Turnos", value = totalTurnos.toString(), modifier = Modifier.weight(1f))
                    StatCardBasic(title = "Clientes", value = clientsAtendidos.toString(), modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCardBasic(title = "Ingresos Totales", value = "$${"%.0f".format(ingresosTotales)}", modifier = Modifier.weight(1f))
                    StatCardBasic(title = "Promedio/Servicio", value = "$${"%.0f".format(promServicio)}", modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                // Servicios Más Realizados
                Text("Servicios Más Realizados", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                val serviceCounts = filteredAppts.groupBy { it.serviceId }
                    .map { (id, list) -> 
                        val serviceName = services.find { it.id == id }?.name ?: "Eliminado/Desconocido"
                        Pair(serviceName, list.size)
                    }.sortedByDescending { it.second }
                
                if (serviceCounts.isEmpty()) {
                    Text("No hay servicios realizados en este período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    serviceCounts.take(5).forEachIndexed { index, pair ->
                        val (name, count) = pair
                        val perc = if (totalTurnos > 0) count.toFloat() / totalTurnos else 0f
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${index + 1}. $name", fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text("$count (${"%.1f".format(perc * 100)}%)", style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { perc },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                // Mejores Clientes
                var showAllClients by remember { mutableStateOf(false) }
                Text("Mejores Clientes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                val clientStats = filteredAppts.groupBy { it.clientId }
                    .map { (id, list) -> 
                        val clientName = if (id == -1) "Casual / Manual" else clients.find { it.id == id }?.fullName ?: "Desconocido"
                        val visits = list.size
                        val totalSpent = list.filter { it.isPaid }.sumOf { appt -> services.find { s -> s.id == appt.serviceId }?.price ?: 0.0 }
                        Triple(clientName, visits, totalSpent)
                    }.sortedByDescending { it.third }.also {
                         if (it.isEmpty() || (it.size == 1 && it[0].second == 0)) emptyList<Triple<String, Int, Double>>()
                    }

                if (clientStats.isEmpty()) {
                    Text("No hay clientes en este período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val limit = if (showAllClients) clientStats.size else minOf(5, clientStats.size)
                    clientStats.take(limit).forEachIndexed { index, triple ->
                        val (name, visits, spent) = triple
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${index + 1}. $name", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$visits visitas", style = MaterialTheme.typography.bodySmall)
                                    Text("$${"%.0f".format(spent)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                    if (clientStats.size > 5 && !showAllClients) {
                        TextButton(onClick = { showAllClients = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Ver Todos")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Métodos de Pago
                Text("Métodos de Pago", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                val methodStats = paidAppts.groupBy { it.paymentMethod }
                    .map { (method, list) ->
                        val count = list.size
                        val total = list.sumOf { appt -> services.find { s -> s.id == appt.serviceId }?.price ?: 0.0 }
                        Triple(method.ifEmpty { "Otro" }, count, total)
                    }.sortedByDescending { it.third }

                if (methodStats.isEmpty() || ingresosTotales == 0.0) {
                    Text("No hay pagos en este período.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    methodStats.forEach { triple ->
                        val (method, count, total) = triple
                        val perc = if (ingresosTotales > 0) total / ingresosTotales else 0.0
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(method, fontWeight = FontWeight.Medium)
                                Text("$${"%.0f".format(total)} (${"%.1f".format(perc * 100)}%)", style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { perc.toFloat() },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                            Text("$count op.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Ingresos por día (Gráfico simple)
                Text("Ingresos por Día", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                val dfDay = SimpleDateFormat("dd/MM", Locale.getDefault())
                val dailyIncomes = paidAppts.groupBy {
                    val c = Calendar.getInstance().apply { timeInMillis = it.dateTimestamp }
                    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                    c.timeInMillis
                }.mapValues { (_, list) ->
                    list.sumOf { appt -> services.find { s -> s.id == appt.serviceId }?.price ?: 0.0 }
                }.toSortedMap()

                if (dailyIncomes.isEmpty()) {
                    Text("No hay ingresos para graficar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    var maxIncome = dailyIncomes.values.maxOrNull()?.toFloat() ?: 1f
                    if (maxIncome <= 0f) maxIncome = 1f
                    val daysCount = dailyIncomes.size
                    
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp).padding(vertical = 16.dp)) {
                        val width = size.width
                        val height = size.height
                        
                        val points = mutableListOf<Offset>()
                        val keys = dailyIncomes.keys.toList()
                        
                        if (keys.size == 1) {
                            // Point in center
                            points.add(Offset(width / 2f, height - (dailyIncomes[keys[0]]!!.toFloat() / maxIncome) * height))
                            drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = points[0])
                        } else {
                            val stepX = width / (keys.size - 1)
                            keys.forEachIndexed { i, key ->
                                val income = dailyIncomes[key]!!.toFloat()
                                val x = i * stepX
                                val y = height - (income / maxIncome) * height
                                points.add(Offset(x, y))
                            }
                            
                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].x, points[i].y)
                                }
                            }
                            
                            drawPath(
                                path = path,
                                color = primaryColor,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            points.forEach { pt ->
                                drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = pt)
                            }
                        }
                    }
                    
                    // X-axis labels (first, middle, last if many)
                    val keys = dailyIncomes.keys.toList()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (keys.isNotEmpty()) {
                            Text(dfDay.format(Date(keys.first())), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (keys.size > 2) {
                                Text(dfDay.format(Date(keys[keys.size / 2])), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (keys.size > 1) {
                                Text(dfDay.format(Date(keys.last())), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Estadísticas de Ausencias
                Text("Estadísticas de Ausencias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Incluye ausencias y bloqueos pasados y futuros dentro del período seleccionado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                val absences = appSettings.absencesList.filter { it.start <= absencesEndTime && it.end >= effectiveStartTime } // Full intersection
                val extensas = absences.filter { !it.isPartial }
                val parciales = absences.filter { it.isPartial }

                val now = System.currentTimeMillis()
                
                // Breakdowns Realizadas vs Programadas
                val extensasRealizadas = extensas.filter { it.start <= now }
                val extensasProgramadas = extensas.filter { it.start > now }
                
                val parcialesRealizadas = parciales.filter { it.start <= now }
                val parcialesProgramadas = parciales.filter { it.start > now }

                var sumHoursParciales = 0f
                parciales.forEach {
                    val sParts = it.startTime.split(":")
                    val eParts = it.endTime.split(":")
                    if (sParts.size == 2 && eParts.size == 2) {
                        val sH = sParts[0].toFloat() + sParts[1].toFloat() / 60f
                        val eH = eParts[0].toFloat() + eParts[1].toFloat() / 60f
                        if (eH > sH) sumHoursParciales += (eH - sH)
                    }
                }

                var sumDaysExtensas = 0
                extensas.forEach {
                    val realStart = maxOf(it.start, effectiveStartTime)
                    val realEnd = minOf(it.end, absencesEndTime)
                    val diff = realEnd - realStart
                    if (diff > 0) {
                        sumDaysExtensas += (diff / 86400000L).toInt() + 1
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ausencias Extensas", fontWeight = FontWeight.Medium)
                            Text("${extensas.size} (${sumDaysExtensas} días)", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (extensas.isNotEmpty()) {
                            Text("  • Realizadas: ${extensasRealizadas.size}", style = MaterialTheme.typography.bodySmall)
                            Text("  • Programadas: ${extensasProgramadas.size}", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ausencias Parciales", fontWeight = FontWeight.Medium)
                            Text("${parciales.size} (${"%.1f".format(sumHoursParciales)} hrs)", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (parciales.isNotEmpty()) {
                            Text("  • Realizadas: ${parcialesRealizadas.size}", style = MaterialTheme.typography.bodySmall)
                            Text("  • Programadas: ${parcialesProgramadas.size}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Botón Reiniciar
                var showResetDialog by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reiniciar Estadísticas")
                }

                Spacer(modifier = Modifier.height(32.dp))

                if (showResetDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetDialog = false },
                        title = { Text("Reiniciar Estadísticas") },
                        text = { Text("Esta acción eliminará únicamente los datos estadísticos acumulados. No eliminará clientes, servicios, turnos, pagos ni configuraciones. ¿Desea continuar?") },
                        confirmButton = {
                            TextButton(onClick = {
                                appSettings.statisticsStartDate = System.currentTimeMillis()
                                showResetDialog = false
                            }) {
                                Text("Reiniciar", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showResetDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }

            }
        }
    }
}

@Composable
fun StatCardBasic(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
