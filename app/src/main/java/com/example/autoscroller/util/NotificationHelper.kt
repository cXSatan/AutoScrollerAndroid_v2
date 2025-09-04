package com.example.autoscroller.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.autoscroller.R
import com.example.autoscroller.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "autoscroller_status"
    private const val CHANNEL_NAME = "AutoScroller"
    private const val CHANNEL_DESC = "AutoScroller foreground status"

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = CHANNEL_DESC
                    setShowBadge(false)
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun buildStatusNotification(ctx: Context, running: Boolean): Notification {
        val openIntent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            ctx, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = if (running) "AutoScroller running" else "AutoScroller idle"
        val text = if (running) "Tap to open controls" else "Tap to open app"

        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}