package com.nikhil.wakeme.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nikhil.wakeme.R
import com.nikhil.wakeme.data.AlarmEntity
import java.util.Calendar
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    navController: NavController, alarmId: Long, viewModel: AlarmEditViewModel = viewModel()
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

        when (val state = uiState) {
            is AlarmEditUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is AlarmEditUiState.Success -> {
                AlarmEditContent(
                    alarm = state.alarm,
                    alarmId = alarmId,
                    onSave = { hour, minute, label, snooze, ringtoneUri, days ->
                        viewModel.saveAlarm(alarmId, hour, minute, label, snooze, ringtoneUri, days)
                        navController.popBackStack()
                    })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditContent(
    alarm: AlarmEntity?, alarmId: Long, onSave: (Int, Int, String, Int, Uri?, Set<Int>) -> Unit
) {
    val context = LocalContext.current
    val isNewAlarm = alarmId == 0L

    val calendar = Calendar.getInstance()
    val initialHour = alarm?.originalHour ?: calendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = alarm?.originalMinute ?: calendar.get(Calendar.MINUTE)

    val timePickerState =
        rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute)

    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var selectedSnoozeOption by remember {
        mutableStateOf(
            if (alarm?.snoozeDuration in listOf(5, 10, 15))
                alarm?.snoozeDuration.toString()
            else "Custom"
        )
    }
    var customSnoozeValue by remember { mutableStateOf(alarm?.snoozeDuration?.toString() ?: "10") }
    var selectedRingtoneUri by remember { mutableStateOf(alarm?.ringtoneUri?.toUri()) }
    var ringtoneTitle by remember {
        mutableStateOf(selectedRingtoneUri?.let {
            RingtoneManager.getRingtone(context, it)?.getTitle(context)
        } ?: "Default Ringtone")
    }

    var isWheelPickerVisible by remember { mutableStateOf(false) }

    val ringtonePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra(
                        RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                        Uri::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
                selectedRingtoneUri = uri
                ringtoneTitle =
                    uri?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context) }
                        ?: "Default Ringtone"
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    if (isNewAlarm) "New Alarm" else "Edit Alarm",
                    style = MaterialTheme.typography.titleLarge
                )
            }, actions = {
                TextButton(onClick = {
                    val snoozeMinutes =
                        (if (selectedSnoozeOption == "Custom") customSnoozeValue.toIntOrNull()
                        else selectedSnoozeOption.toIntOrNull()) ?: 10
                    onSave(
                        timePickerState.hour,
                        timePickerState.minute,
                        label,
                        snoozeMinutes,
                        selectedRingtoneUri,
                        emptySet()
                    )
                }) {
                    Text("Save")
                }
            })
        }, containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                androidx.compose.animation.AnimatedVisibility(visible = !isWheelPickerVisible) {
                    TimePicker(state = timePickerState)
                }
                androidx.compose.animation.AnimatedVisibility(visible = isWheelPickerVisible) {
                    TimePickerWheel(
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                        onHourChange = { timePickerState.hour = it },
                        onMinuteChange = { timePickerState.minute = it }
                    )
                }
            }

            IconButton(onClick = { isWheelPickerVisible = !isWheelPickerVisible }) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Time Picker Style")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Snooze Duration", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))

            val snoozeOptions = listOf("5", "10", "15", "Custom")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(snoozeOptions) { duration ->
                    FilterChip(
                        selected = selectedSnoozeOption == duration,
                        onClick = { selectedSnoozeOption = duration },
                        label = { Text(if (duration == "Custom") "Custom" else "$duration min") })
                }
            }

            if (selectedSnoozeOption == "Custom") {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customSnoozeValue,
                    onValueChange = { customSnoozeValue = it.filter(Char::isDigit) },
                    label = { Text("Custom Snooze (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(200.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        ringtonePickerLauncher.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_TYPE,
                                    RingtoneManager.TYPE_ALARM
                                )
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            }
                        )
                    }
            ) {
                OutlinedTextField(
                    value = ringtoneTitle,
                    onValueChange = {},
                    label = { Text("Ringtone") },
                    enabled = false,
                    leadingIcon = {
                        Icon(Icons.Default.MusicNote, contentDescription = "Ringtone")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimePickerWheel(
    hour: Int, minute: Int, onHourChange: (Int) -> Unit, onMinuteChange: (Int) -> Unit
) {
    val hourState = rememberLazyListState()
    val minuteState = rememberLazyListState()

    LaunchedEffect(hour) { hourState.animateScrollToItem(hour) }
    LaunchedEffect(minute) { minuteState.animateScrollToItem(minute) }

    LaunchedEffect(hourState.isScrollInProgress) {
        if (!hourState.isScrollInProgress) {
            val centerIndex =
                hourState.layoutInfo.visibleItemsInfo.minByOrNull {
                    abs((it.offset + it.size / 2) - hourState.layoutInfo.viewportSize.height / 2)
                }?.index
            centerIndex?.let { onHourChange(it) }
        }
    }
    LaunchedEffect(minuteState.isScrollInProgress) {
        if (!minuteState.isScrollInProgress) {
            val centerIndex =
                minuteState.layoutInfo.visibleItemsInfo.minByOrNull {
                    abs((it.offset + it.size / 2) - minuteState.layoutInfo.viewportSize.height / 2)
                }?.index
            centerIndex?.let { onMinuteChange(it) }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val itemHeight = 50.dp
        val visibleItems = 3
        val listHeight = itemHeight * visibleItems
        val padding = (listHeight - itemHeight) / 2

        LazyColumn(
            state = hourState,
            modifier = Modifier
                .height(listHeight)
                .width(80.dp),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = hourState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(padding)) }
            items(24) { h ->
                Text(
                    text = "%02d".format(h),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier
                        .height(itemHeight)
                        .wrapContentHeight()
                )
            }
            item { Spacer(modifier = Modifier.height(padding)) }
        }

        Text(
            ":",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        LazyColumn(
            state = minuteState,
            modifier = Modifier
                .height(listHeight)
                .width(80.dp),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = minuteState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(padding)) }
            items(60) { m ->
                Text(
                    text = "%02d".format(m),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier
                        .height(itemHeight)
                        .wrapContentHeight()
                )
            }
            item { Spacer(modifier = Modifier.height(padding)) }
        }
    }
}
