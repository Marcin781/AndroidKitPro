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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleBackgroundScan()
        setContent { CyberAgentScreen() }
    }

    private fun scheduleBackgroundScan() {
        val request = PeriodicWorkRequestBuilder<ScanWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "cyberagent_periodic_scan",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
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
                status = "Skanowanie aplikacji…"
                scope.launch {
                    val result = withContext(Dispatchers.IO) { AppScanner(androidx.compose.ui.platform.LocalContext.current).scan() }
                    val high = result.count { it.risk == RiskLevel.HIGH }
                    val review = result.count { it.risk == RiskLevel.REVIEW }
                    status = "Aplikacje: ${result.size}\nHIGH: $high\nREVIEW: $review\nSAFE: ${result.size - high - review}"
                    busy = false
                }
            }) { Text("Skanuj aplikacje") }
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
