package com.cyberagent.android

enum class RiskLevel { SAFE, REVIEW, HIGH }

data class RiskEvaluation(val level: RiskLevel, val reasons: List<String>)

/** Conservative heuristic engine: permissions are signals, not proof of malware. */
object RiskEngine {
    private val highSignal = setOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.SYSTEM_ALERT_WINDOW"
    )

    private val reviewSignals = setOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CALL_LOG",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.READ_CONTACTS",
        "android.permission.ACCESS_FINE_LOCATION"
    )

    fun evaluate(packageName: String, permissions: List<String>, isSystemApp: Boolean): RiskEvaluation {
        if (isSystemApp) return RiskEvaluation(RiskLevel.SAFE, listOf("Aplikacja systemowa"))

        val high = permissions.filter { it in highSignal }
        val review = permissions.filter { it in reviewSignals }
        val reasons = buildList {
            if (high.isNotEmpty()) add("Wrażliwe uprawnienia systemowe: ${high.size}")
            if (review.isNotEmpty()) add("Uprawnienia wymagające przeglądu: ${review.size}")
            if (packageName.startsWith("com.google.") || packageName.startsWith("com.android.")) {
                add("Znany schemat pakietu systemowego/Google")
            }
        }

        val level = when {
            high.size >= 2 -> RiskLevel.HIGH
            high.isNotEmpty() || review.size >= 3 -> RiskLevel.REVIEW
            else -> RiskLevel.SAFE
        }
        return RiskEvaluation(level, reasons.ifEmpty { listOf("Brak sygnałów podwyższonego ryzyka") })
    }
}
