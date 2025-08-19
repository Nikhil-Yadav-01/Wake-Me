package com.nikhil.wakeme.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nikhil.wakeme.R
import kotlin.random.Random

@Composable
fun AlarmTriggerScreen(label: String, onStop: () -> Unit, onSnooze: () -> Unit) {
    val a = remember { Random.nextInt(10, 25) }
    val b = remember { Random.nextInt(10, 25) }
    val answer = a * b
    var input by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.alarm_trigger_bg),
            contentDescription = "Background",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label, style = MaterialTheme.typography.titleLarge, color = Color.White
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "To stop the alarm, solve this math problem:",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "$a × $b = ?",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = {
                    Text(
                        "Enter your answer",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = {
                    if (input.toIntOrNull() == answer) {
                        onStop()
                    } else {
                        Toast.makeText(context, "Wrong answer! Try again.", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
            )

            Spacer(Modifier.height(12.dp))

            Row {
                Button(
                    onClick = {
                        if (input.toIntOrNull() == answer) {
                            onStop()
                        } else {
                            Toast.makeText(context, "Wrong answer! Try again.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }, modifier = Modifier.padding(8.dp), colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        "Submit", style = MaterialTheme.typography.bodyLarge
                    )
                }
                Button(
                    onClick = onSnooze,
                    modifier = Modifier.padding(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        "Snooze", style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
