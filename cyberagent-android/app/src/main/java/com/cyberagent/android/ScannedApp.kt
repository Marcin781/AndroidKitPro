package com.cyberagent.android

data class ScannedApp(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val permissions: List<String>,
    val sha256: String?,
    val risk: RiskLevel,
    val reasons: List<String>
)
