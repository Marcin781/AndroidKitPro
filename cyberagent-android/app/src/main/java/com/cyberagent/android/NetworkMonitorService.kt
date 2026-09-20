package com.cyberagent.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.IBinder
import androidx.core.app.ServiceCompat

/**
 * Read-only network telemetry.
 * It observes Android connectivity metadata only; it does not create a VPN,
 * capture packets, inspect payloads, decrypt traffic, or collect domains/URLs.
 */
class NetworkMonitorService : Service() {
    companion object {
        const val ACTION_START = "com.cyberagent.android.action.START_NETWORK_MONITOR"
        const val ACTION_STOP = "com.cyberagent.android.action.STOP_NETWORK_MONITOR"
        const val CHANNEL_ID = "cyberagent_network"
        const val NOTIFICATION_ID = 2001
    }

    private lateinit var connectivityManager: ConnectivityManager
    private val activeNetworks = mutableSetOf<Network>()
    private var registered = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            activeNetworks += network
            publish(network)
        }

        override fun onLost(network: Network) {
            activeNetworks -= network
            val fallback = connectivityManager.activeNetwork
            if (fallback != null) publish(fallback)
            else {
                NetworkMonitorStore.save(
                    applicationContext, "NONE", false, false, false, activeNetworks.size
                )
            }
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            publish(network, capabilities)
        }
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopMonitoring()
            ACTION_START -> startMonitoring()
        }
        return START_NOT_STICKY
    }

    private fun startMonitoring() {
        if (registered) return

        startForeground(NOTIFICATION_ID, notification())
        registered = true
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)
        }.onFailure {
            registered = false
            NetworkMonitorStore.setStopped(applicationContext)
            stopSelf()
        }
    }

    private fun stopMonitoring() {
        if (registered) {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            registered = false
        }
        activeNetworks.clear()
        NetworkMonitorStore.setStopped(applicationContext)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publish(network: Network, supplied: NetworkCapabilities? = null) {
        val caps = supplied ?: connectivityManager.getNetworkCapabilities(network) ?: return
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Other"
        }
        NetworkMonitorStore.save(
            applicationContext,
            transport,
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
            activeNetworks.size
        )
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "CyberAgent — monitoring sieci",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun notification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("CyberAgent monitoruje sieć")
            .setContentText("Analizowane są wyłącznie metadane połączenia.")
            .setOngoing(true)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (registered) {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            registered = false
        }
        activeNetworks.clear()
        NetworkMonitorStore.setStopped(applicationContext)
        super.onDestroy()
    }
}
