package com.nikhil.wakeme.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nikhil.wakeme.AlarmTriggerActivity
import com.nikhil.wakeme.R
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.data.AlarmEntity

object NotificationHelper {
    private const val UPCOMING_CHANNEL_ID = "upcoming_alarm_channel"
    private const val UPCOMING_CHANNEL_NAME = "Upcoming Alarms"

    private const val FULLSCREEN_CHANNEL_ID = "wake_me_alarm_channel"
    private const val FULLSCREEN_CHANNEL_NAME = "Wake Me Alarms"
    private const val FULL_SCREEN_REQUEST_CODE = 1001

    fun showUpcomingAlarmNotification(context: Context, alarm: Alarm) {
        Log.d("NotificationHelper", "showUpcomingAlarmNotification: ${alarm.id}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                UPCOMING_CHANNEL_ID,
                UPCOMING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, UPCOMING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wake_pulse)
            .setContentTitle("Upcoming Alarm")
            .setContentText("Alarm in 5 minutes")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(alarm.id.toInt() + 10000, builder.build()) // separate ID
    }

    fun showAlarmNotification(context: Context, alarm: Alarm) {
        Log.d("NotificationHelper", "showAlarmNotification: ${alarm.id}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FULLSCREEN_CHANNEL_ID,
                FULLSCREEN_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null) // sound is handled by AlarmService
                enableVibration(false)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(context, AlarmTriggerActivity::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            FULL_SCREEN_REQUEST_CODE,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, FULLSCREEN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wake_pulse)
            .setContentTitle(alarm.label ?: "Alarm")
            .setContentText("Time to wake up!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(alarm.id.toInt(), builder.build())
    }

    fun cancelNotification(context: Context, alarmId: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(alarmId)
        nm.cancel(alarmId + 10000) // also cancel upcoming notification if exists
    }
}
