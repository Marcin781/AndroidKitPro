package com.cyberagent.android

import android.content.Context

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
            .putBoolean("running", true).putString("transport", transport)
            .putBoolean("validated", validated).putBoolean("metered", metered)
            .putBoolean("vpn_present", vpnPresent).putInt("active_networks", activeNetworks)
            .putLong("updated_at", System.currentTimeMillis()).apply()
    }
    fun setStopped(context: Context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("running", false).apply() }
    fun isRunning(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("running", false)
    fun load(context: Context): Snapshot? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("updated_at")) return null
        return Snapshot(
            p.getString("transport", "UNKNOWN") ?: "UNKNOWN",
            p.getBoolean("validated", false), p.getBoolean("metered", false),
            p.getBoolean("vpn_present", false), p.getInt("active_networks", 0),
            p.getLong("updated_at", 0L)
        )
    }
}
