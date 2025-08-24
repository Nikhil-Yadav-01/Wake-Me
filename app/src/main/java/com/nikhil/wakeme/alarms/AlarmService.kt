package com.nikhil.wakeme.alarms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        private const val SERVICE_NOTIFICATION_ID = 100 // for foreground service
        private const val SERVICE_CHANNEL_ID = "alarm_service_channel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val repo = AlarmRepository(applicationContext)
        val alarmId = intent?.getLongExtra("ALARM_ID", -1L) ?: -1L

        if (alarmId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_START -> {
                // Start foreground *immediately* (required for Android 12+)
                val silentNotification = createServiceNotification("Alarm is starting…")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        SERVICE_NOTIFICATION_ID,
                        silentNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(SERVICE_NOTIFICATION_ID, silentNotification)
                }

                if (mediaPlayer?.isPlaying == true) return START_STICKY
                CoroutineScope(Dispatchers.IO).launch {
                    val alarm = repo.getById(alarmId)

                    alarm?.let {
                        startAlarm(alarm.ringtoneUri?.toString(), alarm.vibration)

                        val intent = Intent(this@AlarmService, AlarmTriggerActivity::class.java).apply {
                            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("ALARM_ID", alarmId)
                        }
                        startActivity(intent)
                        NotificationHelper.showAlarmNotification(applicationContext, alarm)
                    }
                }
            }
            ACTION_STOP -> {
                stopAlarm()
                NotificationHelper.cancelNotification(applicationContext, alarmId)

                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startAlarm(ringtoneUriString: String?, vibration: Boolean) {
        val alarmSoundUri =
            ringtoneUriString?.toUri() ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        mediaPlayer = MediaPlayer.create(this, alarmSoundUri).apply {
            isLooping = true
            start()
        }

        if (vibration) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 1000, 1000), 0)
            }
        }
    }

    private fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
    }

    private fun createServiceNotification(text: String): Notification {
        val channelName = "Alarm Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("Wake Me Alarm")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_wake_pulse)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
