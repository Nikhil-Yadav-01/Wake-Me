package com.nikhil.wakeme.alarms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.nikhil.wakeme.AlarmTriggerActivity
import com.nikhil.wakeme.R
import com.nikhil.wakeme.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "AlarmServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra("ALARM_ID", -1L) ?: -1L
        if (alarmId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_START -> {
                // This check makes the service idempotent.
                // If the sound is already playing, do nothing.
                if (mediaPlayer?.isPlaying == true) {
                    return START_STICKY
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val alarm = AlarmDatabase.getInstance(applicationContext).alarmDao().getById(alarmId)
                    if (alarm != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            startForeground(
                                NOTIFICATION_ID,
                                createNotification(alarmId, alarm.label ?: "Alarm"),
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                            )
                        } else {
                            startForeground(
                                NOTIFICATION_ID,
                                createNotification(alarmId, alarm.label ?: "Alarm")
                            )
                        }
                        startAlarm(alarm.ringtoneUri)
                    } else {
                        stopSelf()
                    }
                }
            }
            ACTION_STOP -> {
                stopAlarm()
                notificationManager.cancel(NOTIFICATION_ID) // Dismiss the notification
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startAlarm(ringtoneUriString: String?) {
        val alarmSoundUri = ringtoneUriString?.toUri() ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer.create(this, alarmSoundUri).apply {
            isLooping = true
            start()
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Correctly handle vibration for different API levels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
    }

    private fun createNotification(alarmId: Long, label: String): Notification {
        createNotificationChannel()
        val triggerIntent = Intent(this, AlarmTriggerActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, triggerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alarm is ringing")
            .setContentText(label)
            .setSmallIcon(R.drawable.ic_wake_pulse)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true) // Make the notification ongoing
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Sound Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        // No need to call stopAlarm() or cancel notification here if ACTION_STOP handles it.
        // However, stopAlarm() is generally a good cleanup in onDestroy as a failsafe.
        stopAlarm()
    }
}
