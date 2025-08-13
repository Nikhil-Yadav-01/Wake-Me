package com.nikhil.wakeme.alarms

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nikhil.wakeme.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STOP = "com.nikhil.wakeme.ACTION_STOP"
        const val ACTION_SNOOZE = "com.nikhil.wakeme.ACTION_SNOOZE"
    }

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val action = intent.action ?: return
        if (alarmId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AlarmDatabase.getInstance(context)
            val alarm = db.alarmDao().getById(alarmId) ?: return@launch

            when (action) {
                ACTION_STOP -> {
                    AlarmScheduler.cancelAlarm(context, alarmId)
                    alarm.enabled = false
                    db.alarmDao().update(alarm)
                    context.stopService(Intent(context, AlarmForegroundService::class.java).apply {
                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                    })
                }
                ACTION_SNOOZE -> {
                    val snoozeMillis = alarm.snoozeMinutes * 60L * 1000L
                    alarm.timeMillis = System.currentTimeMillis() + snoozeMillis
                    alarm.autoSnoozeCount = 0 // reset counter on manual snooze
                    db.alarmDao().update(alarm)
                    // This is safe because the alarm was already scheduled,
                    // so the permission must have been granted.
                    AlarmScheduler.scheduleAlarm(context, alarm)
                    context.stopService(Intent(context, AlarmForegroundService::class.java).apply {
                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                    })
                }
            }
        }
    }
}
