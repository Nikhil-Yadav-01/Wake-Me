package com.nikhil.wakeme.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.alarms.AlarmService
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.util.NotificationHelper
import com.nikhil.wakeme.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmTriggerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AlarmRepository(application)
    private val scheduler = AlarmScheduler

    private val _uiState = MutableStateFlow<Resource<Alarm>>(Resource.Loading())
    val uiState = _uiState.asStateFlow()

    fun loadAlarm(alarmId: Long) {
        _uiState.value = Resource.Loading()
        viewModelScope.launch {
            if (alarmId == -1L) {
                _uiState.value = Resource.Error("Invalid Alarm ID")
                return@launch
            }

            val alarm = withContext(Dispatchers.IO) { repo.getById(alarmId) }
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
            viewModelScope.launch(Dispatchers.IO) {
                scheduler.scheduleAlarm(getApplication(), alarm, isSnooze = true)
                _uiState.value = Resource.Success(alarm)
            }
        }
    }

    fun stopAlarm() {
        stopAlarmService()
        val currentState = uiState.value
        if (currentState is Resource.Success) {
            val alarm = currentState.data
            NotificationHelper.cancelNotification(getApplication(), alarm.id.toInt())

            if (alarm.isRecurring) {
                scheduler.scheduleAlarm(getApplication(), alarm)
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    repo.update(alarm.copy(enabled = false))
                    _uiState.value = Resource.Success(alarm)
                }
            }
        }
    }
}
