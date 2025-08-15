package com.nikhil.wakeme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.nikhil.wakeme.ui.screens.AlarmTriggerScreen

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
                    AlarmTriggerScreen(
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
