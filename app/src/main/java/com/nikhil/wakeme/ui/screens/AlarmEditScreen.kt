package com.nikhil.wakeme.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.alarms.AlarmScheduler
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(navController: NavController, alarmId: Long) {
    val context = LocalContext.current
    val repo = remember { AlarmRepository(context) }
    val scope = rememberCoroutineScope()
    val isNewAlarm = alarmId == 0L

    var alarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var label by remember { mutableStateOf("") }
    val timePickerState = rememberTimePickerState()

    LaunchedEffect(alarmId) {
        if (!isNewAlarm) {
            alarm = repo.getById(alarmId)
            label = alarm?.label ?: ""
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
                enabled = true
            ) ?: AlarmEntity(
                timeMillis = cal.timeInMillis,
                label = label,
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
        }
    ) { padding ->
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
        }
    }
}
