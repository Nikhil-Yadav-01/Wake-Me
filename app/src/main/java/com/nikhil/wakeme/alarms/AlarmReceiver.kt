package com.nikhil.wakeme.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_TYPE) ?: "MAIN"
        if (alarmId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            when (type) {
                "UPCOMING" -> AlarmHandler.handleUpcoming(context, alarmId)
                else -> AlarmHandler.handleTrigger(context, alarmId)
            }
        }
    }
}