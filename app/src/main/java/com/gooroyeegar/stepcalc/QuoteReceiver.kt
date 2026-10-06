package com.gooroyeegar.stepcalc

import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat

class QuoteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val quotes = context.resources.getStringArray(R.array.daily_quotes)
        if (quotes.isEmpty()) return
        val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val msg = quotes[(day - 1) % quotes.size]
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                "daily",
                context.getString(R.string.notification_channel_daily),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        nm.notify(
            9000,
            NotificationCompat.Builder(context, "daily")
                .setSmallIcon(R.drawable.ic_stat_steps)
                .setContentTitle(context.getString(R.string.daily_notification_title))
                .setContentText(msg)
                .setAutoCancel(true)
                .build()
        )
    }
}
