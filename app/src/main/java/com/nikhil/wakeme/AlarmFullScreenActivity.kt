package com.nikhil.wakeme

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nikhil.wakeme.ui.theme.Orbitron // Import Orbitron font family
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
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.alarm_trigger_bg),
            contentDescription = "Background",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge, // Using titleLarge
                fontFamily = Orbitron // Explicitly applying font family
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "To stop the alarm, solve this math problem:",
                style = MaterialTheme.typography.bodyLarge, // Using bodyLarge
                fontFamily = Orbitron // Explicitly applying font family
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "$a × $b = ?",
                style = MaterialTheme.typography.headlineLarge, // Using headlineLarge
                fontFamily = Orbitron // Explicitly applying font family
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Enter your answer", fontFamily = Orbitron) } // Applying font to label
            )

            Spacer(Modifier.height(12.dp))

            Row {
                Button(
                    onClick = {
                        if (input.toIntOrNull() == answer) {
                            Toast.makeText(context, "Correct! Alarm stopped.", Toast.LENGTH_SHORT)
                                .show()
                            onStop()
                        } else {
                            Toast.makeText(context, "Wrong answer! Try again.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Submit", fontFamily = Orbitron) // Applying font to button text
                }
                Button(onClick = onSnooze, modifier = Modifier.padding(8.dp)) {
                    Text("Snooze", fontFamily = Orbitron) // Applying font to button text
                }
            }
        }
    }
}
