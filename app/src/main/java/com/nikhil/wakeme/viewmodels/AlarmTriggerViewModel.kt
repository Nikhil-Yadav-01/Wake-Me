package com.nikhil.wakeme.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.data.AlarmDatabase
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.util.NotificationHelper
import com.nikhil.wakeme.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmTriggerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AlarmDatabase.Companion.getInstance(application)
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

    fun snoozeAlarm() {
        val currentState = uiState.value
        if (currentState is Resource.Success) {
            val alarm = currentState.data
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
        val currentState = uiState.value
        if (currentState is Resource.Success) {
            val alarm = currentState.data
            NotificationHelper.cancelNotification(getApplication(), alarm.id.toInt())

            if (alarm.daysOfWeek.isNotEmpty()) {
                val nextRegularTrigger = alarm.calculateNextTrigger()
                val updatedAlarm = alarm.copy(
                    ringTime = nextRegularTrigger,
                    enabled = true
                )
                viewModelScope.launch {
                    db.alarmDao().update(updatedAlarm)
                    scheduler.scheduleAlarm(getApplication(), updatedAlarm)
                }
            } else {
                val updatedAlarm = alarm.copy(enabled = false)
                viewModelScope.launch {
                    db.alarmDao().update(updatedAlarm)
                    scheduler.cancelAlarm(getApplication(), alarm.id)
                }
            }
        }
    }
}