package com.cyberagent.android

object NetworkIocMatcher {
    enum class IndicatorType { IP, DOMAIN, URL, SHA256 }
    data class NetworkIndicator(val type: IndicatorType, val value: String)
    data class Match(val indicator: NetworkIndicator, val reason: String)

    fun match(feed: ThreatFeedClient.ThreatFeed?, ip: String? = null, domain: String? = null, url: String? = null, sha256: String? = null): Match? {
        if (feed == null) return null
        for ((type, value) in listOf(
            IndicatorType.IP to ip, IndicatorType.DOMAIN to domain,
            IndicatorType.URL to url, IndicatorType.SHA256 to sha256
        )) {
            val normalized = value?.trim()?.lowercase() ?: continue
            if (normalized.isNotEmpty() && normalized in feed.indicators) {
                return Match(NetworkIndicator(type, normalized), "Dokładne dopasowanie ${type.name} do IOC z feedu v${feed.version}")
            }
        }
        return null
    }
}
