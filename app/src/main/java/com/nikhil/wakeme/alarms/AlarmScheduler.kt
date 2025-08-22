package com.nikhil.wakeme.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.data.calculateNextTrigger
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

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
            putExtra("ALARM_ID", alarmId)
        }
        context.stopService(serviceIntent)
    }

    // Decide how to handle alarm
    fun scheduleAlarm(context: Context, alarm: Alarm, isSnooze: Boolean = false) {
        Log.d("AlarmScheduler", "scheduleAlarm: $alarm")
        if (!alarm.enabled) return
        val repo = AlarmRepository(context)

        cancelExistingAlarmTriggers(context, alarm.id)

        CoroutineScope(Dispatchers.IO).launch {
            // Update next occurrence or disable if one-shot
            val updated =
                if (isSnooze) {
                    val snoozedTimeMillis = System.currentTimeMillis() + alarm.snoozeDuration * 60 * 1000L
                    alarm.copy(
                        updatedAt = System.currentTimeMillis(),
                        nextTriggerAt = snoozedTimeMillis,
                        snoozeCount = alarm.snoozeCount + 1
                    )
                } else {
                    val next = alarm.calculateNextTrigger().timeInMillis
                    alarm.copy(
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
                Log.d("AlarmScheduler", "${updated.nextTriggerAt}  ${System.currentTimeMillis()}")

                scheduleUpcomingWithAlarmManager(context, updated)
            } else {
                scheduleUpcomingWithWorkManager(context, updated)
                scheduleMainWithWorkManager(context, updated)
            }
        }
    }

    private fun scheduleUpcomingWithWorkManager(context: Context, alarm: Alarm) {
        scheduleWithWorkManager(context, alarm, false)
    }

    private fun scheduleMainWithWorkManager(context: Context, alarm: Alarm) {
        scheduleWithWorkManager(context, alarm, true)
    }

    private fun scheduleWithWorkManager(context: Context, alarm: Alarm, isMain: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val workTag =
            if (isMain) "$ALARM_WORK_TAG_PREFIX${alarm.id}_main" else "$ALARM_WORK_TAG_PREFIX${alarm.id}_upcoming"

        val data = Data.Builder()
            .putLong(EXTRA_ALARM_ID, alarm.id)
            .putString(EXTRA_TYPE, if (isMain) "MAIN" else "UPCOMING")
            .build()
        val triggerAt = if (isMain) (alarm.nextTriggerAt ?: return)
        else (alarm.nextTriggerAt ?: return) - TimeUnit.MINUTES.toMillis(5)

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
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }

        Log.d("AlarmScheduler", "scheduled $alarm")
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        cancelExistingAlarmTriggers(context, alarmId)
    }
}
