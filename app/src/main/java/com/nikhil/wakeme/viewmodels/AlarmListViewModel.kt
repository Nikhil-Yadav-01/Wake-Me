package com.nikhil.wakeme.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.util.Resource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AlarmListViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AlarmRepository(application)

    val uiState: StateFlow<Resource<List<AlarmEntity>>> = repo.getAllFlow()
        .map { alarms ->
            if (alarms.isEmpty()) {
                Resource.Empty()
            } else {
                Resource.Success(alarms)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = Resource.Loading()
        )
}