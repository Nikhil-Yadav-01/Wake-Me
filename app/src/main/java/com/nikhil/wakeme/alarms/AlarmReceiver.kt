package com.nikhil.wakeme.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId != -1L) {
            val workManager = WorkManager.getInstance(context)
            val alarmData = Data.Builder()
                .putLong(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
                .setInputData(alarmData)
                .setInitialDelay(0, TimeUnit.MILLISECONDS) // Run immediately
                .addTag("${AlarmScheduler.ALARM_WORK_TAG_PREFIX}$alarmId")
                .build()
            workManager.enqueue(workRequest)
        }
    }
}
