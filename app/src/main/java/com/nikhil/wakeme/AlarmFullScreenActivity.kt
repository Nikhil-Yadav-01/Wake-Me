package com.nikhil.wakeme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlin.random.Random

class AlarmFullScreenActivity : ComponentActivity() {
    private val viewModel: AlarmFullScreenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        if (alarmId != -1L) {
            viewModel.loadAlarm(alarmId)
        }

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        setContent {
            val alarm by viewModel.alarm.collectAsState()
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.alarm_trigger_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                alarm?.let {
                    FullScreenAlarmUI(
                        label = it.label ?: "Alarm",
                        onStop = {
                            viewModel.stopAlarm()
                            finish()
                        },
                        onSnooze = {
                            viewModel.snoozeAlarm()
                            finish()
                        }
                    )
                }
            }
        }
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
