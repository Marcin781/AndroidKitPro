package com.cyberagent.android

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Read-only client for the operator-controlled CyberAgent threat feed. */
object ThreatFeedClient {
    private const val FEED_URL = "https://cyberagent-api.onrender.com/api/v1/threat-feed"

    data class ThreatFeed(val version: Int, val indicators: Set<String>)

    fun fetch(): ThreatFeed? = runCatching {
        val connection = URL(FEED_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            if (connection.responseCode !in 200..299) return null

            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val array = json.optJSONArray("indicators")
            val indicators = buildSet {
                if (array != null) {
                    for (i in 0 until array.length()) {
                        val value = array.optString(i).trim().lowercase()
                        if (value.isNotEmpty()) add(value)
                    }
                }
            }
            ThreatFeed(json.optInt("version", 1), indicators)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    fun matchesSha256(feed: ThreatFeed?, sha256: String?): Boolean =
        !sha256.isNullOrBlank() && feed?.indicators?.contains(sha256.lowercase()) == true
}
