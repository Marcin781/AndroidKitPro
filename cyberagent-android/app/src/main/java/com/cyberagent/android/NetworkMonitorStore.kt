package com.cyberagent.android

import android.content.Context

/** Local-only network telemetry. No packet payloads, domains, URLs, SSIDs or identifiers are stored. */
object NetworkMonitorStore {
    private const val PREFS = "cyberagent_network"

    data class Snapshot(
        val transport: String,
        val validated: Boolean,
        val metered: Boolean,
        val vpnPresent: Boolean,
        val activeNetworks: Int,
        val updatedAt: Long
    )

    fun save(context: Context, transport: String, validated: Boolean, metered: Boolean, vpnPresent: Boolean, activeNetworks: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("running", true)
            .putString("transport", transport)
            .putBoolean("validated", validated)
            .putBoolean("metered", metered)
            .putBoolean("vpn_present", vpnPresent)
            .putInt("active_networks", activeNetworks)
            .putLong("updated_at", System.currentTimeMillis())
            .apply()
    }

    fun setStopped(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("running", false).apply()
    }

    fun isRunning(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("running", false)

    fun load(context: Context): Snapshot? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains("updated_at")) return null
        return Snapshot(
            prefs.getString("transport", "UNKNOWN") ?: "UNKNOWN",
            prefs.getBoolean("validated", false),
            prefs.getBoolean("metered", false),
            prefs.getBoolean("vpn_present", false),
            prefs.getInt("active_networks", 0),
            prefs.getLong("updated_at", 0L)
        )
    }
}
