package com.nikhil.wakeme.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
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
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Channel for alarm notifications"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 500, 500)
                    setSound(alarmSound, audioAttributes)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun showAlarmNotification(context: Context, alarm: AlarmEntity) {
        createChannelIfNeeded(context)

        val fullScreenIntent = Intent(context, AlarmFullScreenActivity::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            FULL_SCREEN_ALARM_REQUEST_CODE,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use an appropriate icon
            .setContentTitle(alarm.label ?: "Alarm")
            .setContentText("Time to wake up!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent) 
            .setOngoing(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alarm.id.toInt(), notificationBuilder.build())
    }

    fun cancelNotification(context: Context, alarmId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId)
    }
}
