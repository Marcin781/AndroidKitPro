package com.cyberagent.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** Read-only inventory scanner with optional SHA-256 threat-feed matching. */
class AppScanner(private val context: Context) {
    data class AppFinding(
        val packageName: String, val label: String, val versionName: String?,
        val permissions: List<String>, val apkSha256: String?, val risk: RiskLevel, val reasons: List<String>
    )

    fun scan(): List<AppFinding> {
        val pm = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }
        val feed = ThreatFeedClient.fetch()
        return packages.map { scanPackage(pm, it, feed) }
            .sortedWith(compareByDescending<AppFinding> { it.risk.ordinal }.thenBy { it.label.lowercase() })
    }

    private fun scanPackage(pm: PackageManager, info: PackageInfo, feed: ThreatFeedClient.ThreatFeed?): AppFinding {
        val appInfo = info.applicationInfo
        val label = appInfo?.loadLabel(pm)?.toString() ?: info.packageName
        val permissions = info.requestedPermissions?.toList().orEmpty()
        val hash = appInfo?.sourceDir?.let { sha256File(it) }
        val evaluation = RiskEngine.evaluate(
            packageName = info.packageName,
            permissions = permissions,
            isSystemApp = appInfo?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } == true
        )
        val feedMatch = ThreatFeedClient.matchesSha256(feed, hash)
        val risk = if (feedMatch) RiskLevel.HIGH else evaluation.level
        val reasons = buildList {
            addAll(evaluation.reasons)
            if (feedMatch) add("SHA-256 matches CyberAgent threat feed (v${feed?.version ?: 1})")
        }
        return AppFinding(info.packageName, label, info.versionName, permissions, hash, risk, reasons)
    }

    private fun sha256File(path: String): String? = try {
        val digest = MessageDigest.getInstance("SHA-256")
        java.io.File(path).inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) { null }
}
