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
import com.nikhil.wakeme.data.AlarmDatabase
import com.nikhil.wakeme.data.AlarmEntity
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

    fun scheduleAlarm(context: Context, alarm: AlarmEntity) {
        if (!alarm.enabled) return

        cancelExistingAlarmTriggers(context, alarm.id)

        // 🔑 Reset upcomingShown before scheduling
        alarm.upcomingShown = false
        CoroutineScope(Dispatchers.IO).launch {
            val db = AlarmDatabase.getInstance(context)
            db.alarmDao().update(alarm)
        }

        val alarmManager = ContextCompat.getSystemService(context, AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canScheduleExactAlarms(context)) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_ALARM_ID, alarm.id)
                putExtra(EXTRA_TYPE, "MAIN")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarm.ringTime,
                pendingIntent
            )

            scheduleUpcomingWithAlarmManager(context, alarm)
        } else {
            scheduleUpcomingWithWorkManager(context, alarm)
            scheduleMainWithWorkManager(context, alarm)
        }
    }

    private fun scheduleUpcomingWithWorkManager(context: Context, alarm: AlarmEntity) {
        val workManager = WorkManager.getInstance(context)
        val workTag = "$ALARM_WORK_TAG_PREFIX${alarm.id}_upcoming"

        val data = Data.Builder()
            .putLong(EXTRA_ALARM_ID, alarm.id)
            .putString(EXTRA_TYPE, "UPCOMING")
            .build()

        val triggerAt = alarm.ringTime - TimeUnit.MINUTES.toMillis(5)
        val delay = triggerAt - System.currentTimeMillis()
        if (delay <= 0) return

        val request = OneTimeWorkRequestBuilder<AlarmWorker>()
            .setInputData(data)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(workTag)
            .build()

        workManager.enqueue(request)
    }

    private fun scheduleMainWithWorkManager(context: Context, alarm: AlarmEntity) {
        val workManager = WorkManager.getInstance(context)
        val workTag = "$ALARM_WORK_TAG_PREFIX${alarm.id}_main"

        val data = Data.Builder()
            .putLong(EXTRA_ALARM_ID, alarm.id)
            .putString(EXTRA_TYPE, "MAIN")
            .build()

        val delay = alarm.ringTime - System.currentTimeMillis()
        if (delay <= 0) return

        val request = OneTimeWorkRequestBuilder<AlarmWorker>()
            .setInputData(data)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(workTag)
            .build()

        workManager.enqueue(request)
    }

    private fun scheduleUpcomingWithAlarmManager(context: Context, alarm: AlarmEntity) {
        val triggerAt = alarm.ringTime - TimeUnit.MINUTES.toMillis(5)
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
            alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        cancelExistingAlarmTriggers(context, alarmId)
    }
}
