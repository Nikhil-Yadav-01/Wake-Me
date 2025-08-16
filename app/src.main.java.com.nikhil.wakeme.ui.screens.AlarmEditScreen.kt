package com.nikhil.wakeme.ui.screens

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nikhil.wakeme.R
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.data.AlarmEntity
import java.util.*

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

    when (val state = uiState) {
        is AlarmEditUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AlarmEditUiState.Success -> {
            AlarmEditContent(
                alarm = state.alarm,
                alarmId = alarmId,
                onSave = { timeMillis, label, snooze, ringtoneUri ->
                    viewModel.saveAlarm(alarmId, timeMillis, label, snooze, ringtoneUri)
                    navController.popBackStack()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditContent(
    alarm: AlarmEntity?,
    alarmId: Long,
    onSave: (Long, String, Int, Uri?) -> Unit
) {
    val context = LocalContext.current
    val isNewAlarm = alarmId == 0L

    var label by remember { mutableStateOf(alarm?.label ?: "") }
    var snoozeDuration by remember { mutableStateOf(alarm?.snoozeDuration?.toFloat() ?: 10f) }
    var selectedRingtoneUri by remember { mutableStateOf(alarm?.ringtoneUri?.toUri()) }
    var ringtoneTitle by remember {
        mutableStateOf(
            selectedRingtoneUri?.let { RingtoneManager.getRingtone(context, it)?.getTitle(context) } ?: "Default Ringtone"
        )
    }

    val timePickerState = rememberTimePickerState(
        initialHour = alarm?.let {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timeMillis }
            cal.get(Calendar.HOUR_OF_DAY)
        } ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = alarm?.let {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timeMillis }
            cal.get(Calendar.MINUTE)
        } ?: Calendar.getInstance().get(Calendar.MINUTE)
    )

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
            selectedRingtoneUri = uri
            ringtoneTitle = uri?.let {
                RingtoneManager.getRingtone(context, it)?.getTitle(context)
            } ?: "Default Ringtone"
        }
    }

    fun save() {
        if (!AlarmScheduler.canScheduleExactAlarms(context)) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
            return
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
            set(Calendar.MINUTE, timePickerState.minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        onSave(cal.timeInMillis, label, snoozeDuration.toInt(), selectedRingtoneUri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewAlarm) "New Alarm" else "Edit Alarm", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    TextButton(onClick = { save() }) {
                        Text("Save")
                    }
                }
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.set_alarm_bg),
                contentDescription = "Background",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm Label") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                TimePicker(state = timePickerState)
                Spacer(modifier = Modifier.height(24.dp))
                Text("Snooze: ${snoozeDuration.toInt()} minutes")
                Slider(
                    value = snoozeDuration,
                    onValueChange = { snoozeDuration = it },
                    valueRange = 5f..30f,
                    steps = 5
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                selectedRingtoneUri?.let {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
                                }
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
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
