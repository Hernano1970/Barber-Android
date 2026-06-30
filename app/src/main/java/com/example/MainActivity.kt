package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.MyApplicationTheme
import com.example.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Force preview refresh 25
class MainActivity : ComponentActivity() {
    // Triggering a refresh for the preview
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))
    enableEdgeToEdge()
    
    var dbError by androidx.compose.runtime.mutableStateOf<String?>(null)
    
    // Verificar que la base de datos puede abrirse correctamente
    try {
        val testDb = AppDatabase.getDatabase(this)
        // Forzamos abrir la base de datos para ejecutar validaciones y migraciones
        testDb.openHelper.readableDatabase.version
    } catch (e: Exception) {
        e.printStackTrace()
        dbError = e.message ?: "Error desconocido"
    }

    val licenseManager = com.example.license.LicenseManager(this)

    setContent {
      MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        if (dbError != null) {
            DatabaseErrorScreen(
                errorMessage = dbError!!,
                onExit = { finish() }
            )
        } else {
            val context = androidx.compose.ui.platform.LocalContext.current
            val rememberedLicenseManager = remember { com.example.license.LicenseManager(context) }
            var isBlocked by remember { mutableStateOf(rememberedLicenseManager.shouldBlockApp()) }
            var warningMsg by remember { mutableStateOf(rememberedLicenseManager.getWarningMessage()) }
            var showWarningDialog by remember { mutableStateOf(warningMsg != null) }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_START || event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        isBlocked = rememberedLicenseManager.shouldBlockApp()
                        warningMsg = rememberedLicenseManager.getWarningMessage()
                        showWarningDialog = warningMsg != null
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(Unit) {
                while(true) {
                    kotlinx.coroutines.delay(60000)
                    isBlocked = rememberedLicenseManager.shouldBlockApp()
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                com.example.ui.BarberApp()
                
                if (isBlocked) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        com.example.ui.LicenseBlockedScreen(
                            onLicenseActivated = { 
                                isBlocked = rememberedLicenseManager.shouldBlockApp()
                                warningMsg = rememberedLicenseManager.getWarningMessage()
                                showWarningDialog = warningMsg != null
                            }
                        )
                    }
                }
                
                if (!isBlocked && showWarningDialog && warningMsg != null) {
                    AlertDialog(
                        onDismissRequest = { showWarningDialog = false },
                        title = { Text("Aviso de Licencia") },
                        text = { Text(warningMsg!!) },
                        confirmButton = {
                            TextButton(onClick = { showWarningDialog = false }) {
                                Text("Entendido")
                            }
                        }
                    )
                }
            }
        }
      }
    }
  }
}

@Composable
fun DatabaseErrorScreen(errorMessage: String, onExit: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Error Crítico de Base de Datos",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("No se pudo iniciar la aplicación debido a un problema con la base de datos. Ningún dato ha sido borrado, pero requerirá atención técnica o restaurar un respaldo reciente.")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Detalles del error:", fontWeight = FontWeight.Bold)
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text("Salir de la aplicación")
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}
