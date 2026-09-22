package com.cyberagent.android

import android.Manifest
import android.content.pm.PackageManager
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
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        scheduleBackgroundScan()
        setContent { CyberAgentScreen() }
    }
    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
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
    var history by remember { mutableStateOf(ScanResultStore.loadHistory(context)) }
    var selected by remember { mutableStateOf<AppScanner.AppFinding?>(null) }
    var networkEnabled by remember { mutableStateOf(NetworkMonitorStore.isRunning(context)) }
    var networkSnapshot by remember { mutableStateOf(NetworkMonitorStore.load(context)) }
    var alerts by remember { mutableStateOf(NetworkAlertStore.load(context)) }
    var feedCount by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        feedCount = withContext(Dispatchers.IO) { ThreatFeedClient.fetch()?.indicatorCount }
    }

    LaunchedEffect(networkEnabled) {
        while (networkEnabled) {
            networkSnapshot = NetworkMonitorStore.load(context)
            delay(2000)
        }
    }
    MaterialTheme {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("CyberAgent", style = MaterialTheme.typography.headlineMedium)
            Text("Defensywny agent bezpieczeństwa Android")
            Text(status)
            Text(coach)
            Button(onClick = {
                if (networkEnabled) {
                    NetworkTelemetryCollector.stop(context)
                    networkEnabled = false
                } else {
                    NetworkTelemetryCollector.start(context)
                    networkEnabled = true
                    status = "Monitoring sieci uruchomiony — tylko telemetria ConnectivityManager"
                }
            }) {
                Text(if (networkEnabled) "Wyłącz monitoring sieci" else "Włącz monitoring sieci")
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("IOC / Threat Intelligence", style = MaterialTheme.typography.titleMedium)
                    Text(if (feedCount == null) "Feed: niedostępny" else "IOC w feedzie: $feedCount")
                    Text("Dopasowanie: tylko dokładne IOC — bez automatycznego blokowania")
                }
            }
            networkSnapshot?.let { snapshot ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Telemetria sieci", style = MaterialTheme.typography.titleMedium)
                        Text("Transport: ${snapshot.transport}")
                        Text("Internet zweryfikowany: ${if (snapshot.validated) "tak" else "nie"}")
                        Text("Sieć taryfikowana: ${if (snapshot.metered) "tak" else "nie"}")
                        Text("VPN obecny: ${if (snapshot.vpnPresent) "tak" else "nie"}")
                        Text("Aktywne sieci: ${snapshot.activeNetworks}")
                    }
                }
            }
            if (alerts.isNotEmpty()) {
                Text("Ostatnie alerty IOC", style = MaterialTheme.typography.titleMedium)
                alerts.take(5).forEach { alert ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            Text("${alert.type}: ${alert.indicator}", style = MaterialTheme.typography.bodyMedium)
                            Text(alert.reason, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Button(enabled = !busy, onClick = {
                busy = true
                status = "Skanowanie aplikacji i obliczanie SHA-256…"
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) { AppScanner(context).scan() }
                        findings = result
                        ScanResultStore.save(context, result)
                        history = ScanResultStore.loadHistory(context)
                        val high = result.count { it.risk == RiskLevel.HIGH }
                        val review = result.count { it.risk == RiskLevel.REVIEW }
                        status = "Aplikacje: ${result.size} • HIGH: ${high} • REVIEW: ${review} • SAFE: ${result.size - high - review}"
                        coach = "Cyber Coach: ${CyberCoach.advice(result)}"
                        ThreatNotification.showHighRisk(context, high)
                    } catch (e: Exception) {
                        status = "Błąd skanowania: ${e.message ?: "nieznany błąd"}"
                    } finally { busy = false }
                }
            }) { Text(if (busy) "Skanowanie…" else "Skanuj aplikacje") }
            if (history.isNotEmpty()) {
                Text("Historia skanów", style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(history.take(10)) { item -> ScanHistoryRow(item) }
                }
            } else if (findings.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(findings, key = { it.packageName }) { finding -> AppFindingCard(finding) { selected = finding } }
                }
            }
        }
        selected?.let { finding ->
            AlertDialog(onDismissRequest = { selected = null }, title = { Text(finding.label) },
                text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Ryzyko: ${finding.risk}")
                    Text("Pakiet: ${finding.packageName}")
                    Text("Wersja: ${finding.versionName ?: "brak"}")
                    Text("SHA-256: ${finding.apkSha256 ?: "niedostępny"}")
                    Text("Powody: ${finding.reasons.joinToString("; ")}")
                    Text("Uprawnienia: ${finding.permissions.size}")
                }},
                confirmButton = { TextButton(onClick = { selected = null }) { Text("Zamknij") } })
        }
    }
}

@Composable
private fun ScanHistoryRow(item: ScanResultStore.Summary) {
    val date = remember(item.scannedAt) { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.scannedAt)) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Text(date, style = MaterialTheme.typography.labelMedium)
            Text("Aplikacje: ${item.apps} • HIGH: ${item.high} • REVIEW: ${item.review} • SAFE: ${item.safe}")
        }
    }
}

@Composable
private fun AppFindingCard(finding: AppScanner.AppFinding, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(finding.label, style = MaterialTheme.typography.titleMedium)
                Text(finding.risk.name)
            }
            Text(finding.packageName, style = MaterialTheme.typography.bodySmall)
            Text(finding.reasons.firstOrNull() ?: "Brak sygnałów")
        }
    }
}