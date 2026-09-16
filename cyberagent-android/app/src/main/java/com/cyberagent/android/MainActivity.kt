package com.cyberagent.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CyberAgentScreen() }
    }
}

@Composable
private fun CyberAgentScreen() {
    var status by remember { mutableStateOf("Gotowy do skanowania") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("CyberAgent", style = MaterialTheme.typography.headlineMedium)
            Text("Defensywny agent bezpieczeństwa Android")
            Text(status)
            Button(enabled = !busy, onClick = {
                busy = true
                status = "Sprawdzanie backendu…"
                scope.launch {
                    status = if (checkBackend()) "Backend ONLINE ✓" else "Backend OFFLINE"
                    busy = false
                }
            }) { Text("Sprawdź backend") }
        }
    }
}

private suspend fun checkBackend(): Boolean = withContext(Dispatchers.IO) {
    val connection = (URL("https://cyberagent-api.onrender.com/health").openConnection() as HttpURLConnection)
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.responseCode in 200..299
    } finally { connection.disconnect() }
}
