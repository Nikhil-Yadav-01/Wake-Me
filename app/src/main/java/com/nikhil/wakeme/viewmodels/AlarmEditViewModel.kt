package com.nikhil.wakeme.viewmodels

import android.app.Application
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.data.calculateNextTrigger
import com.nikhil.wakeme.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AlarmEditViewModel(
    private val app: Application
) : AndroidViewModel(app) {

    /** UI State observed by Compose */
    data class UiState(
        val now: Long = Calendar.getInstance().timeInMillis,
        val hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        val minute: Int = Calendar.getInstance().get(Calendar.MINUTE),
        val label: String = "",
        val snoozeDuration: Int = 10,
        val ringtoneUri: Uri? = null,
        val ringtoneTitle: String = "Default Ringtone",
        val daysOfWeek: Set<Int> = emptySet(),
        val vibration: Boolean = true
    )

    val repository: AlarmRepository = AlarmRepository(app)

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private val _loadState = MutableStateFlow<Resource<Alarm?>>(Resource.Loading())
    val loadState: StateFlow<Resource<Alarm?>> = _loadState

    /** Load alarm from DB in background */
    fun loadAlarm(alarmId: Long, isNew: Boolean) {
        if (isNew) {
            _loadState.value = Resource.Success(null) // no DB call
            return
        }
        viewModelScope.launch {
            _loadState.value = Resource.Loading()
            try {
                val alarm = withContext(Dispatchers.IO) { repository.getById(alarmId) }
                alarm?.let {
                    _uiState.value = UiState(
                        now = alarm.originalDateTime,
                        hour = alarm.hour,
                        minute = alarm.minute,
                        label = alarm.label.orEmpty(),
                        snoozeDuration = alarm.snoozeDuration,
                        ringtoneUri = alarm.ringtoneUri,
                        ringtoneTitle = alarm.ringtoneTitle ?: "Default Ringtone",
                        daysOfWeek = alarm.daysOfWeek,
                        vibration = alarm.vibration
                    )
                }
                _loadState.value = Resource.Success(alarm)
            } catch (e: Exception) {
                _loadState.value = Resource.Error(e.localizedMessage ?: "Error loading alarm")
            }
        }
    }

    /** Delete alarm in background */
    fun deleteAlarm(alarm: Alarm) {
        try {
            _loadState.value = Resource.Loading()
            viewModelScope.launch(Dispatchers.IO) {
                repository.delete(alarm)
                AlarmScheduler.cancelAlarm(app, alarm)
            }
            _loadState.value = Resource.Success(null)
        } catch (e: Exception) {
            _loadState.value = Resource.Error(e.localizedMessage ?: "Error deleting alarm")
        }
    }

    /** Save alarm in background */
    fun saveAlarm(alarmId: Long) {
        try {
            _loadState.value = Resource.Loading()
            val state = _uiState.value

            // Create alarm with proper next trigger calculation
            val baseAlarm = Alarm(
                id = alarmId,
                originalDateTime = state.now,
                label = state.label,
                snoozeDuration = state.snoozeDuration,
                ringtoneUri = state.ringtoneUri,
                ringtoneTitle = state.ringtoneTitle,
                daysOfWeek = state.daysOfWeek,
                vibration = state.vibration
            )

            // Calculate the proper next trigger time
            val next = baseAlarm.calculateNextTrigger().timeInMillis
            val alarm = baseAlarm.copy(nextTriggerAt = next)

            viewModelScope.launch(Dispatchers.IO) {
                val savedId = if (alarmId == 0L) {
                    repository.insert(alarm)
                } else {
                    repository.update(alarm)
                    alarmId
                }

                repository.getById(savedId)?.let {
                    AlarmScheduler.scheduleAlarm(app, it)
                }
            }
            _loadState.value = Resource.Success(alarm)
        } catch (_: Exception) {
            _loadState.value = Resource.Error("Error saving alarm")
        }
    }

    /** ----- UI State Setters (reactive) ----- */
    fun setDate(dateMillis: Long) = _uiState.update { state ->
        // Preserve existing hour and minute from current state.date
        val currentCal = Calendar.getInstance().apply { timeInMillis = state.now }
        val selectedCal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, currentCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, currentCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        state.copy(now = selectedCal.timeInMillis)
    }

    fun setHour(hour: Int) = _uiState.update { state ->
        val cal = Calendar.getInstance().apply { timeInMillis = state.now }
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.before(Calendar.getInstance())) {
//            Toast.makeText(getApplication(), "Time is in the past", Toast.LENGTH_SHORT).show()
            state
        } else {
            state.copy(now = cal.timeInMillis)
        }
    }

    fun setMinute(minute: Int) = _uiState.update { state ->
        val cal = Calendar.getInstance().apply { timeInMillis = state.now }
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.before(Calendar.getInstance())) {
//            Toast.makeText(getApplication(), "Time is in the past", Toast.LENGTH_SHORT).show()
            state
        } else {
            state.copy(now = cal.timeInMillis)
        }
    }

    fun setLabel(label: String) = _uiState.update { it.copy(label = label) }
    fun setSnooze(duration: Int) = _uiState.update { it.copy(snoozeDuration = duration) }

    /** Set ringtone and resolve title in background */
    fun setRingtone(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(ringtoneUri = null, ringtoneTitle = "Default") }
            return
        }
        viewModelScope.launch {
            val title = withContext(Dispatchers.IO) { resolveRingtoneTitle(uri) }
            _uiState.update { it.copy(ringtoneUri = uri, ringtoneTitle = title) }
        }
    }

    fun setDays(days: Set<Int>) = _uiState.update { it.copy(daysOfWeek = days) }
    fun setVibration(enabled: Boolean) = _uiState.update { it.copy(vibration = enabled) }

    /** ----- Helper: Resolve ringtone title safely ----- */
    fun resolveRingtoneTitle(uri: Uri?): String =
        try {
            uri?.let {
                RingtoneManager.getRingtone(getApplication(), it)
                    ?.getTitle(getApplication())
            } ?: "Default Ringtone"
        } catch (_: Exception) {
            "Default Ringtone"
        }
}
