package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.data.Client
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PhoneContact(val name: String, val phone: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContactsScreen(viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar Contactos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (hasPermission) {
            ContactsListScreen(padding, viewModel, navController)
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Se requiere permiso para leer los contactos.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.READ_CONTACTS) }) {
                        Text("Solicitar Permiso")
                    }
                }
            }
        }
    }
}

@Composable
fun ContactsListScreen(padding: PaddingValues, viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var allContacts by remember { mutableStateOf<List<PhoneContact>>(emptyList()) }
    var duplicateWarning by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var missingPhoneContact by remember { mutableStateOf<PhoneContact?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val contacts = mutableListOf<PhoneContact>()
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                ),
                null, null, null
            )
            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val name = it.getString(nameIndex) ?: ""
                    val hasPhone = it.getInt(hasPhoneIndex) > 0
                    var number = ""
                    if (hasPhone) {
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(id),
                            null
                        )
                        phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                val numberIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (numberIndex != -1) {
                                    number = pc.getString(numberIndex) ?: ""
                                    number = number.replace(Regex("[^0-9+]"), "")
                                }
                            }
                        }
                    }
                    if (name.isNotBlank()) {
                        contacts.add(PhoneContact(name, number))
                    }
                }
            }
            // Remove exact duplicates and sort alphabetically
            val collator = java.text.Collator.getInstance(java.util.Locale("es", "ES")).apply { strength = java.text.Collator.PRIMARY }
            allContacts = contacts.distinctBy { it.phone }.sortedWith { a, b -> collator.compare(a.name, b.name) }
        }
    }

    val filteredContacts = if (searchQuery.isBlank()) {
        allContacts
    } else {
        allContacts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar contacto...") },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            singleLine = true
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filteredContacts) { contact ->
                ListItem(
                    headlineContent = { Text(contact.name, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                    supportingContent = { Text(contact.phone) },
                    trailingContent = {
                        Button(onClick = {
                            val isDuplicate = if (contact.phone.isNotBlank()) {
                                clients.any { it.isPermanent && (it.phone == contact.phone || it.phone.replace(Regex("[^0-9+]"), "") == contact.phone) }
                            } else false
                            if (isDuplicate) {
                                duplicateWarning = "Este cliente ya se encuentra registrado."
                            } else if (contact.phone.isBlank()) {
                                missingPhoneContact = contact
                            } else {
                                viewModel.addClient(contact.name, contact.phone, "")
                                snackbarMessage = "Contacto importado: ${contact.name}"
                            }
                        }) {
                            Text("Importar")
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (missingPhoneContact != null) {
        val contact = missingPhoneContact!!
        AlertDialog(
            onDismissRequest = { missingPhoneContact = null },
            title = { Text("ATENCIÓN") },
            text = { Text("Este cliente no tiene un número de teléfono registrado. Si continúa, no será posible enviar notificaciones ni recordatorios de turnos mediante WhatsApp. ¿Desea guardar el cliente de todas formas?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addClient(contact.name, contact.phone, "")
                    snackbarMessage = "Contacto importado: ${contact.name}"
                    missingPhoneContact = null
                }) { Text("Guardar de Todas Formas", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { missingPhoneContact = null }) { Text("Volver y Completar Teléfono") }
            }
        )
    }

    if (duplicateWarning != null) {
        AlertDialog(
            onDismissRequest = { duplicateWarning = null },
            title = { Text("Atención") },
            text = { Text(duplicateWarning!!) },
            confirmButton = {
                TextButton(onClick = { duplicateWarning = null }) { Text("OK") }
            }
        )
    }
    
    if (snackbarMessage != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { snackbarMessage = null }) { Text("OK") }
            }
        ) {
            Text(snackbarMessage!!)
        }
    }
}
