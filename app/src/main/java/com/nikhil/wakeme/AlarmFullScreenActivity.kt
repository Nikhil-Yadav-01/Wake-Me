package com.nikhil.wakeme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikhil.wakeme.alarms.AlarmActionReceiver
import com.nikhil.wakeme.alarms.AlarmScheduler
import android.content.Intent
import kotlin.random.Random

class AlarmFullScreenActivity : ComponentActivity() {
    private var alarmId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        setContent {
            FullScreenAlarmUI(
                label = "Alarm",
                onStop = { stopAlarm() },
                onSnooze = { snoozeAlarm() }
            )
        }
    }

    private fun stopAlarm() {
        val stopIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_STOP // Corrected line
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(stopIntent)
        finish()
    }

    private fun snoozeAlarm() {
        val snoozeIntent = Intent(this, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_SNOOZE // Corrected line
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(snoozeIntent)
        finish()
    }

}

@Composable
fun FullScreenAlarmUI(label: String, onStop: () -> Unit, onSnooze: () -> Unit) {
    val a = remember { Random.nextInt(1, 12) }
    val b = remember { Random.nextInt(1, 12) }
    val answer = a * b
    var input by remember { mutableStateOf("") }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Solve to stop: $a × $b")
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Answer") })
            Spacer(Modifier.height(12.dp))
            Row {
                Button(onClick = {
                    if (input.toIntOrNull() == answer) onStop()
                }, modifier = Modifier.padding(8.dp)) {
                    Text("Submit")
                }
                Button(onClick = onSnooze, modifier = Modifier.padding(8.dp)) { Text("Snooze") }
            }
        }
    }
}
