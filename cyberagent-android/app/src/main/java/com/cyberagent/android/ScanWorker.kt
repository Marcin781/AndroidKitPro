package com.cyberagent.android

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Periodic, read-only device application inventory scan. */
class ScanWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = try {
        val findings = AppScanner(applicationContext).scan()
        val high = findings.count { it.risk == RiskLevel.HIGH }
        val review = findings.count { it.risk == RiskLevel.REVIEW }
        applicationContext.getSharedPreferences("cyberagent", Context.MODE_PRIVATE)
            .edit()
            .putInt("last_scan_apps", findings.size)
            .putInt("last_scan_high", high)
            .putInt("last_scan_review", review)
            .putLong("last_scan_at", System.currentTimeMillis())
            .apply()
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
