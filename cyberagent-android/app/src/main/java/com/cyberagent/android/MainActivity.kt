package com.cyberagent.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    var networkHistory by remember { mutableStateOf(NetworkTelemetryHistoryStore.load(context)) }
    var alerts by remember { mutableStateOf(NetworkAlertStore.load(context)) }
    var feedCount by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        feedCount = withContext(Dispatchers.IO) { ThreatFeedClient.fetch()?.indicatorCount }
    }

    LaunchedEffect(networkEnabled) {
        while (networkEnabled) {
            networkSnapshot = NetworkMonitorStore.load(context)
            networkHistory = NetworkTelemetryHistoryStore.load(context)
            alerts = NetworkAlertStore.load(context)
            delay(2000)
        }
    }

    val background = Brush.verticalGradient(
        listOf(Color(0xFF07111F), Color(0xFF0B1830), Color(0xFF10152A))
    )

    Box(Modifier.fillMaxSize().background(background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("CYBERAGENT", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF7DD3FC))
                    Text("Defensywny agent bezpieczeństwa Android", color = Color(0xFFD7E3F4))
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF13233D)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("STATUS", style = MaterialTheme.typography.labelLarge, color = Color(0xFF38BDF8))
                        Text(status, color = Color.White)
                        Text(coach, color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (networkEnabled) {
                            NetworkTelemetryCollector.stop(context)
                            networkEnabled = false
                            status = "Monitoring sieci wyłączony"
                        } else {
                            NetworkTelemetryCollector.start(context)
                            networkEnabled = true
                            status = "Monitoring sieci uruchomiony — tylko telemetria"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (networkEnabled) Color(0xFFB91C1C) else Color(0xFF2563EB)
                    )
                ) {
                    Text(if (networkEnabled) "Wyłącz monitoring sieci" else "Włącz monitoring sieci")
                }
            }

            item {
                DashboardCard("IOC / Threat Intelligence", Color(0xFFA78BFA)) {
                    Text(if (feedCount == null) "Feed: niedostępny" else "IOC w feedzie: ${feedCount}", color = Color.White)
                    Text("Dopasowanie: dokładne IOC • bez automatycznego blokowania",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                }
            }

            networkSnapshot?.let { snapshot ->
                item {
                    DashboardCard("Telemetria sieci", Color(0xFF22D3EE)) {
                        Text("Transport: ${snapshot.transport}", color = Color.White)
                        Text("Internet zweryfikowany: ${if (snapshot.validated) "tak" else "nie"}", color = Color(0xFFD7E3F4))
                        Text("Sieć taryfikowana: ${if (snapshot.metered) "tak" else "nie"}", color = Color(0xFFD7E3F4))
                        Text("VPN obecny: ${if (snapshot.vpnPresent) "tak" else "nie"}", color = Color(0xFFD7E3F4))
                        Text("Aktywne sieci: ${snapshot.activeNetworks}", color = Color(0xFFD7E3F4))
                    }
                }
            }

            if (networkHistory.isNotEmpty()) {
                item { SectionTitle("Historia zmian sieci", Color(0xFF34D399)) }
                items(networkHistory.take(10)) { event ->
                    DashboardCard("${event.transport} • sieci: ${event.activeNetworks}", Color(0xFF34D399)) {
                        Text(
                            "Zweryfikowana: ${if (event.validated) "tak" else "nie"} • " +
                                "taryfikowana: ${if (event.metered) "tak" else "nie"} • " +
                                "VPN: ${if (event.vpnPresent) "tak" else "nie"}",
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            if (alerts.isNotEmpty()) {
                item { SectionTitle("Ostatnie alerty IOC", Color(0xFFFBBF24)) }
                items(alerts.take(10)) { alert ->
                    DashboardCard("${alert.type}: ${alert.indicator}", Color(0xFFFBBF24)) {
                        Text(alert.reason, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                    }
                }
            }

            item {
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
                                history = ScanResultStore.loadHistory(context)
                                val high = result.count { it.risk == RiskLevel.HIGH }
                                val review = result.count { it.risk == RiskLevel.REVIEW }
                                status = "Aplikacje: ${result.size} • HIGH: $high • REVIEW: $review • SAFE: ${result.size - high - review}"
                                coach = "Cyber Coach: ${CyberCoach.advice(result)}"
                                ThreatNotification.showHighRisk(context, high)
                            } catch (e: Exception) {
                                status = "Błąd skanowania: ${e.message ?: "nieznany błąd"}"
                            } finally {
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Text(if (busy) "Skanowanie…" else "Skanuj aplikacje")
                }
            }

            if (findings.isNotEmpty()) {
                item { SectionTitle("Wyniki bieżącego skanu", Color(0xFF60A5FA)) }
                items(findings, key = { it.packageName }) { finding ->
                    AppFindingCard(finding) { selected = finding }
                }
            }

            if (history.isNotEmpty()) {
                item { SectionTitle("Historia skanów", Color(0xFF818CF8)) }
                items(history.take(10)) { item -> ScanHistoryRow(item) }
            }

            item { Spacer(Modifier.height(24.dp)) }
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
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Zamknij") } }
        )
    }
}

@Composable
private fun DashboardCard(title: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111C30)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier.width(5.dp).heightIn(min = 76.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                    .background(accent)
            )
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                content()
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, accent: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.width(4.dp).height(24.dp)
                .clip(RoundedCornerShape(4.dp)).background(accent)
        )
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
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