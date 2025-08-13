package com.nikhil.wakeme.alarms

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.nikhil.wakeme.data.AlarmEntity

object AlarmScheduler {
    const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"
    private const val TAG = "AlarmScheduler"

    private fun getImmutableFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    }

    private fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return am.canScheduleExactAlarms()
        }
        return true
    }

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun scheduleAlarm(context: Context, alarm: AlarmEntity) {
        if (!alarm.enabled) return
        if (!canScheduleExactAlarms(context)) {
            Log.w(TAG, "Cannot schedule exact alarm, permission not granted.")
            return
        }

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val pi = PendingIntent.getBroadcast(context, alarm.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or getImmutableFlag()
        )
        val triggerAt = alarm.timeMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, alarmId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or getImmutableFlag()
        )
        am.cancel(pi)
        pi.cancel()
    }
}
