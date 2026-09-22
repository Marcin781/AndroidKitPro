package com.cyberagent.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Bounded local-only store for confirmed IOC matches. */
object NetworkAlertStore {
    private const val FILE = "network-alerts.json"
    private const val MAX_ALERTS = 50
    private const val DEDUP_WINDOW_MS = 10 * 60 * 1000L

    data class Alert(val timestamp: Long, val type: String, val indicator: String, val reason: String)

    fun record(context: Context, match: NetworkIocMatcher.Match) {
        recordIfNew(context, match, System.currentTimeMillis())
    }

    fun recordIfNew(context: Context, match: NetworkIocMatcher.Match, now: Long): Boolean = runCatching {
        val file = File(context.filesDir, FILE)
        val array = if (file.exists()) runCatching { JSONArray(file.readText()) }.getOrElse { JSONArray() } else JSONArray()
        for (i in array.length() - 1 downTo 0) {
            val item = array.getJSONObject(i)
            if (item.optString("type") == match.indicator.type.name &&
                item.optString("indicator") == match.indicator.value &&
                now - item.optLong("timestamp") < DEDUP_WINDOW_MS
            ) return@runCatching false
        }
        array.put(JSONObject().apply {
            put("timestamp", now)
            put("type", match.indicator.type.name)
            put("indicator", match.indicator.value)
            put("reason", match.reason)
        })
        while (array.length() > MAX_ALERTS) array.remove(0)
        file.writeText(array.toString())
        true
    }.getOrDefault(false)

    fun load(context: Context): List<Alert> = runCatching {
        val array = File(context.filesDir, FILE).let { if (it.exists()) JSONArray(it.readText()) else JSONArray() }
        buildList {
            for (i in array.length() - 1 downTo 0) {
                val item = array.getJSONObject(i)
                add(Alert(item.optLong("timestamp"), item.optString("type"), item.optString("indicator"), item.optString("reason")))
            }
        }
    }.getOrDefault(emptyList())
}
