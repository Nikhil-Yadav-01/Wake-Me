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
        }

        return Result.success()
    }
}
