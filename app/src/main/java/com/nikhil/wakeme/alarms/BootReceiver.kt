package com.nikhil.wakeme.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nikhil.wakeme.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val appContext = context.applicationContext ?: return

            CoroutineScope(Dispatchers.IO).launch {
                val db = AlarmDatabase.getInstance(appContext)
                val alarms = db.alarmDao().getEnabledAlarmsList()
                Log.i("BootReceiver", "Rescheduling ${alarms.size} alarms after reboot/update.")

                for (alarm in alarms) {
                    val nextTrigger = alarm.calculateNextTrigger()
                    val updatedAlarm = if (alarm.ringTime != nextTrigger) {
                        alarm.copy(ringTime = nextTrigger).also { db.alarmDao().update(it) }
                    } else alarm

                    AlarmScheduler.scheduleAlarm(appContext, updatedAlarm)
                }
            }
        }
    }
}
