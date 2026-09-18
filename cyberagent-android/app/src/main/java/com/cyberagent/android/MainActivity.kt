package com.cyberagent.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleBackgroundScan()
        setContent { CyberAgentScreen() }
    }

    private fun scheduleBackgroundScan() {
        val request = PeriodicWorkRequestBuilder<ScanWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "cyberagent_periodic_scan", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }
}

@Composable
private fun CyberAgentScreen() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Gotowy do skanowania") }
    var coach by remember { mutableStateOf("Cyber Coach: uruchom skanowanie, aby rozpocząć analizę.") }
    var busy by remember { mutableStateOf(false) }
    var findings by remember { mutableStateOf<List<AppScanner.AppFinding>>(emptyList()) }
    var selected by remember { mutableStateOf<AppScanner.AppFinding?>(null) }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("CyberAgent", style = MaterialTheme.typography.headlineMedium)
            Text("Defensywny agent bezpieczeństwa Android")
            Text(status)
            Text(coach)

            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    status = "Skanowanie aplikacji i obliczanie SHA-256…"
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) { AppScanner(context).scan() }
                            findings = result
                            ScanResultStore.save(context, result)
                            val high = result.count { it.risk == RiskLevel.HIGH }
                            val review = result.count { it.risk == RiskLevel.REVIEW }
                            status = "Aplikacje: ${result.size} • HIGH: ${high} • REVIEW: ${review} • SAFE: ${result.size - high - review}"
                            coach = "Cyber Coach: ${CyberCoach.advice(result)}"
                        } catch (e: Exception) {
                            status = "Błąd skanowania: ${e.message ?: "nieznany błąd"}"
                        } finally {
                            busy = false
                        }
                    }
                }
            ) {
                Text(if (busy) "Skanowanie…" else "Skanuj aplikacje")
            }

            if (findings.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(findings, key = { it.packageName }) { finding ->
                        AppFindingCard(finding) { selected = finding }
                    }
                }
            }
        }

        selected?.let { finding ->
            AlertDialog(
                onDismissRequest = { selected = null },
                title = { Text(finding.label) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Ryzyko: ${finding.risk}")
                        Text("Pakiet: ${finding.packageName}")
                        Text("Wersja: ${finding.versionName ?: "brak"}")
                        Text("SHA-256: ${finding.apkSha256 ?: "niedostępny"}")
                        Text("Powody: ${finding.reasons.joinToString("; ")}")
                        Text("Uprawnienia: ${finding.permissions.size}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selected = null }) { Text("Zamknij") }
                }
            )
        }
    }
}

@Composable
private fun AppFindingCard(finding: AppScanner.AppFinding, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(finding.label, style = MaterialTheme.typography.titleMedium)
                Text(finding.risk.name)
            }
            Text(finding.packageName, style = MaterialTheme.typography.bodySmall)
            Text(finding.reasons.firstOrNull() ?: "Brak sygnałów")
        }
    }
}
