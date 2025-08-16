package com.nikhil.wakeme.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.data.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AlarmListUiState {
    data class Success(val alarms: List<AlarmEntity>) : AlarmListUiState
    object Loading : AlarmListUiState
}

class AlarmListViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AlarmRepository(application)

    val uiState: StateFlow<AlarmListUiState> = repo.getAllFlow()
        .map { AlarmListUiState.Success(it) } // Map the list to a Success state
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Keep flow alive for 5s after last collector
            initialValue = AlarmListUiState.Loading // The initial state is Loading
        )
}
