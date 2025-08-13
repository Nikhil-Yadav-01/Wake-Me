package com.nikhil.wakeme.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nikhil.wakeme.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AlarmDatabase.getInstance(context)
            val alarm = db.alarmDao().getById(alarmId) ?: return@launch
            if (!alarm.enabled) return@launch

            val svc = Intent(context, AlarmForegroundService::class.java).apply {
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(svc)
            } else {
                context.startService(svc)
            }
        }
    }
}
