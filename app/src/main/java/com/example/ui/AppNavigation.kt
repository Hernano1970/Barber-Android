package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard, androidx.compose.ui.graphics.Color(0xFF6200EE))
    object Billing : Screen("billing", "Pagos", Icons.Filled.AttachMoney, androidx.compose.ui.graphics.Color(0xFFFF9800))
    object Clients : Screen("clients", "Clientes", Icons.Filled.People)
    object Services : Screen("services", "Servicios", Icons.Filled.ContentCut)
    object Agenda : Screen("agenda", "Agenda", Icons.Filled.CalendarMonth, androidx.compose.ui.graphics.Color(0xFF4CAF50))
    object Settings : Screen("settings", "Ajustes", Icons.Filled.Settings, androidx.compose.ui.graphics.Color(0xFF2196F3))
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Agenda,
    Screen.Billing,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarberApp(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val businessName by viewModel.businessName.collectAsState()
    val businessColorval by viewModel.businessColor.collectAsState()

    Scaffold(
        topBar = {
            if (bottomNavItems.any { it.route == currentRoute } || currentRoute == null) {
                TopAppBar(
                    title = { 
                        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            coil.compose.AsyncImage(
                                model = com.example.R.mipmap.ic_launcher,
                                contentDescription = "App Icon",
                                modifier = androidx.compose.ui.Modifier.size(36.dp).padding(end = 8.dp)
                            )
                            Text(businessName, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) 
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = androidx.compose.ui.graphics.Color(businessColorval)
                    )
                )
            }
        },
        bottomBar = {
            if (bottomNavItems.any { it.route == currentRoute } || currentRoute == null) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title, tint = item.color) },
                            label = { Text(item.title, color = item.color) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = item.color.copy(alpha = 0.2f)
                            ),
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel, navController)
            }
            composable(Screen.Billing.route) {
                BillingScreen(viewModel, navController)
            }
            composable(Screen.Clients.route) {
                ClientsScreen(viewModel, navController)
            }
            composable(Screen.Services.route) {
                ServicesScreen(viewModel, navController)
            }
            composable(Screen.Agenda.route) {
                AgendaScreen(viewModel, navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel, navController)
            }
            composable("statistics") {
                StatisticsScreen(viewModel, navController)
            }
            composable("settings_business") {
                BusinessProfileScreen(viewModel, navController)
            }
            composable("settings_hours") {
                WorkingHoursScreen(viewModel, navController)
            }
            composable("settings_agenda") {
                AgendaSettingsScreen(viewModel, navController)
            }
            composable("settings_vacations") {
                VacationsScreen(viewModel, navController)
            }
            composable("settings_whatsapp") {
                WhatsAppSettingsScreen(viewModel, navController)
            }
            composable("settings_notifications") {
                NotificationsSettingsScreen(viewModel, navController)
            }
            composable("settings_backup") {
                BackupSettingsScreen(viewModel, navController)
            }
            composable("settings_wallets") {
                WalletSettingsScreen(viewModel, navController)
            }
            composable("ausencias_read_only") {
                AusenciasReadOnlyScreen(viewModel, navController)
            }
            composable("upcoming_appointments") {
                UpcomingAppointmentsScreen(viewModel, navController)
            }
            // Add Client
            composable("add_client") {
                AddClientScreen(viewModel, navController)
            }
            // Import Contacts
            composable("import_contacts") {
                ImportContactsScreen(viewModel, navController)
            }
            // Edit Client
            composable(
                route = "edit_client/{id}",
                arguments = listOf(androidx.navigation.navArgument("id") { 
                    type = androidx.navigation.NavType.IntType
                })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                EditClientScreen(viewModel, navController, id)
            }
            // Add Service
            composable("add_service") {
                AddServiceScreen(viewModel, navController)
            }
            // Edit Service
            composable(
                route = "edit_service/{id}",
                arguments = listOf(androidx.navigation.navArgument("id") { 
                    type = androidx.navigation.NavType.IntType
                })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                EditServiceScreen(viewModel, navController, id)
            }
            // Add Appointment
            composable(
                route = "add_appointment?timestamp={timestamp}",
                arguments = listOf(androidx.navigation.navArgument("timestamp") { 
                    type = androidx.navigation.NavType.LongType
                    defaultValue = -1L 
                })
            ) { backStackEntry ->
                val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: -1L
                AddAppointmentScreen(viewModel, navController, timestamp)
            }
            // Edit Appointment
            composable(
                route = "edit_appointment/{id}",
                arguments = listOf(androidx.navigation.navArgument("id") { 
                    type = androidx.navigation.NavType.IntType
                })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                EditAppointmentScreen(viewModel, navController, id)
            }
            // Collect QR
            composable(
                route = "collect_qr/{id}",
                arguments = listOf(androidx.navigation.navArgument("id") { 
                    type = androidx.navigation.NavType.IntType
                })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                CollectQrScreen(viewModel, navController, id)
            }
        }
    }
}
