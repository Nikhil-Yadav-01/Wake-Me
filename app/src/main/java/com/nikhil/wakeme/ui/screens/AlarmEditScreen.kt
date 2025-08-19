package com.nikhil.wakeme.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.nikhil.wakeme.R
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.util.Resource
import com.nikhil.wakeme.viewmodels.AlarmEditViewModel
import kotlinx.coroutines.flow.filter
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    navController: NavController,
    alarmId: Long,
    viewModel: AlarmEditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(alarmId) {
        viewModel.loadAlarm(alarmId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Image(
            painter = painterResource(id = R.drawable.set_alarm_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        when (val resource = uiState) {
            is Resource.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is Resource.Success -> {
                AlarmEditContent(
                    alarm = resource.data,
                    alarmId = alarmId,
                    onSave = { hour, minute, label, snooze, ringtoneUri, days ->
                        viewModel.saveAlarm(alarmId, hour, minute, label, snooze, ringtoneUri, days)
                        navController.popBackStack()
                    }
                )
            }
            is Resource.Error -> {
                Toast.makeText(LocalContext.current, resource.message, Toast.LENGTH_SHORT).show()
            }
            is Resource.Empty -> {
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditContent(
    alarm: Alarm?,
    alarmId: Long,
    onSave: (Int, Int, String, Int, Uri?, Set<Int>) -> Unit
) {
    val context = LocalContext.current
    val isNewAlarm = alarmId == 0L

    val calendar = Calendar.getInstance()
    val initialHour = alarm?.hour ?: calendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = alarm?.minute ?: calendar.get(Calendar.MINUTE)

    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var selectedSnoozeOption by remember { mutableStateOf(if (alarm?.snoozeDuration in listOf(5, 10, 15)) alarm?.snoozeDuration.toString() else "Custom") }
    var customSnoozeValue by remember { mutableStateOf(alarm?.snoozeDuration?.toString() ?: "10") }

    var selectedRingtoneUri by remember { mutableStateOf(alarm?.ringtoneUri) }
    var ringtoneTitle by remember { mutableStateOf(selectedRingtoneUri?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context) } ?: "Default Ringtone") }

    var isWheelPickerVisible by remember { mutableStateOf(false) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            selectedRingtoneUri = uri
            ringtoneTitle = uri?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context) } ?: "Default Ringtone"
        }
    }

    var selectedDays by remember { mutableStateOf(alarm?.daysOfWeek ?: emptySet()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewAlarm) "New Alarm" else "Edit Alarm", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    TextButton(onClick = {
                        val snoozeMinutes = (if (selectedSnoozeOption == "Custom") customSnoozeValue.toIntOrNull() else selectedSnoozeOption.toIntOrNull()) ?: 10
                        onSave(hour, minute, label, snoozeMinutes, selectedRingtoneUri, selectedDays)
                    }) {
                        Text("Save")
                    }
                }
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedVisibility(visible = !isWheelPickerVisible) {
                    val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
                    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
                        hour = timePickerState.hour
                        minute = timePickerState.minute
                    }
                    TimePicker(state = timePickerState)
                }

                androidx.compose.animation.AnimatedVisibility(visible = isWheelPickerVisible) {
                    TimePickerWheel(
                        hour = hour,
                        minute = minute,
                        onHourChange = { hour = it },
                        onMinuteChange = { minute = it }
                    )
                }
            }

            IconButton(onClick = { isWheelPickerVisible = !isWheelPickerVisible }) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Time Picker Style")
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Alarm Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Repeat", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            DayOfWeekSelector(
                selectedDays = selectedDays,
                onDaySelected = { day, isSelected ->
                    selectedDays = if (isSelected) {
                        selectedDays + day
                    } else {
                        selectedDays - day
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Snooze Duration", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            val snoozeOptions = listOf("5", "10", "15", "Custom")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(snoozeOptions) { duration ->
                    FilterChip(
                        selected = selectedSnoozeOption == duration,
                        onClick = { selectedSnoozeOption = duration },
                        label = { Text(if (duration == "Custom") "Custom" else "$duration min") }
                    )
                }
            }

            if (selectedSnoozeOption == "Custom") {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customSnoozeValue,
                    onValueChange = { customSnoozeValue = it.filter(Char::isDigit) },
                    label = { Text("Custom Snooze (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(200.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            selectedRingtoneUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it) }
                        }
                        ringtonePickerLauncher.launch(intent)
                    }
            ) {
                OutlinedTextField(
                    value = ringtoneTitle,
                    onValueChange = {},
                    label = { Text("Ringtone") },
                    enabled = false,
                    leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = "Ringtone") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun DayOfWeekSelector(
    selectedDays: Set<Int>,
    onDaySelected: (Int, Boolean) -> Unit
) {
    val days = listOf("S", "M", "T", "W", "T", "F", "S")
    val calendarDays = listOf(
        Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEachIndexed { index, dayLabel ->
            val calendarDay = calendarDays[index]
            val isSelected = selectedDays.contains(calendarDay)
            ElevatedFilterChip(
                selected = isSelected,
                onClick = { onDaySelected(calendarDay, !isSelected) },
                label = { Text(dayLabel) }
            )
        }
    }
}

@Composable
fun TimePickerWheel(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NumberWheel(
            range = 0..23,
            value = hour,
            onValueChange = onHourChange,
            modifier = Modifier.width(100.dp)
        )
        Text(":", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 8.dp))
        NumberWheel(
            range = 0..59,
            value = minute,
            onValueChange = onMinuteChange,
            modifier = Modifier.width(100.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberWheel(
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(range, value) {
        if (listState.firstVisibleItemIndex != value) {
            listState.scrollToItem(value)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect {
                val centerItemInfo = listState.layoutInfo.visibleItemsInfo.minByOrNull { abs(it.offset) }
                if (centerItemInfo != null) {
                    val newValue = centerItemInfo.index
                    if (newValue != value) {
                        onValueChange(newValue)
                    }
                }
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier.height(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 40.dp)
    ) {
        items(range.last - range.first + 1) { index ->
            Text(
                text = "%02d".format(range.start + index),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}


@Preview
@Composable
fun EditPreview() {
    AlarmEditScreen(
        navController = rememberNavController(),
        alarmId = 1L,
    )
}
