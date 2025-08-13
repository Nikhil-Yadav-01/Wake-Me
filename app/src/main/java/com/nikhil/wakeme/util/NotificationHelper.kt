package com.nikhil.wakeme.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nikhil.wakeme.AlarmFullScreenActivity
import com.nikhil.wakeme.R
import com.nikhil.wakeme.data.AlarmEntity

object NotificationHelper {
    const val CHANNEL_ID = "wake_me_alarm_channel"
    const val CHANNEL_NAME = "Wake Me Alarms"
    private const val FULL_SCREEN_ALARM_REQUEST_CODE = 1001

    fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for alarm notifications"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 500, 500)
                    setSound(null, null) 
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun showAlarmNotification(context: Context, alarm: AlarmEntity) {
        createChannelIfNeeded(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val fullScreenIntent = Intent(context, AlarmFullScreenActivity::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            FULL_SCREEN_ALARM_REQUEST_CODE,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(alarm.label ?: "Alarm")
            .setContentText("It's time to wake up!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .build()

        notificationManager.notify(alarm.id.toInt(), notification)
    }
}
