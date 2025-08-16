package com.nikhil.wakeme.alarms

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nikhil.wakeme.data.AlarmDatabase
import com.nikhil.wakeme.util.NotificationHelper

class AlarmWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val alarmId = inputData.getLong(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) {
            return Result.failure()
        }

        val db = AlarmDatabase.getInstance(context)
        val alarm = db.alarmDao().getById(alarmId) ?: return Result.failure()

        if (alarm.enabled) {
            // 1. Show the full-screen UI
            NotificationHelper.showAlarmNotification(context, alarm)

            // 2. Start the foreground service to play the sound
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                action = AlarmService.ACTION_START
                putExtra("ALARM_ID", alarm.id)
            }
            context.startService(serviceIntent)

            // 3. Reschedule or disable the alarm for the future
            if (alarm.daysOfWeek.isNotEmpty()) {
                val nextTriggerMillis = alarm.calculateNextTrigger()
                val updatedAlarm = alarm.copy(ringTime = nextTriggerMillis)
                db.alarmDao().update(updatedAlarm)
                AlarmScheduler.scheduleAlarm(context, updatedAlarm)
            } else {
                val updatedAlarm = alarm.copy(enabled = false)
                db.alarmDao().update(updatedAlarm)
            }
        }

        return Result.success()
    }
}
