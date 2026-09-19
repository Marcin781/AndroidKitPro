package com.cyberagent.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

/**
 * Consent-based network monitoring foundation.
 * The v4 service establishes the VPN permission flow only; it does not
 * inspect, decrypt, redirect, or modify packet contents.
 */
class NetworkMonitorService : VpnService() {
    companion object {
        const val ACTION_START = "com.cyberagent.android.action.START_NETWORK_MONITOR"
        const val ACTION_STOP = "com.cyberagent.android.action.STOP_NETWORK_MONITOR"
        const val CHANNEL_ID = "cyberagent_network"
        const val NOTIFICATION_ID = 2001
    }

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopMonitoring()
            ACTION_START -> startMonitoring()
        }
        return START_NOT_STICKY
    }

    private fun startMonitoring() {
        if (vpnInterface != null) return
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        vpnInterface = Builder()
            .setSession("CyberAgent Network Monitor")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .establish()
    }

    private fun stopMonitoring() {
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "CyberAgent — monitoring sieci", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("CyberAgent monitoruje sieć")
            .setContentText("Monitoring działa za zgodą użytkownika; treść ruchu nie jest analizowana.")
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }
}
