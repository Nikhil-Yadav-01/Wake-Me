package com.nikhil.wakeme.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId != -1L) {
            val workManager = WorkManager.getInstance(context)
            val alarmData = Data.Builder()
                .putLong(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                .build()

            val alarmWorkRequest = OneTimeWorkRequestBuilder<AlarmWorker>()
                .setInputData(alarmData)
                .addTag("${AlarmScheduler.ALARM_WORK_TAG_PREFIX}$alarmId")
                .build()

            // Use enqueueUniqueWork to prevent multiple workers for the same alarm event.
            // This is the key to preventing the race condition.
            val uniqueWorkName = "alarm_work_$alarmId"
            workManager.enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE, // If work already exists, replace it. This is safe.
                alarmWorkRequest
            )
        }
    }
}
