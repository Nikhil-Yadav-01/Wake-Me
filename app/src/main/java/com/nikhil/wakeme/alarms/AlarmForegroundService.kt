package com.nikhil.wakeme.alarms

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.nikhil.wakeme.AlarmFullScreenActivity
import com.nikhil.wakeme.data.AlarmDatabase
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlarmForegroundService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmId: Long = -1L

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob) // Or Dispatchers.Default if preferred

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId == -1L) {
            stopSelf()
            return START_NOT_STICKY
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = AlarmDatabase.getInstance(applicationContext)
            val alarm = db.alarmDao().getById(alarmId)
            if (alarm == null || !alarm.enabled) {
                stopSelf()
                return@launch
            }

            acquireWakeLock()
            val notification = buildNotification(alarm)
            startForeground(12345, notification)
            playAlarm(alarm)
            startAutoSnoozeWatcher(alarm)
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeMe:AlarmWakeLock")
        wakeLock?.acquire(10 * 60 * 1000L)
    }

    private fun buildNotification(alarm: AlarmEntity): android.app.Notification {
        NotificationHelper.createChannelIfNeeded(this)
        val stopIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_STOP
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val stopPI = PendingIntent.getBroadcast(
            this,
            (alarmId + 1).toInt(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or getImmutableFlag()
        )

        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val snoozePI = PendingIntent.getBroadcast(
            this,
            (alarmId + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or getImmutableFlag()
        )

        val fullIntent = Intent(this, AlarmFullScreenActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullPI = PendingIntent.getActivity(
            this,
            (alarmId + 3).toInt(),
            fullIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or getImmutableFlag()
        )

        val nb = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle(alarm.label ?: "Alarm")
            .setContentText("Alarm ringing - solve challenge to stop")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM).setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Snooze", snoozePI)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPI)
            .setFullScreenIntent(fullPI, true)

        return nb.build()
    }

    private fun playAlarm(alarm: AlarmEntity) {
        try {
            mediaPlayer?.release()
            val mp = MediaPlayer()
            mp.setAudioStreamType(AudioManager.STREAM_ALARM)
            if (!alarm.toneUri.isNullOrEmpty()) {
                mp.setDataSource(this, alarm.toneUri!!.toUri())
                mp.prepare()
            } else {
                val uri = android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                mp.setDataSource(this, uri)
                mp.prepare()
            }
            mp.isLooping = true
            mp.setVolume(0.1f, 0.1f)
            mp.start()
            mediaPlayer = mp

            Thread {
                var vol = 0.1f
                while (mediaPlayer != null && mediaPlayer!!.isPlaying && vol < 1f) {
                    try {
                        Thread.sleep(1500)
                    } catch (e: InterruptedException) {
                        break
                    }
                    vol += 0.1f
                    try {
                        mediaPlayer?.setVolume(vol, vol)
                    } catch (_: Exception) {
                    }
                }
            }.start()

            if (alarm.vibrate) {
                val v = getSystemService(VIBRATOR_SERVICE) as Vibrator?
                v?.vibrate(longArrayOf(0, 500, 200, 500), 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "play error", e)
        }
    }

    private fun startAutoSnoozeWatcher(alarm: AlarmEntity) {
        val WAIT_SECONDS = 60
        serviceScope.launch {
            try {
                delay(WAIT_SECONDS * 1000L)

                // Check if the coroutine is still active (service might have been stopped)
                if (!isActive) {
                    return@launch
                }
                val currentMediaPlayer = mediaPlayer
                if (currentMediaPlayer != null && currentMediaPlayer.isPlaying) {
                    if (alarm.autoSnoozeEnabled) {
                        // Switch to IO dispatcher for database operations
                        launch(Dispatchers.IO) {
                            try {
                                val db = AlarmDatabase.getInstance(applicationContext)
                                val alarmEntity = db.alarmDao().getById(alarmId) ?: return@launch

                                val maxCycles = alarmEntity.autoSnoozeMaxCycles
                                if (maxCycles > 0 && alarmEntity.autoSnoozeCount >= maxCycles) {
                                    // Reached limit: mark missed (disable) and stop
                                    alarmEntity.enabled = false
                                    db.alarmDao().update(alarmEntity)
                                    stopSelfSafely() // Call a potentially safer stopSelf
                                    return@launch
                                }
                                alarmEntity.autoSnoozeCount += 1
                                val snoozeMillis = alarmEntity.snoozeMinutes * 60L * 1000L
                                alarmEntity.timeMillis = System.currentTimeMillis() + snoozeMillis
                                db.alarmDao().update(alarmEntity)
                                AlarmScheduler.scheduleAlarm(applicationContext, alarmEntity)

                                // Cleanup
                                stopSelfSafely() // Call a potentially safer stopSelf
                            } catch (e: Exception) {
                                Log.e("AlarmService", "Error in auto-snooze DB operation", e)
                                // Optionally, try to stop the service or handle the error appropriately
                                stopSelfSafely()
                            }
                        }
                    } else {
                        // Auto snooze is not enabled, but alarm is still playing after WAIT_SECONDS.
                        // You might want to do nothing, or stop the alarm, or stop the service.
                        // For example, if you want to stop the service if auto-snooze is disabled and time is up:
                        // stopSelfSafely()
                    }
                } else {
                    // Media player is not playing (already stopped or never started)
                    // You might want to stop the service if it's not needed anymore.
                    // stopSelfSafely()
                }
            } catch (e: InterruptedException) {
                // Coroutine was cancelled (e.g., serviceJob.cancel())
                Thread.currentThread().interrupt() // Restore interruption status
                Log.i("AlarmService", "AutoSnoozeWatcher was interrupted/cancelled.")
            }
        }
    }

    private fun stopSelfSafely() {
        // Add any checks or logging if needed before calling stopSelf()
        Log.d("AlarmService", "Attempting to stop service via stopSelfSafely for alarmId: $alarmId")
        stopSelf()
    }

    override fun onDestroy() {
        try {
            mediaPlayer?.stop(); mediaPlayer?.release()
        } catch (_: Exception) {
        }
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }

    private fun getImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }
}
