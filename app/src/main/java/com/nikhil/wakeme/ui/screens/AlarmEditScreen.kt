package com.nikhil.wakeme.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikhil.wakeme.R
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.ui.components.DayOfWeekSelector
import com.nikhil.wakeme.ui.components.ExpandableCard
import com.nikhil.wakeme.ui.components.TimePickerWheel
import com.nikhil.wakeme.util.Resource
import com.nikhil.wakeme.util.gradientBrush
import com.nikhil.wakeme.util.gradients
import com.nikhil.wakeme.viewmodels.AlarmEditViewModel
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarmId: Long,
    isNewAlarm: Boolean,
    goBack: () -> Unit,
    loadedAlarm: Alarm? = null,
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

    // Remember date picker state at the top Composable scope
    val datePickerState = remember(uiState.now) {
        DatePickerState(
            initialSelectedDateMillis = uiState.now, selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= System.currentTimeMillis()
                }
            }, locale = Locale.getDefault()
        )
    }

    LaunchedEffect(alarmId, isNewAlarm, loadedAlarm) {
        viewModel.loadAlarm(alarmId, isNewAlarm, loadedAlarm)
    }

    Scaffold(
        topBar = { AlarmEditTopBar(loadState, isNewAlarm, viewModel, goBack) }) { padding ->
        AppScreen(
            resource = loadState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            backgroundResId = R.drawable.set_alarm_bg,
            onRetry = { viewModel.loadAlarm(alarmId, isNewAlarm, loadedAlarm) },
            onError = { goBack() }) {
            AlarmEditContentLazy(
                uiState,
                viewModel,
                ringtonePickerLauncher,
                isNewAlarm,
                goBack,
                alarmId,
                datePickerState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditContentLazy(
    uiState: AlarmEditViewModel.UiState,
    viewModel: AlarmEditViewModel,
    ringtonePickerLauncher: ActivityResultLauncher<Intent>,
    isNewAlarm: Boolean,
    goBack: () -> Unit,
    alarmId: Long,
    datePickerState: DatePickerState
) {
    // Picker mode toggle
    var wheelMode by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TimePickerSection(uiState.now, wheelMode, viewModel::setHour, viewModel::setMinute)
            SwitchPickerButton(wheelMode) { wheelMode = it }
        }

        item {
            LabelSection(uiState.label, viewModel::setLabel)
        }

        item {
            DatePickerSection(
                selectedDate = uiState.now,
                onDateSelected = { viewModel.setDate(it) },
                datePickerState = datePickerState
            )
        }

        item {
            RepeatDaysSection(uiState.daysOfWeek, viewModel::setDays)
        }

        item {
            ExpandableCard("Snooze") {
                SnoozeSection(uiState.snoozeDuration, viewModel::setSnooze)
            }
        }

        item {
            RingtoneSection(uiState, ringtonePickerLauncher)
        }

        item {
            VibrationSection(uiState.vibration, viewModel::setVibration)
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
    now: Long, wheelMode: Boolean, setHour: (Int) -> Unit, setMinute: (Int) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
    ) {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)

        if (wheelMode) {
            TimePickerWheel(
                hour = currentHour,
                minute = currentMinute,
                onHourChange = { h -> setHour(h) },
                onMinuteChange = { m -> setMinute(m) })
        } else {
            val state = rememberTimePickerState(currentHour, currentMinute, true)
            LaunchedEffect(state.hour, state.minute) {
                setHour(state.hour)
                setMinute(state.minute)
            }
            TimePicker(
                state = state
            )
        }
    }
}

@Composable
fun SwitchPickerButton(wheelMode: Boolean, onSwitch: (Boolean) -> Unit) {
    TextButton(onClick = { onSwitch(!wheelMode) }) { Text("Switch Picker") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSection(label: String, setLabel: (String) -> Unit) {
    OutlinedTextField(
        value = label,
        onValueChange = { setLabel(it) },
        placeholder = { Text("Enter alarm name") },
        leadingIcon = {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_input_add),
                contentDescription = "Alarm Label Icon",
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp, brush = gradientBrush(gradients), shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp)),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedTextColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.secondary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.secondary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSection(
    selectedDate: Long?, onDateSelected: (Long) -> Unit, datePickerState: DatePickerState
) {
    var showDialog by remember { mutableStateOf(false) }

    val formattedDate = remember(selectedDate) {
        selectedDate?.let {
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(it))
        } ?: "Select Date"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() }) { showDialog = true }
            .border(2.dp, gradientBrush(gradients), RoundedCornerShape(20.dp))
            .clip(shape = RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Icon(
            painter = painterResource(R.drawable.calendar),
            contentDescription = "Date",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }

    if (showDialog) {
        DatePickerDialog(onDismissRequest = { showDialog = false }, confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                showDialog = false
            }) { Text("OK") }
        }, dismissButton = {
            TextButton(onClick = { showDialog = false }) { Text("Cancel") }
        }) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun RepeatDaysSection(daysOfWeek: Set<Int>, setDays: (Set<Int>) -> Unit) {
    DayOfWeekSelector(daysOfWeek) { day, isSelected ->
        val newSet = if (isSelected) daysOfWeek + day else daysOfWeek - day
        setDays(newSet)
    }
}

@Composable
fun SnoozeSection(snoozeDuration: Int, setSnooze: (Int) -> Unit) {
    val snoozeOptions = listOf(5, 10, 15)
    // Determine selected: use -1 for custom
    val selected = remember(snoozeDuration) {
        if (snoozeDuration in snoozeOptions) snoozeDuration else -1
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items(snoozeOptions + listOf(-1)) { opt ->
            val label = if (opt == -1) "Custom" else "$opt min"
            ElevatedFilterChip(
                selected = selected == opt, onClick = { setSnooze(opt) }, label = {
                Text(
                    text = label, style = MaterialTheme.typography.labelMedium
                )
            }, colors = FilterChipDefaults.elevatedFilterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedLabelColor = MaterialTheme.colorScheme.onTertiary
            )
            )
        }
    }

    if (selected == -1) {
        Spacer(Modifier.height(8.dp))
        var customSnooze by remember { mutableStateOf(if (snoozeDuration > 0) snoozeDuration.toString() else "") }

        OutlinedTextField(
            value = customSnooze,
            onValueChange = { input ->
                val sanitized = input.filter(Char::isDigit)
                customSnooze = sanitized
                setSnooze(sanitized.toIntOrNull() ?: 0)
            },
            label = { Text("Snooze in minutes") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(100.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
            )
        )
    }
}

@Composable
fun RingtoneSection(
    uiState: AlarmEditViewModel.UiState, ringtonePickerLauncher: ActivityResultLauncher<Intent>
) {
    OutlinedTextField(
        value = uiState.ringtoneTitle,
        onValueChange = {},
        enabled = false,
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.music_note),
                contentDescription = "Ringtone",
                tint = MaterialTheme.colorScheme.secondary
            )
        },
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
            }
            .border(
                width = 2.dp, brush = gradientBrush(gradients), shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp)),
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            disabledLeadingIconColor = MaterialTheme.colorScheme.secondary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.secondary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.secondary,
        ))
}

@Composable
fun VibrationSection(vibration: Boolean, setVibration: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .border(2.dp, gradientBrush(gradients), RoundedCornerShape(20.dp))
            .padding(16.dp, vertical = 8.dp)
            .clip(shape = RoundedCornerShape(20.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Vibrate when ringing")
        Switch(checked = vibration, onCheckedChange = { setVibration(it) })
    }
}

@Composable
fun SaveButton(isNewAlarm: Boolean, onSave: () -> Unit) {
    Button(onClick = onSave) {
        Text(if (isNewAlarm) "Create Alarm" else "Save Changes")
    }
}
