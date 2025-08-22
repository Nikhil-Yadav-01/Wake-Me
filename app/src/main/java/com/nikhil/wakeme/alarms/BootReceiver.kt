package com.nikhil.wakeme.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.data.calculateNextTrigger
import com.nikhil.wakeme.data.toAlarmEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("BootReceiver", "onReceive() called with: context = $context, intent = $intent")
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val appContext = context.applicationContext ?: return

            CoroutineScope(Dispatchers.IO).launch {
                val repo = AlarmRepository(appContext)
                val alarms = repo.getEnabledAlarmsList()
                Log.i("BootReceiver", "Rescheduling ${alarms.size} alarms after reboot/update.")
                alarms.forEach { alarm ->
//                    val next = alarm.calculateNextTrigger().timeInMillis
//                    val updated = alarm.copy(nextTriggerAt = next, upcomingShown = false)
//                    repo.update(updated)
                    AlarmScheduler.scheduleAlarm(appContext, alarm)
                }
            }
        }
    }
}
