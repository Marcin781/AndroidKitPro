package com.cyberagent.android

/** Local network event record. Payload/content is intentionally absent. */
data class NetworkEvent(
    val timestamp: Long,
    val transport: String,
    val destinationIp: String? = null,
    val destinationDomain: String? = null,
    val destinationUrl: String? = null,
    val bytes: Long = 0L
) {
    fun correlate(feed: ThreatFeedClient.ThreatFeed?): NetworkIocMatcher.Match? =
        NetworkIocMatcher.match(
            feed = feed,
            ip = destinationIp,
            domain = destinationDomain,
            url = destinationUrl
        )
}
