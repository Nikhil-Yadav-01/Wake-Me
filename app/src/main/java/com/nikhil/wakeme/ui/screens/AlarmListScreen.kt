package com.nikhil.wakeme.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.alarms.AlarmScheduler
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun AlarmListScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember { AlarmRepository(context) }
    val scope = rememberCoroutineScope()

    var alarms by remember { mutableStateOf<List<AlarmEntity>>(emptyList()) }
    LaunchedEffect(Unit) {
        repo.getAllFlow().collect { list -> alarms = list }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Wake Me") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("edit") }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(modifier = padding(padding).fillMaxSize()) {
            items(alarms) { alarm ->
                AlarmRow(alarm = alarm, onStop = {
                    scope.launch {
                        val repo = AlarmRepository(context)
                        val a = alarm.copy(enabled = false)
                        repo.update(a)
                        AlarmScheduler.cancelAlarm(context, alarm.id)
                    }
                }, onEnable = {
                    scope.launch {
                        val repo = AlarmRepository(context)
                        val a = alarm.copy(enabled = true)
                        repo.update(a)
                        AlarmScheduler.scheduleAlarm(context, a)
                    }
                }, onEdit = {
                    nav.navigate("edit")
                })
            }
        }
    }
}

@Composable
fun AlarmRow(alarm: AlarmEntity, onStop: () -> Unit, onEnable: () -> Unit, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp), elevation = 4.dp) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(alarm.label ?: "Alarm")
                val date = Date(alarm.timeMillis)
                Text(date.toString(), style = MaterialTheme.typography.body2)
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
