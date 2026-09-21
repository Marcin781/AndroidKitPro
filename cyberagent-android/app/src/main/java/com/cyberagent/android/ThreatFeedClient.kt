package com.cyberagent.android

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Read-only client for the operator-controlled CyberAgent threat feed. */
object ThreatFeedClient {
    private const val FEED_URL = "https://cyberagent-api.onrender.com/api/v1/threat-feed"

    data class ThreatFeed(
        val version: Int,
        val indicators: Set<String>
    ) {
        val indicatorCount: Int get() = indicators.size
    }

    fun fetch(): ThreatFeed? = runCatching {
        val connection = (URL(FEED_URL).openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val indicators = buildSet {
                val array = json.optJSONArray("indicators") ?: return@buildSet
                for (i in 0 until array.length()) {
                    val value = array.optString(i).trim().lowercase()
                    if (value.isNotEmpty()) add(value)
                }
            }
            ThreatFeed(json.optInt("version", 1), indicators)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    fun matchesSha256(feed: ThreatFeed?, sha256: String?): Boolean {
        if (feed == null || sha256.isNullOrBlank()) return false
        return sha256.lowercase() in feed.indicators
    }
}
