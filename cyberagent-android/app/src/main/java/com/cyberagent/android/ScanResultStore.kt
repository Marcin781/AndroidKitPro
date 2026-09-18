package com.cyberagent.android

import android.content.Context

/** Small local event store for the latest scan summary. */
object ScanResultStore {
    private const val PREFS = "cyberagent"
    data class Summary(val apps: Int, val high: Int, val review: Int, val safe: Int, val scannedAt: Long)

    fun save(context: Context, findings: List<AppScanner.AppFinding>) {
        val high = findings.count { it.risk == RiskLevel.HIGH }
        val review = findings.count { it.risk == RiskLevel.REVIEW }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("last_scan_apps", findings.size).putInt("last_scan_high", high).putInt("last_scan_review", review)
            .putLong("last_scan_at", System.currentTimeMillis()).apply()
    }

    fun load(context: Context): Summary? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains("last_scan_apps")) return null
        val apps = prefs.getInt("last_scan_apps", 0)
        val high = prefs.getInt("last_scan_high", 0)
        val review = prefs.getInt("last_scan_review", 0)
        return Summary(apps, high, review, (apps - high - review).coerceAtLeast(0), prefs.getLong("last_scan_at", 0L))
    }
}