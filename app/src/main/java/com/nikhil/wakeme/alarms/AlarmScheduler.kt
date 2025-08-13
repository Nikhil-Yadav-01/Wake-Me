package com.nikhil.wakeme.alarms

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nikhil.wakeme.data.AlarmEntity
import java.util.concurrent.TimeUnit

object AlarmScheduler {
    const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"
    private const val ALARM_WORK_TAG_PREFIX = "alarm_work_"

    fun scheduleAlarm(context: Context, alarm: AlarmEntity) {
        if (!alarm.enabled) return

        val workManager = WorkManager.getInstance(context)
        val workTag = "$ALARM_WORK_TAG_PREFIX${alarm.id}"

        // Cancel any existing work for this alarm
        workManager.cancelAllWorkByTag(workTag)

        val alarmData = Data.Builder()
            .putLong(EXTRA_ALARM_ID, alarm.id)
            .build()

        val delay = alarm.timeMillis - System.currentTimeMillis()
        if (delay < 0) return 

        val workRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
            .setInputData(alarmData)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(workTag)
            .build()

        workManager.enqueue(workRequest)
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val workManager = WorkManager.getInstance(context)
        val workTag = "$ALARM_WORK_TAG_PREFIX$alarmId"
        workManager.cancelAllWorkByTag(workTag)
    }
}
