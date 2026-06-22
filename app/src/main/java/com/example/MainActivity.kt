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

class MainActivity : ComponentActivity() {
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

    setContent {
      MyApplicationTheme {
        if (dbError != null) {
            DatabaseErrorScreen(
                errorMessage = dbError!!,
                onExit = { finish() }
            )
        } else {
            com.example.ui.BarberApp()
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
