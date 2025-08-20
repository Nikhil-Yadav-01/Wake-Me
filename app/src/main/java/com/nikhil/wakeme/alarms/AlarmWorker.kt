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
        val type = inputData.getString(AlarmScheduler.EXTRA_TYPE) ?: "MAIN"
        if (alarmId == -1L) return Result.failure()

        val db = AlarmDatabase.getInstance(context)
        val alarm = db.alarmDao().getById(alarmId) ?: return Result.failure()

        if (!alarm.enabled) return Result.success()

        return when (type) {
            "UPCOMING" -> {
                if (!alarm.upcomingShown) {
                    NotificationHelper.showUpcomingAlarmNotification(context, alarm)
                    alarm.upcomingShown = true
                    db.alarmDao().update(alarm)
                }
                Result.success()
            }
            "MAIN" -> {
                NotificationHelper.cancelNotification(context, alarm.id.toInt())

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    action = AlarmService.ACTION_START
                    putExtra("ALARM_ID", alarm.id)
                }
                context.startService(serviceIntent)

                if (alarm.daysOfWeek.isNotEmpty()) {
                    val nextTrigger = alarm.calculateNextTrigger()
                    val updated = alarm.copy(ringTime = nextTrigger, upcomingShown = false)
                    db.alarmDao().update(updated)
                    AlarmScheduler.scheduleAlarm(context, updated)
                } else {
                    db.alarmDao().update(alarm.copy(enabled = false))
                }

                Result.success()
            }
            else -> Result.failure()
        }
    }
}
