package com.nikhil.wakeme.ui.screens

import android.app.TimePickerDialog
import android.widget.TimePicker
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nikhil.wakeme.data.AlarmEntity
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.alarms.AlarmScheduler
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun AlarmEditScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember { AlarmRepository(context) }
    val scope = rememberCoroutineScope()

    var label by remember { mutableStateOf("Alarm") }
    var snooze by remember { mutableIntStateOf(5) }
    var autoSnoozeMax by remember { mutableIntStateOf(0) } // 0 unlimited

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") })
        Spacer(Modifier.height(8.dp))
        Row {
            Text("Snooze (min):")
            Spacer(Modifier.width(8.dp))
            TextField(value = snooze.toString(), onValueChange = { snooze = it.toIntOrNull()?:5 }, modifier = Modifier.width(80.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text("Auto-snooze max cycles (0 = unlimited):")
            Spacer(Modifier.width(8.dp))
            TextField(value = autoSnoozeMax.toString(), onValueChange = { autoSnoozeMax = it.toIntOrNull()?:0 }, modifier = Modifier.width(80.dp))
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            // pick time and create alarm
            val cal = Calendar.getInstance()
            val tp = TimePickerDialog(context, { _: TimePicker, h: Int, m: Int ->
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                cal.set(Calendar.SECOND, 0)
                val millis = cal.timeInMillis
                scope.launch {
                    val a = AlarmEntity(timeMillis = millis, label = label, snoozeMinutes = snooze, autoSnoozeMaxCycles = autoSnoozeMax)
                    val id = repo.insert(a)
                    val saved = a.copy(id = id)
                    AlarmScheduler.scheduleAlarm(context, saved)
                    nav.navigateUp()
                }
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
            tp.show()
        }) {
            Text("Set Time")
        }
    }
}
