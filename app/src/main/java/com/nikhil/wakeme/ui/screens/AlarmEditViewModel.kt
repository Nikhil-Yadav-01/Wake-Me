package com.nikhil.wakeme.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmEditViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AlarmRepository(application)

    // uiState now emits Resource<AlarmEntity?>
    private val _uiState = MutableStateFlow<Resource<AlarmEntity?>>(Resource.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadAlarm(alarmId: Long) {
        // Reset to Loading every time we load a new alarm or re-load
        _uiState.value = Resource.Loading
        if (alarmId == 0L) {
            _uiState.value = Resource.Success(null) // New alarm, no existing data
        } else {
            viewModelScope.launch {
                try {
                    val alarm = repo.getById(alarmId)
                    if (alarm != null) {
                        _uiState.value = Resource.Success(alarm)
                    } else {
                        _uiState.value = Resource.Error("Alarm not found") // Handle case where alarm doesn't exist
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
        label: String,
        snoozeDuration: Int,
        ringtoneUri: Uri?,
        daysOfWeek: Set<Int>
    ) {
        viewModelScope.launch {
            // Optionally, you could set _uiState to Resource.Loading() here as well
            // to show a saving indicator, but for quick operations, it might flicker.
            try {
                val isNewAlarm = alarmId == 0L
                val existingAlarm = if (!isNewAlarm) repo.getById(alarmId) else null

                val alarmEntity = (existingAlarm ?: AlarmEntity(timeMillis = 0)).copy(
                    label = label,
                    snoozeDuration = snoozeDuration,
                    enabled = true,
                    ringtoneUri = ringtoneUri?.toString(),
                    daysOfWeek = daysOfWeek,
                    originalHour = hour,
                    originalMinute = minute
                )

                val nextTriggerTime = alarmEntity.calculateNextTrigger()
                val finalAlarmToSave = alarmEntity.copy(timeMillis = nextTriggerTime)

                val id = if (isNewAlarm) {
                    repo.insert(finalAlarmToSave)
                } else {
                    repo.update(finalAlarmToSave)
                    finalAlarmToSave.id
                }

                AlarmScheduler.scheduleAlarm(getApplication(), finalAlarmToSave.copy(id = id))
                // If you want to reflect the saved state in UI: _uiState.value = Resource.Success(finalAlarmToSave.copy(id = id))
            } catch (e: Exception) {
                // Handle save error, e.g., show a Toast or update UI state to Resource.Error
                // _uiState.value = Resource.Error("Failed to save alarm: ${e.message}")
            }
        }
    }
}
