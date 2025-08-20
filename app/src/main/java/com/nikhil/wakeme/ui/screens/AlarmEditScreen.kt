package com.nikhil.wakeme.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikhil.wakeme.R
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.ui.components.AppScreen
import com.nikhil.wakeme.ui.components.DayOfWeekSelector
import com.nikhil.wakeme.ui.components.ErrorMode
import com.nikhil.wakeme.ui.components.GradientSectionCard
import com.nikhil.wakeme.ui.components.TimePickerWheel
import com.nikhil.wakeme.util.Resource
import com.nikhil.wakeme.util.gradients
import com.nikhil.wakeme.viewmodels.AlarmEditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    goBack: () -> Unit,
    alarmId: Long,
    viewModel: AlarmEditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isNewAlarm = alarmId == 0L

    // Load alarm
    LaunchedEffect(alarmId) {
        if (isNewAlarm) viewModel.loadAlarm(alarmId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNewAlarm) "New Alarm" else "Edit Alarm",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    if (uiState is Resource.Success && !isNewAlarm) {
                        val alarm = (uiState as Resource.Success<Alarm?>).data
                        IconButton(onClick = {
                            alarm?.let { viewModel.deleteAlarm(it) }
                            goBack()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = "Delete",
                                tint = Color.Red
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        AppScreen(
            resource = uiState,
            modifier = Modifier.fillMaxSize().padding(padding),
            backgroundResId = R.drawable.set_alarm_bg,
            onRetry = { viewModel.loadAlarm(alarmId) },
            errorMode = ErrorMode.Toast,
            onError = { goBack() }
        ) { alarm ->
            AlarmEditContent(
                alarm = alarm,
                isNewAlarm = isNewAlarm,
                onSave = { hour, minute, label, snooze, ringtone, days ->
                    viewModel.saveAlarm(
                        alarmId = alarmId,
                        hour = hour,
                        minute = minute,
                        label = label,
                        snoozeDuration = snooze,
                        ringtoneUri = ringtone,
                        daysOfWeek = days
                    )
                    goBack()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditContent(
    alarm: Alarm?,
    isNewAlarm: Boolean,
    onSave: (Int, Int, String, Int, Uri?, Set<Int>) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var hour by remember { mutableStateOf(alarm?.hour ?: calendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(alarm?.minute ?: calendar.get(Calendar.MINUTE)) }
    var label by remember { mutableStateOf(alarm?.label.orEmpty()) }
    var customSnooze by remember { mutableStateOf(alarm?.snoozeDuration?.toString() ?: "10") }
    var selectedSnooze by remember {
        mutableStateOf(
            if (alarm?.snoozeDuration in listOf(5, 10, 15)) alarm?.snoozeDuration.toString() else "Custom"
        )
    }
    var ringtoneUri by remember { mutableStateOf(alarm?.ringtoneUri) }
    var selectedDays by remember { mutableStateOf(alarm?.daysOfWeek ?: emptySet()) }
    var wheelMode by remember { mutableStateOf(false) }

    // Launcher for ringtone picker
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                ringtoneUri = uri
            }
        }
    }

    // Ringtone title is derived directly from URI (no separate state needed)
    var ringtoneTitle by remember { mutableStateOf("Default Ringtone") }

    LaunchedEffect(ringtoneUri) {
        ringtoneTitle = withContext(Dispatchers.IO) {
            try {
                ringtoneUri?.let { uri ->
                    RingtoneManager.getRingtone(context, uri)?.getTitle(context)
                } ?: "Default Ringtone"
            } catch (e: Exception) {
                "Default Ringtone"
            }
        }
    }


    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Time Picker
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (wheelMode) {
                TimePickerWheel(
                    hour = hour,
                    minute = minute,
                    onHourChange = { hour = it },
                    onMinuteChange = { minute = it }
                )
            } else {
                val state = rememberTimePickerState(hour, minute, true)
                LaunchedEffect(state.hour, state.minute) {
                    hour = state.hour
                    minute = state.minute
                }
                TimePicker(state)
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { wheelMode = !wheelMode }) {
            Text("Switch Picker")
        }

        // Label
        GradientSectionCard(gradients) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Alarm Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Repeat
        GradientSectionCard(gradients, "Repeat") {
            DayOfWeekSelector(selectedDays) { day, isSelected ->
                selectedDays = if (isSelected) selectedDays + day else selectedDays - day
            }
        }

        // Snooze
        GradientSectionCard(gradients, "Snooze Duration") {
            val snoozeOptions = listOf("5", "10", "15", "Custom")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(snoozeOptions) { opt ->
                    ElevatedFilterChip(
                        selected = selectedSnooze == opt,
                        onClick = { selectedSnooze = opt },
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        label = { Text(if (opt == "Custom") "Custom" else "$opt min") }
                    )
                }
            }
            if (selectedSnooze == "Custom") {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = customSnooze,
                    onValueChange = { customSnooze = it.filter(Char::isDigit) },
                    label = { Text("Custom Snooze (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(200.dp),
                    singleLine = true
                )
            }
        }

        // Ringtone
        GradientSectionCard(gradients, "Ringtone") {
            OutlinedTextField(
                value = ringtoneTitle,
                onValueChange = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_TYPE,
                                RingtoneManager.TYPE_ALARM
                            )
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            ringtoneUri?.let {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
                            }
                        }
                        ringtonePickerLauncher.launch(intent)
                    },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true
            )
        }

        Spacer(Modifier.height(20.dp))

        // Save button
        Button(onClick = {
            val snooze = (if (selectedSnooze == "Custom") customSnooze.toIntOrNull() else selectedSnooze.toIntOrNull())
                ?: 10
            onSave(hour, minute, label, snooze, ringtoneUri, selectedDays)
        }) {
            Text(if (isNewAlarm) "Create Alarm" else "Save Changes")
        }
    }
}

