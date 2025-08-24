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
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.data.calculateNextTrigger
import com.nikhil.wakeme.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object AlarmScheduler {
    const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"
    const val EXTRA_TYPE = "EXTRA_TYPE" // UPCOMING or MAIN
    const val ALARM_WORK_TAG_PREFIX = "alarm_work_"

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    private fun cancelExistingAlarmTriggers(context: Context, alarm: Alarm) {
        val alarmId = alarm.id
        NotificationHelper.cancelNotification(context, alarmId)

        CoroutineScope(Dispatchers.IO).launch {
            val repo = AlarmRepository(context)
            repo.update(alarm.copy(enabled = false))
        }

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
        val workUpcomingTag = "$ALARM_WORK_TAG_PREFIX{$alarmId}_upcoming"
        val workMainTag = "$ALARM_WORK_TAG_PREFIX{$alarmId}_main"
        workManager.cancelAllWorkByTag(workUpcomingTag)
        workManager.cancelAllWorkByTag(workMainTag)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
            putExtra("ALARM_ID", alarmId)
        }
        context.stopService(serviceIntent)
    }

    // Decide how to handle alarm
    fun scheduleAlarm(context: Context, alarm: Alarm, isSnooze: Boolean = false) {

        if (!alarm.enabled) return
        val repo = AlarmRepository(context)

        cancelExistingAlarmTriggers(context, alarm)

        CoroutineScope(Dispatchers.IO).launch {
            // Update next occurrence
            val updated =
                if (isSnooze) {
                    val snoozedTimeMillis =
                        System.currentTimeMillis() + alarm.snoozeDuration * 60 * 1000L
                    alarm.copy(
                        enabled = true,
                        updatedAt = System.currentTimeMillis(),
                        nextTriggerAt = snoozedTimeMillis,
                        snoozeCount = alarm.snoozeCount + 1
                    )
                } else {
                    val next = alarm.calculateNextTrigger().timeInMillis
                    alarm.copy(
                        enabled = true,
                        updatedAt = System.currentTimeMillis(),
                        upcomingShown = false,
                        nextTriggerAt = next
                    )
                }
            repo.update(updated)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canScheduleExactAlarms(context)) {
                val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)

                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra(EXTRA_ALARM_ID, updated.id)
                    putExtra(EXTRA_TYPE, "MAIN")
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    updated.id.toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    updated.nextTriggerAt,
                    pendingIntent
                )

                scheduleUpcomingWithAlarmManager(context, updated)
            } else {
                scheduleWithWorkManager(context, updated, false)
                scheduleWithWorkManager(context, updated, true)
            }
        }
    }

    private fun scheduleWithWorkManager(context: Context, alarm: Alarm, isMain: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val workTag =
            if (isMain) "$ALARM_WORK_TAG_PREFIX${alarm.id}_main" else "$ALARM_WORK_TAG_PREFIX${alarm.id}_upcoming"

        val data = Data.Builder()
            .putLong(EXTRA_ALARM_ID, alarm.id)
            .putString(EXTRA_TYPE, if (isMain) "MAIN" else "UPCOMING")
            .build()
        val triggerAt = if (isMain) alarm.nextTriggerAt
        else alarm.nextTriggerAt - TimeUnit.MINUTES.toMillis(5)

        val delay = triggerAt - System.currentTimeMillis()
        if (delay <= 0) return

        val request = OneTimeWorkRequestBuilder<AlarmWorker>()
            .setInputData(data)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(workTag)
            .build()

        workManager.enqueue(request)
    }

    private fun scheduleUpcomingWithAlarmManager(context: Context, alarm: Alarm) {
        val triggerAt = alarm.nextTriggerAt - TimeUnit.MINUTES.toMillis(5)
        if (triggerAt <= System.currentTimeMillis()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_TYPE, "UPCOMING")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarm.id * 1000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent
            )
        } else {
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, alarm: Alarm) {
        cancelExistingAlarmTriggers(context, alarm)
    }
}
