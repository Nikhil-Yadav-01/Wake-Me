package com.nikhil.wakeme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.data.AlarmDatabase
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.alarms.AlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmFullScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AlarmDatabase.getInstance(application)
    private val _alarm = MutableStateFlow<AlarmEntity?>(null)
    val alarm = _alarm.asStateFlow()

    fun loadAlarm(alarmId: Long) {
        viewModelScope.launch {
            _alarm.value = db.alarmDao().getById(alarmId)
        }
    }

    fun snoozeAlarm() {
        alarm.value?.let {
            val snoozedAlarm = it.copy(
                timeMillis = System.currentTimeMillis() + it.snoozeDuration * 60 * 1000
            )
            viewModelScope.launch {
                db.alarmDao().update(snoozedAlarm)
                AlarmScheduler.scheduleAlarm(getApplication(), snoozedAlarm)
            }
        }
    }

    fun stopAlarm() {
        alarm.value?.let {
            val updatedAlarm = it.copy(enabled = false)
            viewModelScope.launch {
                db.alarmDao().update(updatedAlarm)
                AlarmScheduler.cancelAlarm(getApplication(), it.id)
            }
        }
    }
}
