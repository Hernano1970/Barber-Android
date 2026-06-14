package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.Absence
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AusenciasReadOnlyScreen(viewModel: MainViewModel, navController: NavController) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")) }
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    var selectedFilter by remember { mutableStateOf("Todas") }
    val filters = listOf("Todas", "Próximas", "Activas", "Finalizadas")

    val allAbsences = viewModel.appSettings.absencesList

    val filteredAbsences = remember(allAbsences, selectedFilter, todayStart) {
        val now = System.currentTimeMillis() // Assuming todayStart is just 00:00, use now for more precise "Activa" if needed, but we'll use todayStart for day boundaries.
        
        allAbsences.filter { absence ->
            val isActive = todayStart in absence.start..absence.end || (absence.start == todayStart && absence.end == todayStart)
            val isFuture = absence.start > now
            val isPast = absence.end < todayStart

            when (selectedFilter) {
                "Próximas" -> isFuture && !isActive
                "Activas" -> isActive
                "Finalizadas" -> isPast
                else -> true
            }
        }.sortedBy { it.start }
    }

    var selectedAbsence by remember { mutableStateOf<Absence?>(null) }

    if (selectedAbsence != null) {
        AbsenceDetailDialog(
            absence = selectedAbsence!!,
            onDismiss = { selectedAbsence = null },
            formatter = formatter,
            todayStart = todayStart
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ausencias Registradas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
            ScrollableTabRow(
                selectedTabIndex = filters.indexOf(selectedFilter),
                edgePadding = 0.dp
            ) {
                filters.forEachIndexed { index, filterName ->
                    Tab(
                        selected = selectedFilter == filterName,
                        onClick = { selectedFilter = filterName },
                        text = { Text(filterName) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (filteredAbsences.isEmpty()) {
                Text(
                    "No hay ausencias registradas para esta vista.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredAbsences, key = { it.id }) { absence ->
                        AbsenceReadOnlyCard(
                            absence = absence,
                            formatter = formatter,
                            todayStart = todayStart,
                            onClick = { selectedAbsence = absence }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AbsenceReadOnlyCard(absence: Absence, formatter: SimpleDateFormat, todayStart: Long, onClick: () -> Unit) {
    val now = System.currentTimeMillis()
    val isActive = todayStart in absence.start..absence.end || (absence.start == todayStart && absence.end == todayStart)
    val isPast = absence.end < todayStart
    val stateText = when {
        isActive -> "Activa"
        isPast -> "Finalizada"
        else -> "Próxima"
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (absence.isPartial) "Ausencia Parcial" else "Ausencia Extensa",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when(stateText) {
                        "Activa" -> MaterialTheme.colorScheme.error
                        "Próxima" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            val startStr = formatter.format(Date(absence.start))
            val endStr = formatter.format(Date(absence.end))
            if (absence.isPartial) {
                Text("$startStr | ${absence.startTime} a ${absence.endTime}", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Desde: $startStr - Hasta: $endStr", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Motivo: ${absence.type}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AbsenceDetailDialog(absence: Absence, onDismiss: () -> Unit, formatter: SimpleDateFormat, todayStart: Long) {
    val now = System.currentTimeMillis()
    val isActive = todayStart in absence.start..absence.end || (absence.start == todayStart && absence.end == todayStart)
    val isPast = absence.end < todayStart
    val stateText = when {
        isActive -> "Activa"
        isPast -> "Finalizada"
        else -> "Próxima"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Detalle de Ausencia")
        },
        text = {
            Column {
                Text("Tipo: ", fontWeight = FontWeight.Bold)
                Text(if (absence.isPartial) "Parcial" else "Extensa")
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Motivo: ", fontWeight = FontWeight.Bold)
                Text(absence.type)
                Spacer(modifier = Modifier.height(8.dp))

                val startStr = formatter.format(Date(absence.start))
                val endStr = formatter.format(Date(absence.end))

                Text("Fecha: ", fontWeight = FontWeight.Bold)
                if (absence.isPartial) {
                    Text("$startStr")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Horario: ", fontWeight = FontWeight.Bold)
                    Text("${absence.startTime} - ${absence.endTime}")
                } else {
                    Text("$startStr al $endStr")
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text("Estado: ", fontWeight = FontWeight.Bold)
                Text(stateText)
                
                if (absence.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nota: ", fontWeight = FontWeight.Bold)
                    Text(absence.note)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
