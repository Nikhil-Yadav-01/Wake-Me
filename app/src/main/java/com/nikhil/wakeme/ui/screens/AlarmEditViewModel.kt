package com.nikhil.wakeme.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.data.AlarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface AlarmEditUiState {
    data class Success(val alarm: AlarmEntity?) : AlarmEditUiState
    object Loading : AlarmEditUiState
}

class AlarmEditViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AlarmRepository(application)

    private val _uiState = MutableStateFlow<AlarmEditUiState>(AlarmEditUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadAlarm(alarmId: Long) {
        if (alarmId == 0L) {
            _uiState.value = AlarmEditUiState.Success(null)
        } else {
            viewModelScope.launch {
                _uiState.value = AlarmEditUiState.Success(repo.getById(alarmId))
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
            val isNewAlarm = alarmId == 0L
            val existingAlarm = if (!isNewAlarm) repo.getById(alarmId) else null

            // Determine the hour and minute to save as originalHour/Minute.
            // If it's an existing alarm and the time wasn't changed by the user (not directly passed as new hour/minute),
            // we should ideally preserve the originalHour/Minute from the existingAlarm.
            // For simplicity here, we assume hour and minute passed are always the user's latest selection.
            val hourToSave = hour
            val minuteToSave = minute

            val alarmEntity = (existingAlarm ?: AlarmEntity(timeMillis = 0)).copy(
                label = label,
                snoozeDuration = snoozeDuration,
                enabled = true,
                ringtoneUri = ringtoneUri?.toString(),
                daysOfWeek = daysOfWeek,
                originalHour = hourToSave,
                originalMinute = minuteToSave
            )

            // Calculate the initial timeMillis using the robust calculator in AlarmEntity
            // This ensures the alarm is initially scheduled for the correct next occurrence
            // based on the original time and daysOfWeek.
            val nextTriggerTime = alarmEntity.calculateNextTrigger()
            val finalAlarmToSave = alarmEntity.copy(timeMillis = nextTriggerTime)

            val id = if (isNewAlarm) {
                repo.insert(finalAlarmToSave)
            } else {
                repo.update(finalAlarmToSave)
                finalAlarmToSave.id
            }

            // Schedule with the final, correct entity (ensuring the ID is set for a new alarm)
            AlarmScheduler.scheduleAlarm(getApplication(), finalAlarmToSave.copy(id = id))
        }
    }
}
