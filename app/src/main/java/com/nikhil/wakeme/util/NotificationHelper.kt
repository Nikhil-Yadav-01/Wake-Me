package com.nikhil.wakeme.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nikhil.wakeme.AlarmTriggerActivity
import com.nikhil.wakeme.R
import com.nikhil.wakeme.data.AlarmEntity

object NotificationHelper {
    private const val CHANNEL_ID = "wake_me_alarm_channel"
    private const val CHANNEL_NAME = "Wake Me Alarms"
    private const val FULL_SCREEN_ALARM_REQUEST_CODE = 1001

    // This function creates the channel responsible for *displaying* the alarm UI.
    // It is intentionally silent because the sound will be handled by a foreground service.
    fun createSilentChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for displaying the alarm screen"
                // No sound or vibration here.
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showAlarmNotification(context: Context, alarm: AlarmEntity) {
        // Ensure the silent channel exists.
        createSilentChannel(context)

        // This intent will launch the full-screen alarm activity.
        val fullScreenIntent = Intent(context, AlarmTriggerActivity::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            FULL_SCREEN_ALARM_REQUEST_CODE,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification that will trigger the full-screen intent.
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_wake_pulse)
            .setContentTitle(alarm.label ?: "Alarm")
            .setContentText("Time to wake up!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true) // This is the key part.
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alarm.id.toInt(), notificationBuilder.build())
    }

    fun cancelNotification(context: Context, alarmId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId)
    }
}
