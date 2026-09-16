package com.cyberagent.android

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Periodic, read-only device application inventory scan. */
class ScanWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = try {
        val findings = AppScanner(applicationContext).scan()
        ScanResultStore.save(applicationContext, findings)
        ThreatFeedClient.fetch()
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }
}
