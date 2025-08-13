package com.nikhil.wakeme.alarms

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
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // It's good practice to ensure context isn't null, though it rarely is here.
            val appContext = context.applicationContext ?: return

            CoroutineScope(Dispatchers.IO).launch {
                if (!canScheduleExactAlarms(appContext)) {
                    Log.w("BootReceiver", "Cannot reschedule alarms on boot: SCHEDULE_EXACT_ALARM permission not granted.")
                    // Option: Send a notification to the user informing them
                    // that alarms could not be rescheduled and they need to open the app
                    // to grant permissions.
                    sendMissingPermissionNotification(appContext)
                    return@launch
                }

                val db = AlarmDatabase.getInstance(appContext)
                val alarms = db.alarmDao().getEnabledAlarmsList()
                Log.i("BootReceiver", "Rescheduling ${alarms.size} alarms.")
                for (a in alarms) {
                    if (a.timeMillis < System.currentTimeMillis()) {
                        // If the alarm time is in the past (e.g., device was off for a while)
                        //  - Option 1: Reschedule for the next day (current logic)
                        //  - Option 2: Reschedule for a short time in the future (e.g., 1 minute)
                        //  - Option 3: Mark as missed (if appropriate for your app)
                        //  - Option 4: Keep original time if it's a repeating alarm and calculate next occurrence
                        Log.d("BootReceiver", "Alarm ${a.id} was in the past. Rescheduling for next day.")
                        a.timeMillis = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                    }
                    AlarmScheduler.scheduleAlarm(appContext, a)
                }
            }
        }
    }

    // You'd need to implement this utility function (as discussed before)
    private fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    private fun sendMissingPermissionNotification(context: Context) {
        // Implementation to build and show a notification
        // This notification should inform the user that alarms couldn't be set
        // and should ideally take them to the app or app settings when tapped.
        // Example (very basic):
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

        // Intent to open your main activity
        val mainActivityIntent = Intent(context, MainActivity::class.java).apply { // Replace YourMainActivity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // You might add an extra to indicate why the app is being opened
            putExtra("reason", "request_exact_alarm_permission")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background) // Replace with your icon
            .setContentTitle("Alarm Reschedule Failed")
            .setContentText("Please grant permission to schedule exact alarms so WakeMe can reliably set your alarms.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager?.notify(PERMISSION_NOTIFICATION_ID, builder.build())
        Log.i("BootReceiver", "Sent notification for missing exact alarm permission.")
    }

    companion object {
        private const val PERMISSION_NOTIFICATION_ID = 1001 // Choose a unique ID
    }
}

