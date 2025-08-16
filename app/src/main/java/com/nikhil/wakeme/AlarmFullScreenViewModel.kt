package com.nikhil.wakeme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.data.AlarmDatabase
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmFullScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AlarmDatabase.getInstance(application)
    private val _alarm = MutableStateFlow<AlarmEntity?>(null)
    val alarm = _alarm.asStateFlow()
    private val scheduler = AlarmScheduler

    fun loadAlarm(alarmId: Long) {
        viewModelScope.launch {
            _alarm.value = db.alarmDao().getById(alarmId)
        }
    }

    fun snoozeAlarm() {
        alarm.value?.let {
            NotificationHelper.cancelNotification(getApplication(), it.id.toInt())

            // Calculate snooze time as current time + snooze duration
            val snoozedTimeMillis = System.currentTimeMillis() + it.snoozeDuration * 60 * 1000L

            val snoozedAlarm = it.copy(
                timeMillis = snoozedTimeMillis,
                enabled = true // Ensure alarm remains enabled
                // originalHour and originalMinute are NOT modified here
            )
            viewModelScope.launch {
                db.alarmDao().update(snoozedAlarm)
                scheduler.scheduleAlarm(getApplication(), snoozedAlarm)
            }
        }
    }

    fun stopAlarm() {
        alarm.value?.let {
            NotificationHelper.cancelNotification(getApplication(), it.id.toInt())

            if (it.daysOfWeek.isNotEmpty()) {
                // For recurring alarms, reschedule to the NEXT REGULAR occurrence
                val nextRegularTrigger = it.calculateNextTrigger() // Use the updated calculateNextTrigger
                val updatedAlarm = it.copy(
                    timeMillis = nextRegularTrigger,
                    enabled = true // Keep enabled for recurring alarms
                )
                viewModelScope.launch {
                    db.alarmDao().update(updatedAlarm)
                    scheduler.scheduleAlarm(getApplication(), updatedAlarm)
                }
            } else {
                // For one-time alarms, disable it
                val updatedAlarm = it.copy(enabled = false)
                viewModelScope.launch {
                    db.alarmDao().update(updatedAlarm)
                    scheduler.cancelAlarm(getApplication(), it.id)
                }
            }
        }
    }
}
