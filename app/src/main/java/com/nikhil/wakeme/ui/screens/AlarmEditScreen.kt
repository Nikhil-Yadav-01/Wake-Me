package com.nikhil.wakeme.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikhil.wakeme.R
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.util.Resource
import com.nikhil.wakeme.viewmodels.AlarmEditViewModel
import kotlinx.coroutines.flow.filter
import java.util.Calendar
import kotlin.math.abs

// --- Gradient helpers ---
fun gradientBrush(colors: List<Color>) = Brush.linearGradient(
    colors = colors
)

// --- Alternating gradients for polish ---
private val sectionGradients = listOf(
    listOf(Color(0xFF6A11CB), Color(0xFF2575FC)), // Purple → Blue
    listOf(Color(0xFFFF5F6D), Color(0xFFFFC371)), // Red → Orange
    listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)), // Blue → Green
    listOf(Color(0xFFEE9CA7), Color(0xFFFFDDE1)),  // Pink → Light
    listOf(Color(0xFF00E5FF), Color(0xFF9D00FF))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    goBack: () -> Unit, alarmId: Long, viewModel: AlarmEditViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(alarmId) { viewModel.loadAlarm(alarmId) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Image(
            painter = painterResource(id = R.drawable.set_alarm_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        when (val resource = uiState) {
            is Resource.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Success -> AlarmEditContent(
                alarm = resource.data,
                alarmId = alarmId,
                onSave = { hour, minute, label, snooze, ringtoneUri, days ->
                    viewModel.saveAlarm(alarmId, hour, minute, label, snooze, ringtoneUri, days)
                    goBack()
                },
                onDeleteAlarm = {
                    viewModel.deleteAlarm(it)
                    goBack()
                }
            )

            is Resource.Error -> {
                Toast.makeText(
                    LocalContext.current, resource.message, Toast.LENGTH_SHORT
                ).show()
                goBack()
            }

            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditContent(
    alarm: Alarm?,
    alarmId: Long,
    onSave: (Int, Int, String?, Int, Uri?, Set<Int>) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit
) {
    val context = LocalContext.current
    val isNewAlarm = alarmId == 0L

    val calendar = remember { Calendar.getInstance() }
    val initialHour = alarm?.hour ?: calendar.get(Calendar.HOUR_OF_DAY)
    val initialMinute = alarm?.minute ?: calendar.get(Calendar.MINUTE)

    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    var label by remember { mutableStateOf(alarm?.label) }
    var selectedSnoozeOption by remember {
        mutableStateOf(
            if (alarm?.snoozeDuration in listOf(
                    5, 10, 15
                )
            ) alarm?.snoozeDuration.toString() else "Custom"
        )
    }
    var customSnoozeValue by remember { mutableStateOf(alarm?.snoozeDuration?.toString() ?: "10") }

    var selectedRingtoneUri by remember { mutableStateOf(alarm?.ringtoneUri) }
    var ringtoneTitle by remember {
        mutableStateOf(selectedRingtoneUri?.let {
            RingtoneManager.getRingtone(context, it)?.getTitle(context)
        } ?: "Default Ringtone")
    }

    var isWheelPickerVisible by remember { mutableStateOf(true) }
    var selectedDays by remember { mutableStateOf(alarm?.daysOfWeek ?: emptySet()) }

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
            selectedRingtoneUri = uri
            ringtoneTitle = uri?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context) }
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
                        (if (selectedSnoozeOption == "Custom") customSnoozeValue.toIntOrNull() else selectedSnoozeOption.toIntOrNull())
                            ?: 10
                    onSave(
                        hour, minute, label, snoozeMinutes, selectedRingtoneUri, selectedDays
                    )
                }) { Text("Save") }
                if (!isNewAlarm && alarm != null) {
                    IconButton(onClick = { onDeleteAlarm(alarm) }) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = "Delete Alarm",
                            tint = Color.Red
                        )
                    }
                }
            })
        }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TimePicker section
            GradientSectionCard(title = null, colors = sectionGradients[4]) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.animation.AnimatedVisibility(visible = !isWheelPickerVisible) {
                        val timePickerState = rememberTimePickerState(hour, minute, true)
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
                            onMinuteChange = { minute = it })
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    onClick = { isWheelPickerVisible = !isWheelPickerVisible } // clickable Card
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.switch_picker),
                        contentDescription = "Swap Picker",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(width = 80.dp, height = 40.dp),
                    )
                }
            }

            // Alarm Name
            GradientSectionCard(sectionGradients[0], title = null) {
                OutlinedTextField(
                    value = label ?: "",
                    onValueChange = { label = it },
                    label = { Text("Alarm Name") },
                    modifier = Modifier
                        .fillMaxWidth(),
                    singleLine = true
                )
            }

            // Repeat section
            GradientSectionCard(colors = sectionGradients[1], title = "Repeat") {
                DayOfWeekSelector(selectedDays = selectedDays, onDaySelected = { day, isSelected ->
                    selectedDays = if (isSelected) selectedDays + day else selectedDays - day
                })
            }

            // Snooze section
            GradientSectionCard(colors = sectionGradients[2], title = "Snooze Duration") {
                val snoozeOptions = listOf("5", "10", "15", "Custom")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(snoozeOptions) { duration ->
                        FilterChip(
                            selected = selectedSnoozeOption == duration,
                            onClick = { selectedSnoozeOption = duration },
                            label = { Text(if (duration == "Custom") "Custom" else "$duration min") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
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
            }

            // Ringtone section
            GradientSectionCard(colors = sectionGradients[3], title = "Ringtone") {
                OutlinedTextField(
                    value = ringtoneTitle,
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
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM
                                )
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone"
                                )
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                selectedRingtoneUri?.let {
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it
                                    )
                                }
                            }
                            ringtonePickerLauncher.launch(intent)
                        },
                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface),
                    singleLine = true
                )
            }
        }
    }
}

// --- Gradient Section Card ---
@Composable
fun GradientSectionCard(
    colors: List<Color>, title: String? = null, content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(2.dp, gradientBrush(colors), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .background(gradientBrush(colors), RoundedCornerShape(20.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            title?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
        }
    }
}

@Composable
fun DayOfWeekSelector(selectedDays: Set<Int>, onDaySelected: (Int, Boolean) -> Unit) {
    val days = listOf("S", "M", "T", "W", "T", "F", "S")
    val calendarDays = listOf(
        Calendar.SUNDAY,
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        days.forEachIndexed { index, dayLabel ->
            val calendarDay = calendarDays[index]
            val isSelected = selectedDays.contains(calendarDay)
            ElevatedFilterChip(
                selected = isSelected,
                onClick = { onDaySelected(calendarDay, !isSelected) },
                label = { Text(dayLabel) })
        }
    }
}

// --- Enhanced TimePickerWheel ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimePickerWheel(
    hour: Int, minute: Int, onHourChange: (Int) -> Unit, onMinuteChange: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, gradientBrush(sectionGradients[0]), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberWheel(
                range = 0..23,
                value = hour,
                onValueChange = onHourChange,
                modifier = Modifier.width(100.dp)
            )
            Text(
                ":",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            NumberWheel(
                range = 0..59,
                value = minute,
                onValueChange = onMinuteChange,
                modifier = Modifier.width(100.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberWheel(
    range: IntRange, value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(range, value) {
        if (listState.firstVisibleItemIndex != value) listState.scrollToItem(value)
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.filter { !it }.collect {
            val centerItemInfo =
                listState.layoutInfo.visibleItemsInfo.minByOrNull { abs(it.offset) }
            if (centerItemInfo != null) {
                val newValue = centerItemInfo.index
                if (newValue != value) onValueChange(newValue)
            }
        }
    }

    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.15f))
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 50.dp)
        ) {
            items(range.last - range.first + 1) { index ->
                val number = range.start + index
                val isSelected = number == value
                val textAlpha by animateFloatAsState(if (isSelected) 1f else 0.4f, label = "alpha")
                val textSize by animateDpAsState(if (isSelected) 32.dp else 20.dp, label = "size")

                Text(
                    text = "%02d".format(number),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = with(LocalDensity.current) { textSize.toSp() }),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(40.dp)
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    RoundedCornerShape(12.dp)
                )
        )
    }
}
