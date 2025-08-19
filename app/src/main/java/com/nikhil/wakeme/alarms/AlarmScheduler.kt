package com.nikhil.wakeme.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nikhil.wakeme.data.AlarmEntity
import java.util.concurrent.TimeUnit

object AlarmScheduler {
    const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"
    const val ALARM_WORK_TAG_PREFIX = "alarm_work_"

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    private fun cancelExistingAlarmTriggers(context: Context, alarmId: Long) {
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)

        val workManager = WorkManager.getInstance(context)
        val workTag = "$ALARM_WORK_TAG_PREFIX$alarmId"
        workManager.cancelAllWorkByTag(workTag)

        // Explicitly stop the AlarmService and dismiss its notification when an alarm is cancelled
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
            putExtra("ALARM_ID", alarmId)
        }
        context.stopService(serviceIntent)
    }

    fun scheduleAlarm(context: Context, alarm: AlarmEntity) {
        if (!alarm.enabled) return

        // Always cancel existing triggers for this alarm ID before re-scheduling
        cancelExistingAlarmTriggers(context, alarm.id)

        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (canScheduleExactAlarms(context)) {
                alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.ringTime, pendingIntent)
            } else {
                // Fallback for devices where exact alarms cannot be scheduled (e.g., permission not granted)
                scheduleAlarmWithWorkManager(context, alarm)
            }
        } else {
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, alarm.ringTime, pendingIntent)
        }
    }

    fun scheduleAlarmWithWorkManager(context: Context, alarm: AlarmEntity) {
        val workManager = WorkManager.getInstance(context)
        val workTag = "$ALARM_WORK_TAG_PREFIX${alarm.id}"

        // No need to cancel WorkManager tasks here as cancelExistingAlarmTriggers is called in scheduleAlarm

        val alarmData = Data.Builder()
            .putLong(EXTRA_ALARM_ID, alarm.id)
            .build()

        val delay = alarm.ringTime - System.currentTimeMillis()
        if (delay < 0) return

        val workRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
            .setInputData(alarmData)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(workTag)
            .build()

        workManager.enqueue(workRequest)
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        cancelExistingAlarmTriggers(context, alarmId) // Use the comprehensive cancellation helper
    }
}
