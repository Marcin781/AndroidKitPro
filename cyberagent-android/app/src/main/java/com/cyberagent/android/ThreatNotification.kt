package com.cyberagent.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** User-visible notification for high-risk scan findings. */
object ThreatNotification {
    private const val CHANNEL_ID = "cyberagent_threats"
    private const val CHANNEL_NAME = "CyberAgent — zagrożenia"
    private const val NOTIFICATION_ID = 1001

    fun showNetworkIoc(context: Context, match: NetworkIocMatcher.Match) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("CyberAgent: IOC dopasowany")
            .setContentText("${match.indicator.type}: ${match.indicator.value}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(match.reason))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1, notification)
    }

    fun showHighRisk(context: Context, count: Int) {
        if (count <= 0) return
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("CyberAgent: wymagany przegląd")
            .setContentText("Wykryto $count aplikacji oznaczonych jako HIGH.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "CyberAgent wykrył $count aplikacji z wysokim sygnałem ryzyka. Sprawdź szczegóły w aplikacji; nie wykonuj nieodwracalnych działań bez weryfikacji."
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}