package com.cyberagent.android

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat

/** Handles the explicit Android VPN consent flow. */
object NetworkMonitorController {
    fun prepare(context: Context): Intent? = VpnService.prepare(context)

    fun start(context: Context) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, NetworkMonitorService::class.java).setAction(NetworkMonitorService.ACTION_START)
        )
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, NetworkMonitorService::class.java).setAction(NetworkMonitorService.ACTION_STOP)
        )
    }
}
