package com.cyberagent.android

object CyberCoach {
    fun advice(findings: List<AppScanner.AppFinding>): String {
        val high = findings.count { it.risk == RiskLevel.HIGH }
        val review = findings.count { it.risk == RiskLevel.REVIEW }
        return when {
            high > 0 -> "Wykryto $high aplikacji wymagających pilnego przeglądu. Sprawdź nazwę pakietu, uprawnienia i SHA-256 przed podjęciem decyzji."
            review > 0 -> "Wykryto $review aplikacji do przeglądu. Uprawnienia są sygnałem ryzyka, a nie dowodem złośliwości."
            findings.isNotEmpty() -> "Brak podwyższonych sygnałów w bieżącej heurystyce. Regularnie aktualizuj system i aplikacje."
            else -> "Uruchom skanowanie aplikacji, aby CyberAgent mógł przygotować ocenę."
        }
    }
}
