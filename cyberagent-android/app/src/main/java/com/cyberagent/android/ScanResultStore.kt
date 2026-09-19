package com.cyberagent.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Local-only bounded scan history. No findings leave the device. */
object ScanResultStore {
    private const val PREFS = "cyberagent"
    private const val HISTORY_FILE = "scan-history.json"
    private const val MAX_HISTORY = 50

    data class Summary(
        val apps: Int,
        val high: Int,
        val review: Int,
        val safe: Int,
        val scannedAt: Long
    )

    fun save(context: Context, findings: List<AppScanner.AppFinding>) {
        val now = System.currentTimeMillis()
        val high = findings.count { it.risk == RiskLevel.HIGH }
        val review = findings.count { it.risk == RiskLevel.REVIEW }
        val safe = (findings.size - high - review).coerceAtLeast(0)

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("last_scan_apps", findings.size)
            .putInt("last_scan_high", high)
            .putInt("last_scan_review", review)
            .putLong("last_scan_at", now)
            .apply()

        runCatching {
            val history = loadHistoryJson(context)
            history.put(JSONObject().apply {
                put("scannedAt", now)
                put("apps", findings.size)
                put("high", high)
                put("review", review)
                put("safe", safe)
            })
            while (history.length() > MAX_HISTORY) history.remove(0)
            File(context.filesDir, HISTORY_FILE).writeText(history.toString())
        }
    }

    fun load(context: Context): Summary? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains("last_scan_apps")) return null
        val apps = prefs.getInt("last_scan_apps", 0)
        val high = prefs.getInt("last_scan_high", 0)
        val review = prefs.getInt("last_scan_review", 0)
        return Summary(
            apps, high, review,
            (apps - high - review).coerceAtLeast(0),
            prefs.getLong("last_scan_at", 0L)
        )
    }

    fun loadHistory(context: Context): List<Summary> = runCatching {
        val array = loadHistoryJson(context)
        buildList {
            for (i in array.length() - 1 downTo 0) {
                val item = array.getJSONObject(i)
                add(
                    Summary(
                        item.optInt("apps"),
                        item.optInt("high"),
                        item.optInt("review"),
                        item.optInt("safe"),
                        item.optLong("scannedAt")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun loadHistoryJson(context: Context): JSONArray {
        val file = File(context.filesDir, HISTORY_FILE)
        if (!file.exists()) return JSONArray()
        return runCatching { JSONArray(file.readText()) }.getOrElse { JSONArray() }
    }
}
