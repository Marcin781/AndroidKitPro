package com.cyberagent.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** Read-only inventory scanner. It does not modify or uninstall applications. */
class AppScanner(private val context: Context) {
    data class AppFinding(
        val packageName: String,
        val label: String,
        val versionName: String?,
        val permissions: List<String>,
        val apkSha256: String?,
        val risk: RiskLevel,
        val reasons: List<String>
    )

    fun scan(): List<AppFinding> {
        val pm = context.packageManager
        val packages = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        }

        return packages.map { info -> scanPackage(pm, info) }
            .sortedWith(compareByDescending<AppFinding> { it.risk.ordinal }.thenBy { it.label.lowercase() })
    }

    private fun scanPackage(pm: PackageManager, info: PackageInfo): AppFinding {
        val appInfo = info.applicationInfo
        val label = appInfo?.loadLabel(pm)?.toString() ?: info.packageName
        val permissions = info.requestedPermissions?.toList().orEmpty()
        val evaluation = RiskEngine.evaluate(
            packageName = info.packageName,
            permissions = permissions,
            isSystemApp = appInfo?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } == true
        )
        val hash = appInfo?.sourceDir?.let { sha256File(it) }
        return AppFinding(
            packageName = info.packageName,
            label = label,
            versionName = info.versionName,
            permissions = permissions,
            apkSha256 = hash,
            risk = evaluation.level,
            reasons = evaluation.reasons
        )
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
    } catch (_: Exception) {
        null
    }
}
