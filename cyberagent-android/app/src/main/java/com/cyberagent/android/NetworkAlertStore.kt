package com.cyberagent.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Bounded local-only store for confirmed IOC matches. */
object NetworkAlertStore {
    private const val FILE = "network-alerts.json"
    private const val MAX_ALERTS = 50

    data class Alert(
        val timestamp: Long,
        val type: String,
        val indicator: String,
        val reason: String
    )

    fun record(context: Context, match: NetworkIocMatcher.Match) {
        runCatching {
            val file = File(context.filesDir, FILE)
            val array = if (file.exists()) runCatching { JSONArray(file.readText()) }.getOrElse { JSONArray() } else JSONArray()
            array.put(JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("type", match.indicator.type.name)
                put("indicator", match.indicator.value)
                put("reason", match.reason)
            })
            while (array.length() > MAX_ALERTS) array.remove(0)
            file.writeText(array.toString())
        }
    }

    fun load(context: Context): List<Alert> = runCatching {
        val array = File(context.filesDir, FILE).let { if (it.exists()) JSONArray(it.readText()) else JSONArray() }
        buildList {
            for (i in array.length() - 1 downTo 0) {
                val item = array.getJSONObject(i)
                add(
                    Alert(
                        item.optLong("timestamp"),
                        item.optString("type"),
                        item.optString("indicator"),
                        item.optString("reason")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}
