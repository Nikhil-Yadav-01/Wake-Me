package com.nikhil.wakeme.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val CHANNEL_ID = "wake_me_alarm_channel"
    const val CHANNEL_NAME = "Wake Me Alarms"

    fun createChannelIfNeeded(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
                ch.description = "Alarm notifications"
                ch.vibrationPattern = longArrayOf(0, 250, 100, 250)
                nm.createNotificationChannel(ch)
            }
        }
    }
}
