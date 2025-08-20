package com.nikhil.wakeme.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.alarms.AlarmService
import com.nikhil.wakeme.data.AlarmDatabase
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.util.NotificationHelper
import com.nikhil.wakeme.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmTriggerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AlarmDatabase.getInstance(application)
    private val _uiState = MutableStateFlow<Resource<AlarmEntity>>(Resource.Loading())
    val uiState = _uiState.asStateFlow()
    private val scheduler = AlarmScheduler

    fun loadAlarm(alarmId: Long) {
        _uiState.value = Resource.Loading()
        viewModelScope.launch {
            if (alarmId == -1L) {
                _uiState.value = Resource.Error("Invalid Alarm ID")
                return@launch
            }
            val alarm = db.alarmDao().getById(alarmId)
            if (alarm != null) {
                _uiState.value = Resource.Success(alarm)
            } else {
                _uiState.value = Resource.Error("Alarm not found")
            }
        }
    }

    private fun stopAlarmService() {
        val serviceIntent = Intent(getApplication(), AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        getApplication<Application>().startService(serviceIntent)
    }

    fun snoozeAlarm() {
        stopAlarmService()
        val currentState = uiState.value
        if (currentState is Resource.Success) {
            val alarm = currentState.data
            // Cancel the notification that shows the full-screen UI
            NotificationHelper.cancelNotification(getApplication(), alarm.id.toInt())

            val snoozedTimeMillis = System.currentTimeMillis() + alarm.snoozeDuration * 60 * 1000L
            val snoozedAlarm = alarm.copy(
                ringTime = snoozedTimeMillis,
                enabled = true
            )
            viewModelScope.launch {
                db.alarmDao().update(snoozedAlarm)
                scheduler.scheduleAlarm(getApplication(), snoozedAlarm)
            }
        }
    }

    fun stopAlarm() {
        stopAlarmService()
        val currentState = uiState.value
        if (currentState is Resource.Success) {
            val alarm = currentState.data
            // Cancel the notification that shows the full-screen UI
            NotificationHelper.cancelNotification(getApplication(), alarm.id.toInt())

            if (alarm.daysOfWeek.isNotEmpty()) {
                // For recurring alarms, we don't need to do anything here as the
                // worker has already scheduled the next occurrence.
                // We just need to stop the sound.
            } else {
                // For non-recurring alarms, they are already disabled by the worker.
                // We just need to stop the sound.
            }
        }
    }
}
