package com.nikhil.wakeme.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nikhil.wakeme.data.AlarmDatabase
import com.nikhil.wakeme.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_TYPE) ?: "MAIN"
        if (alarmId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AlarmDatabase.getInstance(context)
            val alarm = db.alarmDao().getById(alarmId) ?: return@launch
            if (!alarm.enabled) return@launch

            when (type) {
                "UPCOMING" -> {
                    if (!alarm.upcomingShown) {
                        NotificationHelper.showUpcomingAlarmNotification(context, alarm)
                        alarm.upcomingShown = true
                        db.alarmDao().update(alarm)
                    }
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
                }
            }
        }
    }
}
