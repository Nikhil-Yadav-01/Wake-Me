package com.nikhil.wakeme.alarms

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.data.calculateNextTrigger
import com.nikhil.wakeme.util.NotificationHelper

class AlarmWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("AlarmWorker", "doWork: $inputData")
        val alarmId = inputData.getLong(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val type = inputData.getString(AlarmScheduler.EXTRA_TYPE) ?: "MAIN"
        if (alarmId == -1L) return Result.failure()

        return try {
            when (type) {
                "UPCOMING" -> AlarmHandler.handleUpcoming(applicationContext, alarmId)
                else -> AlarmHandler.handleTrigger(applicationContext, alarmId)
            }
            Result.success()
        } catch (t: Throwable) {
            Log.e("AlarmWorker", "doWork error", t)
            Result.retry()
        }
    }
}
