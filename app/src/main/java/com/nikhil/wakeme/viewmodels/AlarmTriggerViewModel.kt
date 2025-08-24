package com.nikhil.wakeme.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.data.AlarmRepository
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

    fun snoozeAlarm() {
        val currentState = uiState.value
        if (currentState is Resource.Success) {
            val alarm = currentState.data
            viewModelScope.launch(Dispatchers.IO) {
                scheduler.scheduleAlarm(application, alarm, isSnooze = true)
                _uiState.value = Resource.Success(alarm)
            }
        }
    }

    fun stopAlarm() {
        val currentState = uiState.value
        if (currentState is Resource.Success) {
            val alarm = currentState.data

            if (alarm.isRecurring) {
                scheduler.scheduleAlarm(application, alarm)
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    scheduler.cancelAlarm(application, alarm)
                    _uiState.value = Resource.Success(alarm)
                }
            }
        }
    }
}
