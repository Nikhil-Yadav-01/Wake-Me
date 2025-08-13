package com.nikhil.wakeme.alarms

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nikhil.wakeme.MainActivity
import com.nikhil.wakeme.R
import com.nikhil.wakeme.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val appContext = context.applicationContext ?: return

            CoroutineScope(Dispatchers.IO).launch {
                if (!canScheduleExactAlarms(appContext)) {
                    Log.w("BootReceiver", "Cannot reschedule alarms on boot: SCHEDULE_EXACT_ALARM permission not granted.")
                    sendMissingPermissionNotification(appContext)
                    return@launch
                }

                val db = AlarmDatabase.getInstance(appContext)
                val alarms = db.alarmDao().getEnabledAlarmsList()
                Log.i("BootReceiver", "Rescheduling ${alarms.size} alarms.")
                for (a in alarms) {
                    val nextTrigger = a.calculateNextTrigger()
                    if (a.timeMillis != nextTrigger) {
                        a.timeMillis = nextTrigger
                        db.alarmDao().update(a)
                    }
                    // This is safe because we check canScheduleExactAlarms above.
                    AlarmScheduler.scheduleAlarm(appContext, a)
                }
            }
        }
    }

    private fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    private fun sendMissingPermissionNotification(context: Context) {
        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)

        val channelId = "alarm_permission_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Permissions",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager?.createNotificationChannel(channel)
        }

        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reason", "request_exact_alarm_permission")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Alarm Reschedule Failed")
            .setContentText("Please grant permission to schedule exact alarms so WakeMe can reliably set your alarms.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager?.notify(PERMISSION_NOTIFICATION_ID, builder.build())
        Log.i("BootReceiver", "Sent notification for missing exact alarm permission.")
    }

    companion object {
        private const val PERMISSION_NOTIFICATION_ID = 1001
    }
}
