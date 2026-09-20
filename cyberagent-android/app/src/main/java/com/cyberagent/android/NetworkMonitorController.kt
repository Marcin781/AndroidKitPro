package com.cyberagent.android

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Starts/stops the read-only Android connectivity telemetry service. */
object NetworkMonitorController {
    fun start(context: Context) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, NetworkMonitorService::class.java)
                .setAction(NetworkMonitorService.ACTION_START)
        )
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, NetworkMonitorService::class.java)
                .setAction(NetworkMonitorService.ACTION_STOP)
        )
    }
}
