package com.nikhil.wakeme.alarms

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nikhil.wakeme.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val appContext = context.applicationContext ?: return

            CoroutineScope(Dispatchers.IO).launch {
                val db = AlarmDatabase.getInstance(appContext)
                val alarms = db.alarmDao().getEnabledAlarmsList()
                Log.i("BootReceiver", "Rescheduling ${alarms.size} alarms.")
                for (a in alarms) {
                    val nextTrigger = a.calculateNextTrigger()
                    if (a.timeMillis != nextTrigger) {
                        a.timeMillis = nextTrigger
                        db.alarmDao().update(a)
                    }
                    AlarmScheduler.scheduleAlarm(appContext, a)
                }
            }
        }
    }
}
