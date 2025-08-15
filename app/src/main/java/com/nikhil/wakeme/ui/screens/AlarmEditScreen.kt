package com.nikhil.wakeme.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nikhil.wakeme.R
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.data.AlarmRepository
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(navController: NavController, alarmId: Long) {
    val context = LocalContext.current
    val repo = remember { AlarmRepository(context) }
    val scope = rememberCoroutineScope()
    val isNewAlarm = alarmId == 0L

    var alarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var label by remember { mutableStateOf("") }
    var snoozeDuration by remember { mutableStateOf(10f) }
    val timePickerState = rememberTimePickerState()

    LaunchedEffect(alarmId) {
        if (!isNewAlarm) {
            alarm = repo.getById(alarmId)
            label = alarm?.label ?: ""
            snoozeDuration = alarm?.snoozeDuration?.toFloat() ?: 10f
            alarm?.let {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timeMillis }
                timePickerState.hour = cal.get(Calendar.HOUR_OF_DAY)
                timePickerState.minute = cal.get(Calendar.MINUTE)
            }
        }
    }

    fun saveAlarm() {
        if (!AlarmScheduler.canScheduleExactAlarms(context)) {
            // Show rationale and request permission
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

        scope.launch {
            val alarmToSave = alarm?.copy(
                timeMillis = cal.timeInMillis,
                label = label,
                snoozeDuration = snoozeDuration.toInt(),
                enabled = true
            ) ?: AlarmEntity(
                timeMillis = cal.timeInMillis,
                label = label,
                snoozeDuration = snoozeDuration.toInt(),
                enabled = true
            )

            val id = if (isNewAlarm) repo.insert(alarmToSave) else {
                repo.update(alarmToSave)
                alarmToSave.id
            }

            val finalAlarm = alarmToSave.copy(id = id)
            AlarmScheduler.scheduleAlarm(context, finalAlarm)
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewAlarm) "New Alarm" else "Edit Alarm") },
                actions = {
                    TextButton(onClick = { saveAlarm() }) {
                        Text("Save")
                    }
                }
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent // Make Scaffold background transparent
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
            }
        }
    }
}
