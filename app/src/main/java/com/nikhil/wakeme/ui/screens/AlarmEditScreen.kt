package com.nikhil.wakeme.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikhil.wakeme.R
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.ui.components.AppScreen
import com.nikhil.wakeme.ui.components.DayOfWeekSelector
import com.nikhil.wakeme.ui.components.ExpandableCard
import com.nikhil.wakeme.ui.components.TimePickerWheel
import com.nikhil.wakeme.util.Resource
import com.nikhil.wakeme.viewmodels.AlarmEditViewModel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarmId: Long,
    isNewAlarm: Boolean,
    goBack: () -> Unit,
    viewModel: AlarmEditViewModel = viewModel()
) {
    val loadState by viewModel.loadState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Ringtone picker launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java
                )
            } else {
                @Suppress("DEPRECATION") result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            uri?.let { viewModel.setRingtone(it) }
        }
    }

    Scaffold(
        topBar = { AlarmEditTopBar(loadState, isNewAlarm, viewModel, goBack) }) { padding ->
        AppScreen(
            resource = loadState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            backgroundResId = R.drawable.set_alarm_bg,
            onRetry = { viewModel.loadAlarm(alarmId, isNewAlarm) },
            onError = { goBack() }) {
            AlarmEditContentLazy(
                uiState, viewModel, ringtonePickerLauncher, isNewAlarm, goBack, alarmId
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditTopBar(
    loadState: Resource<Alarm?>,
    isNewAlarm: Boolean,
    viewModel: AlarmEditViewModel,
    goBack: () -> Unit
) {
    TopAppBar(title = { Text(if (isNewAlarm) "New Alarm" else "Edit Alarm") }, actions = {
        if (loadState is Resource.Success && !isNewAlarm) {
            loadState.data?.let { alarm ->
                IconButton(onClick = { viewModel.deleteAlarm(alarm); goBack() }) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }
        }
    })
}

@Composable
fun AlarmEditContentLazy(
    uiState: AlarmEditViewModel.UiState,
    viewModel: AlarmEditViewModel,
    ringtonePickerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    isNewAlarm: Boolean,
    goBack: () -> Unit,
    alarmId: Long
) {
    // Picker mode toggle
    var wheelMode by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            TimePickerSection(uiState, wheelMode, viewModel)
            SwitchPickerButton(wheelMode) { wheelMode = it }
        }

        item {
            ExpandableCard("Date") {
                DatePickerSection(
                    selectedDate = uiState.date,
                    onDateSelected = { viewModel.setDate(it) }
                )
            }
        }

        item {
            ExpandableCard("Label") {
                LabelSection(uiState, viewModel)
            }
        }

        item {
            ExpandableCard("Repeat Days") {
                RepeatDaysSection(uiState, viewModel)
            }
        }
        item {
            ExpandableCard("Repeat Type") {
                RepeatTypeSection(uiState, viewModel)
            }
        }
        item {
            ExpandableCard("Snooze") {
                SnoozeSection(uiState, viewModel)
            }
        }
        item {
            ExpandableCard("Ringtone") {
                RingtoneSection(uiState, ringtonePickerLauncher)
            }
        }
        item {
            ExpandableCard("Vibration") {
                VibrationSection(uiState, viewModel)
            }
        }

        item {
            ExpandableCard("Volume") {
                VolumeSection(uiState, viewModel)
            }
        }

        item {
            Spacer(Modifier.height(20.dp))

            SaveButton(isNewAlarm) {
                viewModel.saveAlarm(alarmId)
                goBack()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSection(
    uiState: AlarmEditViewModel.UiState,
    wheelMode: Boolean,
    viewModel: AlarmEditViewModel
) {
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val cal = Calendar.getInstance().apply { timeInMillis = uiState.date }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        if (wheelMode) {
            TimePickerWheel(
                hour = currentHour,
                minute = currentMinute,
                onHourChange = { h -> viewModel.setHour(h) },
                onMinuteChange = { m -> viewModel.setMinute(m) }
            )
        } else {
            val state = rememberTimePickerState(currentHour, currentMinute, true)
            LaunchedEffect(state.hour, state.minute) {
                viewModel.setHour(state.hour)
                viewModel.setMinute(state.minute)
            }
            TimePicker(state)
        }
    }
}

@Composable
fun SwitchPickerButton(wheelMode: Boolean, onSwitch: (Boolean) -> Unit) {
    TextButton(onClick = { onSwitch(!wheelMode) }) { Text("Switch Picker") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSection(
    selectedDate: Long?, // Pass current selected date in millis
    onDateSelected: (Long) -> Unit
) {
    // State for the DatePicker
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
    var showDialog by remember { mutableStateOf(false) }

    Row (
        modifier = Modifier.fillMaxWidth()
            .padding(4.dp)
            .clickable { showDialog = !showDialog },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = selectedDate?.let {
                DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                    .format(Date(it))
            } ?: "",
            style = MaterialTheme.typography.bodyMedium
        )
    }
    AnimatedVisibility (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun LabelSection(uiState: AlarmEditViewModel.UiState, viewModel: AlarmEditViewModel) {
    OutlinedTextField(
        value = uiState.label,
        onValueChange = { viewModel.setLabel(it) },
        label = { Text("Alarm Label") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun RepeatDaysSection(uiState: AlarmEditViewModel.UiState, viewModel: AlarmEditViewModel) {
    DayOfWeekSelector(uiState.daysOfWeek) { day, isSelected ->
        val newSet = if (isSelected) uiState.daysOfWeek + day else uiState.daysOfWeek - day
        viewModel.setDays(newSet)
    }

}

@Composable
fun RepeatTypeSection(uiState: AlarmEditViewModel.UiState, viewModel: AlarmEditViewModel) {
    val repeatOptions = listOf("Once", "Weekly", "Monthly", "Custom")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(repeatOptions) { option ->
            ElevatedFilterChip(
                selected = uiState.repeatType == option,
                onClick = { viewModel.setRepeat(option) },
                label = { Text(option) })
        }
    }

}

@Composable
fun SnoozeSection(uiState: AlarmEditViewModel.UiState, viewModel: AlarmEditViewModel) {
    val snoozeOptions = listOf(5, 10, 15)
    // Determine selected: use -1 for custom
    val selected = remember(uiState.snoozeDuration) {
        if (uiState.snoozeDuration in snoozeOptions) uiState.snoozeDuration else -1
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(snoozeOptions + listOf(-1)) { opt ->
            val label = if (opt == -1) "Custom" else "$opt min"
            ElevatedFilterChip(
                selected = selected == opt,
                onClick = { viewModel.setSnooze(opt) },
                label = { Text(label) })
        }
    }

    if (selected == -1) {
        Spacer(Modifier.height(12.dp))
        var customSnooze by remember { mutableStateOf(if (uiState.snoozeDuration > 0) uiState.snoozeDuration.toString() else "") }

        OutlinedTextField(
            value = customSnooze,
            onValueChange = { input ->
                val sanitized = input.filter(Char::isDigit)
                customSnooze = sanitized
                viewModel.setSnooze(sanitized.toIntOrNull() ?: 0)
            },
            label = { Text("Custom Snooze (minutes)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
fun RingtoneSection(
    uiState: AlarmEditViewModel.UiState,
    ringtonePickerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    OutlinedTextField(
        value = uiState.ringtoneTitle,
        onValueChange = {},
        enabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    uiState.ringtoneUri?.let {
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it
                        )
                    }
                }
                ringtonePickerLauncher.launch(intent)
            },
        singleLine = true
    )

}

@Composable
fun VibrationSection(uiState: AlarmEditViewModel.UiState, viewModel: AlarmEditViewModel) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Vibrate when ringing")
        Switch(checked = uiState.vibration, onCheckedChange = { viewModel.setVibration(it) })
    }

}

@Composable
fun VolumeSection(uiState: AlarmEditViewModel.UiState, viewModel: AlarmEditViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Slider(
            value = uiState.volume,
            onValueChange = { viewModel.setVolume(it) },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Volume: ${(uiState.volume * 100).toInt()}%")
    }
}


@Composable
fun SaveButton(isNewAlarm: Boolean, onSave: () -> Unit) {
    Button(onClick = onSave) {
        Text(if (isNewAlarm) "Create Alarm" else "Save Changes")
    }
}
