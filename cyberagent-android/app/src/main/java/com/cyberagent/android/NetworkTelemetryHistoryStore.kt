package com.cyberagent.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Bounded local history of network-state changes. No packet or payload data is stored. */
object NetworkTelemetryHistoryStore {
    private const val FILE = "network-telemetry-history.json"
    private const val MAX_EVENTS = 100

    data class Event(
        val timestamp: Long,
        val transport: String,
        val validated: Boolean,
        val metered: Boolean,
        val vpnPresent: Boolean,
        val activeNetworks: Int
    )

    fun record(context: Context, snapshot: Event) {
        runCatching {
            val file = File(context.filesDir, FILE)
            val array = if (file.exists()) runCatching { JSONArray(file.readText()) }.getOrElse { JSONArray() } else JSONArray()
            val last = array.optJSONObject(array.length() - 1)
            val unchanged = last != null &&
                last.optString("transport") == snapshot.transport &&
                last.optBoolean("validated") == snapshot.validated &&
                last.optBoolean("metered") == snapshot.metered &&
                last.optBoolean("vpnPresent") == snapshot.vpnPresent &&
                last.optInt("activeNetworks") == snapshot.activeNetworks
            if (unchanged) return
            array.put(JSONObject().apply {
                put("timestamp", snapshot.timestamp)
                put("transport", snapshot.transport)
                put("validated", snapshot.validated)
                put("metered", snapshot.metered)
                put("vpnPresent", snapshot.vpnPresent)
                put("activeNetworks", snapshot.activeNetworks)
            })
            while (array.length() > MAX_EVENTS) array.remove(0)
            file.writeText(array.toString())
        }
    }

    fun load(context: Context): List<Event> = runCatching {
        val file = File(context.filesDir, FILE)
        if (!file.exists()) return@runCatching emptyList()
        val array = JSONArray(file.readText())
        buildList {
            for (i in array.length() - 1 downTo 0) {
                val item = array.getJSONObject(i)
                add(Event(
                    item.optLong("timestamp"),
                    item.optString("transport", "UNKNOWN"),
                    item.optBoolean("validated", false),
                    item.optBoolean("metered", false),
                    item.optBoolean("vpnPresent", false),
                    item.optInt("activeNetworks", 0)
                ))
            }
        }
    }.getOrDefault(emptyList())
}
