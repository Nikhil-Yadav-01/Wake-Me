package com.nikhil.wakeme.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.util.Resource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AlarmListViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AlarmRepository(application)

    // Exposes a reactive state of all alarms
    val uiState: StateFlow<Resource<List<Alarm>>> = repo.getAllFlow()
        .map { alarms ->
            when {
                alarms.isEmpty() -> Resource.Empty()
                else -> Resource.Success(alarms)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading()
        )
}
