package com.nikhil.wakeme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nikhil.wakeme.ui.screens.AlarmTriggerScreen
import com.nikhil.wakeme.alarms.AlarmService
import com.nikhil.wakeme.ui.theme.WakeMeTheme
import com.nikhil.wakeme.util.Resource
import com.nikhil.wakeme.viewmodels.AlarmTriggerViewModel
import android.content.Intent
import android.os.CountDownTimer
import androidx.core.content.ContextCompat

class AlarmTriggerActivity : ComponentActivity() {
    private val viewModel: AlarmTriggerViewModel by viewModels()
    private var autoSnoozeTimer: CountDownTimer? = null
    private val AUTO_SNOOZE_DELAY_MILLIS = 5 * 60 * 1000L // 5 minutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alarmId = intent.getLongExtra("ALARM_ID", -1L)

        viewModel.loadAlarm(alarmId)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // Start the auto-snooze timer
        autoSnoozeTimer = object : CountDownTimer(AUTO_SNOOZE_DELAY_MILLIS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Optionally update UI with remaining time
            }

            override fun onFinish() {
                viewModel.snoozeAlarm()
            }
        }.start()

        setContent {
            WakeMeTheme {
                val uiState by viewModel.uiState.collectAsState()
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.alarm_trigger_bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    when (val resource = uiState) {
                        is Resource.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        is Resource.Success -> {
                            val alarm = resource.data
                            AlarmTriggerScreen(
                                label = alarm.label ?: "Alarm",
                                onStop = {
                                    autoSnoozeTimer?.cancel()
                                    viewModel.stopAlarm()
                                    finish()
                                },
                                onSnooze = {
                                    autoSnoozeTimer?.cancel()
                                    viewModel.snoozeAlarm()
                                    finish()
                                }
                            )
                        }

                        is Resource.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Error: ${resource.message}",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        is Resource.Empty -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Alarm data not available.",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        if (alarmId != -1L) {
            val serviceIntent = Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_START
                putExtra("ALARM_ID", alarmId)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }
}
