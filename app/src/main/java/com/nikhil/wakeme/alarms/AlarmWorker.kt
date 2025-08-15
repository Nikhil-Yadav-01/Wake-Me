package com.nikhil.wakeme.alarms

import android.content.Context
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
            NotificationHelper.showAlarmNotification(context, alarm)

            // If it's a recurring alarm, reschedule it for the next occurrence
            if (alarm.daysOfWeek.isNotEmpty()) {
                val nextTrigger = alarm.calculateNextTrigger()
                if (alarm.timeMillis != nextTrigger) {
                    alarm.timeMillis = nextTrigger
                    db.alarmDao().update(alarm)
                    AlarmScheduler.scheduleAlarm(context, alarm)
                }
            } else {
                // For one-time alarms, disable it after it fires
                alarm.enabled = false
                db.alarmDao().update(alarm)
            }
        }

        return Result.success()
    }
}
