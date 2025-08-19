package com.nikhil.wakeme.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.data.toAlarmEntity
import com.nikhil.wakeme.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmEditViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AlarmRepository(application)

    private val _uiState = MutableStateFlow<Resource<Alarm?>>(Resource.Initial())
    val uiState = _uiState.asStateFlow()

    fun loadAlarm(alarmId: Long) {
        _uiState.value = Resource.Loading()
        if (alarmId == 0L) {
            _uiState.value = Resource.Success(null)
        } else {
            viewModelScope.launch {
                try {
                    val alarm = repo.getById(alarmId)
                    if (alarm != null) {
                        _uiState.value = Resource.Success(alarm)
                    } else {
                        _uiState.value = Resource.Error("Alarm not found")
                    }
                } catch (e: Exception) {
                    _uiState.value = Resource.Error("Failed to load alarm: ${e.message}")
                }
            }
        }
    }

    fun saveAlarm(
        alarmId: Long,
        hour: Int,
        minute: Int,
        label: String?,
        snoozeDuration: Int,
        ringtoneUri: Uri?,
        daysOfWeek: Set<Int>
    ) {
        viewModelScope.launch {
            try {
                val isNewAlarm = alarmId == 0L
                // existingAlarm will be of type Alarm?
                val existingAlarm = if (!isNewAlarm) repo.getById(alarmId) else null

                // Create an Alarm object from the current UI state
                val currentAlarm = Alarm(
                    id = existingAlarm?.id ?: 0L,
                    hour = hour,
                    minute = minute,
                    label = label,
                    snoozeDuration = snoozeDuration,
                    enabled = true, // Alarms are enabled on save
                    ringtoneUri = ringtoneUri,
                    daysOfWeek = daysOfWeek,
                    originalHour = hour, // Set originalHour and originalMinute from the UI input
                    originalMinute = minute,
                    createdAt = existingAlarm?.createdAt ?: System.currentTimeMillis()
                )

                // Convert the Alarm object to AlarmEntity for database operations
                val alarmEntityToSave = currentAlarm.toAlarmEntity()

                val id = if (isNewAlarm) {
                    repo.insert(alarmEntityToSave)
                } else {
                    repo.update(alarmEntityToSave)
                    alarmEntityToSave.id
                }

                // Schedule the alarm using the AlarmEntity (which has the correct ringTime)
                AlarmScheduler.scheduleAlarm(getApplication(), alarmEntityToSave.copy(id = id))
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to save alarm: ${e.message}")
            }
        }
    }
}
