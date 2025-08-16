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

            // After the alarm fires, we decide what the next trigger time should be.
            if (alarm.daysOfWeek.isNotEmpty()) {
                // This is a recurring alarm. We must calculate its next regular occurrence.
                val nextTriggerMillis = alarm.calculateNextTrigger()
                val updatedAlarm = alarm.copy(ringTime = nextTriggerMillis)
                db.alarmDao().update(updatedAlarm)
                // Reschedule the alarm for its next regular time.
                AlarmScheduler.scheduleAlarm(context, updatedAlarm)
            } else {
                // This is a one-time alarm. It should be disabled after it fires.
                val updatedAlarm = alarm.copy(enabled = false)
                db.alarmDao().update(updatedAlarm)
                // We don't reschedule, as the alarm is now disabled.
            }
        }

        return Result.success()
    }
}
