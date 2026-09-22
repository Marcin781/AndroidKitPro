package com.cyberagent.android

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

/**
 * Safe network-state telemetry.
 *
 * Reads connection metadata exposed by ConnectivityManager only.
 * It does not create a VPN, intercept packets, inspect payloads, or alter traffic.
 */
object NetworkTelemetryCollector {
    private var connectivityManager: ConnectivityManager? = null
    private var callback: ConnectivityManager.NetworkCallback? = null

    fun start(context: Context) {
        val appContext = context.applicationContext
        if (callback != null) return
        val manager = appContext.getSystemService(ConnectivityManager::class.java) ?: return
        connectivityManager = manager

        refresh(appContext, manager)
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refresh(appContext, manager)
            override fun onLost(network: Network) = refresh(appContext, manager)
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
                refresh(appContext, manager)
        }

        runCatching {
            manager.registerDefaultNetworkCallback(networkCallback)
            callback = networkCallback
        }.onFailure {
            NetworkMonitorStore.setStopped(appContext)
        }
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        val manager = connectivityManager
        val registered = callback
        if (manager != null && registered != null) {
            runCatching { manager.unregisterNetworkCallback(registered) }
        }
        callback = null
        connectivityManager = null
        NetworkMonitorStore.setStopped(appContext)
    }

    private fun refresh(context: Context, manager: ConnectivityManager) {
        val networks = runCatching { manager.allNetworks.toList() }.getOrDefault(emptyList())
        var transport = "NONE"
        var validated = false
        val metered = manager.isActiveNetworkMetered
        var vpnPresent = false

        for (network in networks) {
            val caps = manager.getNetworkCapabilities(network) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) vpnPresent = true
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) validated = true

            if (transport == "NONE") {
                transport = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "BLUETOOTH"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                    else -> "OTHER"
                }
            }
        }

        val timestamp = System.currentTimeMillis()
        NetworkMonitorStore.save(
            context = context,
            transport = transport,
            validated = validated,
            metered = metered,
            vpnPresent = vpnPresent,
            activeNetworks = networks.size
        )
        NetworkTelemetryHistoryStore.record(
            context,
            NetworkTelemetryHistoryStore.Event(
                timestamp = timestamp,
                transport = transport,
                validated = validated,
                metered = metered,
                vpnPresent = vpnPresent,
                activeNetworks = networks.size
            )
        )
    }
}
