package com.nikhil.wakeme.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nikhil.wakeme.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        if (alarmId != -1L) {
            CoroutineScope(Dispatchers.IO).launch {
                val repo = AlarmRepository(context)
                val alarm = repo.getById(alarmId)
                alarm?.let { alarm ->
                    repo.update(
                        alarm.copy(
                            missedCount = alarm.missedCount + 1,
                            snoozeCount = alarm.snoozeCount + 1
                        )
                    )
                    AlarmScheduler.scheduleAlarm(context, alarm, true)
                }
            }

            // Stop AlarmService if running
            val stopIntent = Intent(context, AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP
            }
            context.stopService(stopIntent)
        }
    }
}
