package com.nikhil.wakeme.ui.screens

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.alarms.AlarmScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlarmListScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember { AlarmRepository(context) }
    val scope = rememberCoroutineScope()
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    var alarms by remember { mutableStateOf<List<AlarmEntity>>(emptyList()) }
    LaunchedEffect(Unit) {
        repo.getAllFlow().collect { list -> alarms = list }
    }
    var showPermissionRationale by remember { mutableStateOf(false) }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Permission Required") },
            text = { Text("To ensure alarms are delivered on time, please grant the 'Alarms & reminders' permission.") },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                    showPermissionRationale = false
                }) {
                    Text("Grant")
                }
            },
            dismissButton = {
                Button(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Wake Me") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("edit") }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(alarms) { alarm ->
                AlarmRow(alarm = alarm, onStop = {
                    scope.launch {
                        val repo = AlarmRepository(context)
                        val a = alarm.copy(enabled = false)
                        repo.update(a)
                        AlarmScheduler.cancelAlarm(context, alarm.id)
                    }
                }, onEnable = {
                    if (canScheduleExactAlarms()) {
                        scope.launch {
                            val a = alarm.copy(enabled = true)
                            repo.update(a)
                            AlarmScheduler.scheduleAlarm(context, a)
                        }
                    } else {
                        showPermissionRationale = true
                    }
                }, onEdit = {
                    nav.navigate("edit/${alarm.id}")
                })
            }
        }
    }
}

@Composable
fun AlarmRow(alarm: AlarmEntity, onStop: () -> Unit, onEnable: () -> Unit, onEdit: () -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(alarm.label ?: "Alarm")
                val sdf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                Text(sdf.format(Date(alarm.timeMillis)), style = MaterialTheme.typography.bodyLarge)
            }
            Row {
                Button(onClick = onEdit) { Text("Edit") }
                Spacer(Modifier.width(8.dp))
                if (alarm.enabled) {
                    Button(onClick = onStop) { Text("Disable") }
                } else {
                    Button(onClick = onEnable) { Text("Enable") }
                }
            }
        }
    }
}
