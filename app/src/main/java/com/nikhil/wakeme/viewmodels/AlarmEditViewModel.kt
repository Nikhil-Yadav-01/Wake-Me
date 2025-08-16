package com.nikhil.wakeme.viewmodels

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
import java.util.Calendar

class AlarmEditViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AlarmRepository(application)

    private val _uiState = MutableStateFlow<Resource<AlarmEntity?>>(Resource.Loading())
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
        label: String,
        snoozeDuration: Int,
        ringtoneUri: Uri?,
        daysOfWeek: Set<Int>
    ) {
        viewModelScope.launch {
            try {
                val isNewAlarm = alarmId == 0L
                val existingAlarm = if (!isNewAlarm) repo.getById(alarmId) else null

                // Calculate the initial ring time based on the selected hour and minute
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    // If the time is in the past, move it to the next day
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                }

                val alarmEntity = (existingAlarm ?: AlarmEntity(ringTime = calendar.timeInMillis)).copy(
                    label = label,
                    snoozeDuration = snoozeDuration,
                    enabled = true,
                    ringtoneUri = ringtoneUri?.toString(),
                    daysOfWeek = daysOfWeek,
                    originalHour = hour,
                    originalMinute = minute
                )

                // The calculateNextTrigger function will handle the logic for repeating alarms
                val nextTriggerTime = alarmEntity.calculateNextTrigger()
                val finalAlarmToSave = alarmEntity.copy(ringTime = nextTriggerTime)

                val id = if (isNewAlarm) {
                    repo.insert(finalAlarmToSave)
                } else {
                    repo.update(finalAlarmToSave)
                    finalAlarmToSave.id
                }

                AlarmScheduler.scheduleAlarm(getApplication(), finalAlarmToSave.copy(id = id))
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to save alarm: ${e.message}")
            }
        }
    }
}
