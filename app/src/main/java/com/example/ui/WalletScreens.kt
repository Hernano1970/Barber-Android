package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.data.Wallet
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletSettingsScreen(viewModel: MainViewModel, navController: NavController) {
    val wallets by viewModel.wallets.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<Wallet?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Billeteras") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar Billetera")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (wallets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes billeteras configuradas.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(wallets) { wallet ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { editingWallet = wallet },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(wallet.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Alias: ${wallet.alias}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Titular: ${wallet.titular}", style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { editingWallet = wallet }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar")
                                }
                                IconButton(onClick = { viewModel.deleteWallet(wallet) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingWallet != null) {
        WalletFormDialog(
            wallet = editingWallet,
            onDismiss = {
                showAddDialog = false
                editingWallet = null
            },
            onSave = { wallet ->
                viewModel.updateWallet(wallet)
                showAddDialog = false
                editingWallet = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletFormDialog(wallet: Wallet?, onDismiss: () -> Unit, onSave: (Wallet) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(wallet?.name ?: "Mercado Pago") }
    var alias by remember { mutableStateOf(wallet?.alias ?: "") }
    var cvu by remember { mutableStateOf(wallet?.cvu ?: "") }
    var titular by remember { mutableStateOf(wallet?.titular ?: "") }
    var qrImagePath by remember { mutableStateOf(wallet?.qrImagePath ?: "") }

    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Mercado Pago", "Naranja X", "Ualá", "Cuenta DNI", "Personal Pay", "Otra")

    val fileToCopy = remember { mutableStateOf<Uri?>(null) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            // we will copy the file on Save to avoid dangling files
            fileToCopy.value = uri
            qrImagePath = uri.toString() // temporary preview
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(if (wallet == null) "Nueva Billetera" else "Editar Billetera", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre de la billetera") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    name = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                val clipboardManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Alias") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (clipboardManager.hasPrimaryClip()) {
                                clipboardManager.primaryClip?.getItemAt(0)?.text?.let { pasteText ->
                                    alias = pasteText.toString()
                                }
                            }
                        }) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = "Pegar")
                        }
                    }
                )

                OutlinedTextField(
                    value = cvu,
                    onValueChange = { cvu = it },
                    label = { Text("CVU (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (clipboardManager.hasPrimaryClip()) {
                                clipboardManager.primaryClip?.getItemAt(0)?.text?.let { pasteText ->
                                    cvu = pasteText.toString()
                                }
                            }
                        }) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = "Pegar")
                        }
                    }
                )

                OutlinedTextField(
                    value = titular,
                    onValueChange = { titular = it },
                    label = { Text("Titular de la cuenta") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (clipboardManager.hasPrimaryClip()) {
                                clipboardManager.primaryClip?.getItemAt(0)?.text?.let { pasteText ->
                                    titular = pasteText.toString()
                                }
                            }
                        }) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = "Pegar")
                        }
                    }
                )

                Text("Imagen QR", fontWeight = FontWeight.Bold)
                if (qrImagePath.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = rememberAsyncImagePainter(fileToCopy.value ?: qrImagePath),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(150.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        TextButton(onClick = {
                            qrImagePath = ""
                            fileToCopy.value = null
                        }) {
                            Text("Eliminar QR", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { launcher.launch("image/*") }) {
                            Text("Cambiar QR")
                        }
                    }
                } else {
                    OutlinedButton(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seleccionar QR desde galería")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(onClick = {
                        var finalPath = qrImagePath
                        if (fileToCopy.value != null) {
                            try {
                                val inputStream = context.contentResolver.openInputStream(fileToCopy.value!!)
                                val fileName = "qr_${System.currentTimeMillis()}.jpg"
                                val file = File(context.filesDir, fileName)
                                val outputStream = FileOutputStream(file)
                                inputStream?.copyTo(outputStream)
                                inputStream?.close()
                                outputStream.close()
                                finalPath = file.absolutePath
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        val w = Wallet(
                            id = wallet?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name,
                            alias = alias,
                            cvu = cvu,
                            titular = titular,
                            qrImagePath = finalPath
                        )
                        onSave(w)
                    }) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectQrScreen(viewModel: MainViewModel, navController: NavController, appointmentId: Int) {
    val allAppointments by viewModel.allAppointments.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val services by viewModel.activeServices.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    val appointment = allAppointments.find { it.id == appointmentId }
    val client = clients.find { it.id == appointment?.clientId }
    val service = services.find { it.id == appointment?.serviceId }
    val price = service?.price ?: 0.0

    var selectedWallet by remember { mutableStateOf<Wallet?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(wallets) {
        if (wallets.isNotEmpty() && selectedWallet == null) {
            selectedWallet = wallets.first()
        }
    }

    if (appointment == null) {
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cobro por Transferencia") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            // Resumen
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cliente: ${client?.fullName ?: ""}", style = MaterialTheme.typography.bodyLarge)
                    Text("Servicio: ${service?.name ?: ""}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Importe: ${NumberFormat.getCurrencyInstance(Locale("es", "AR")).format(price)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (wallets.isEmpty()) {
                Text("No tienes billeteras configuradas.", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { navController.navigate("settings_wallets") }) {
                    Text("Configurar Billeteras")
                }
            } else {
                Text("Seleccionar billetera:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedWallet?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        wallets.forEach { w ->
                            DropdownMenuItem(
                                text = { Text(w.name) },
                                onClick = {
                                    selectedWallet = w
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                selectedWallet?.let { w ->
                    // Acciones
                    var showQrFullScreen by remember { mutableStateOf(false) }

                    var showConfirmDialog by remember { mutableStateOf(false) }

                    if (showConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showConfirmDialog = false },
                            title = { Text("Confirmar Pago", fontWeight = FontWeight.Bold) },
                            text = { 
                                Text("¿Confirma que recibió la transferencia correspondiente a este servicio?\n\nUna vez confirmado, el servicio pasará a estado Pagado y se actualizarán los ingresos y estadísticas.") 
                            },
                            confirmButton = {
                                Button(onClick = {
                                    viewModel.updateAppointment(appointment.copy(
                                        isPaid = true,
                                        paymentMethod = "Transferencia",
                                        status = "Pagado"
                                    ))
                                    showConfirmDialog = false
                                    navController.popBackStack()
                                }) {
                                    Text("Confirmar")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showConfirmDialog = false }) {
                                    Text("Cancelar")
                                }
                            }
                        )
                    }

                    if (showQrFullScreen) {
                        Dialog(onDismissRequest = { showQrFullScreen = false }) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(w.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Titular: ${w.titular}", style = MaterialTheme.typography.bodyLarge)
                                    Text("Alias: ${w.alias}", style = MaterialTheme.typography.bodyLarge)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (w.qrImagePath.isNotEmpty()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(w.qrImagePath),
                                            contentDescription = "QR",
                                            modifier = Modifier.size(250.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(200.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Sin imagen QR", color = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = {
                                            showQrFullScreen = false
                                            showConfirmDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Confirmar Pago Recibido")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { showQrFullScreen = false }) {
                                        Text("Cerrar")
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showQrFullScreen = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mostrar QR en Pantalla Completa")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            val template = viewModel.appSettings.whatsappTransferTemplate
                            val clientMsg = template
                                .replace("{nombre}", client?.fullName?.split(" ")?.firstOrNull() ?: "")
                                .replace("{servicio}", service?.name ?: "")
                                .replace("{importe}", "%.2f".format(price))
                                .replace("{billetera}", w.name)
                                .replace("{alias}", w.alias)
                                .replace("{cvu}", w.cvu)
                                .replace("{titular}", w.titular)

                            sendWhatsAppMessageLocal(context, client?.phone ?: "", clientMsg)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Chat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviar Datos por WhatsApp")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (w.qrImagePath.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val file = File(w.qrImagePath)
                                    if (file.exists()) {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/*"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Compartir QR"))
                                    } else {
                                        Toast.makeText(context, "El archivo QR no está disponible.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compartir Imagen QR")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirmar Pago Recibido", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun sendWhatsAppMessageLocal(context: Context, phone: String, message: String) {
    if (phone.isBlank()) {
         Toast.makeText(context, "El cliente no tiene teléfono asignado", Toast.LENGTH_SHORT).show()
         return
    }
    val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
    val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${URLEncoder.encode(message, "UTF-8")}"
    
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
    }
    
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
    }
}
