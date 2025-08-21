package com.nikhil.wakeme.viewmodels

import android.app.Application
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.data.toAlarmEntity
import com.nikhil.wakeme.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmEditViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AlarmRepository(application)

    private val _uiState = MutableStateFlow<Resource<Alarm?>>(Resource.Loading())
    val uiState = _uiState.asStateFlow()

    fun loadAlarm(alarmId: Long) {
        _uiState.value = Resource.Loading()

        if (alarmId == 0L) {
            _uiState.value = Resource.Success(null)
        } else {
            viewModelScope.launch {
                try {
                        val alarm = withContext(Dispatchers.IO) { repo.getById(alarmId) }

                        if (alarm != null) {
                            _uiState.value = Resource.Success(
                                alarm.copy(
                                    ringtoneTitle = resolveRingtoneTitle(alarm.ringtoneUri)
                                )
                            )
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
                withContext(Dispatchers.IO) {
                    val existingAlarm = if (!isNewAlarm) repo.getById(alarmId) else null

                    val ringtoneTitle = resolveRingtoneTitle(ringtoneUri)

                    val currentAlarm = Alarm(
                        id = existingAlarm?.id ?: 0L,
                        hour = hour,
                        minute = minute,
                        label = label,
                        snoozeDuration = snoozeDuration,
                        enabled = true,
                        ringtoneUri = ringtoneUri,
                        daysOfWeek = daysOfWeek,
                        originalHour = hour,
                        originalMinute = minute,
                        createdAt = existingAlarm?.createdAt ?: System.currentTimeMillis(),
                        upcomingShown = false,
                        ringtoneTitle = ringtoneTitle
                    )

                    val alarmEntityToSave = currentAlarm.toAlarmEntity()
                    val id = if (isNewAlarm) {
                        repo.insert(alarmEntityToSave)
                    } else {
                        repo.update(alarmEntityToSave)
                        alarmEntityToSave.id
                    }

                    // schedule alarm
                    AlarmScheduler.scheduleAlarm(
                        getApplication(),
                        currentAlarm.toAlarmEntity().copy(id = id)
                    )
                    _uiState.value = Resource.Success(currentAlarm.copy(id = id))
                }
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to save alarm: ${e.message}")
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repo.delete(alarm.toAlarmEntity())
                    AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
                }
            } catch (e: Exception) {
                _uiState.value = Resource.Error("Failed to delete alarm: ${e.message}")
            }
        }
    }

    private suspend fun resolveRingtoneTitle(uri: Uri?): String =
        try {
            uri?.let {
                RingtoneManager.getRingtone(getApplication(), it)
                    ?.getTitle(getApplication())
            } ?: "Default Ringtone"
        } catch (_: Exception) {
            "Default Ringtone"
        }

}
