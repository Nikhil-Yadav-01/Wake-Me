package com.nikhil.wakeme

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nikhil.wakeme.alarms.AlarmService
import com.nikhil.wakeme.alarms.AutoSnoozeReceiver
import com.nikhil.wakeme.ui.screens.AlarmTriggerScreen
import com.nikhil.wakeme.ui.theme.WakeMeTheme
import com.nikhil.wakeme.util.Resource
import com.nikhil.wakeme.viewmodels.AlarmTriggerViewModel

class AlarmTriggerActivity : ComponentActivity() {
    private val viewModel: AlarmTriggerViewModel by viewModels()
    private val AUTO_SNOOZE_DELAY_MILLIS = 5 * 60 * 1000L // 5 minutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alarmId = intent.getLongExtra("ALARM_ID", -1L)

        viewModel.loadAlarm(alarmId)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        scheduleAutoSnooze(alarmId)

        setContent {
            WakeMeTheme {
                val uiState by viewModel.uiState.collectAsState()
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.trigeer_bg),
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
                            AlarmTriggerScreen(label = alarm.label ?: "Alarm", onStop = {
                                cancelAutoSnooze(alarmId)
                                viewModel.stopAlarm()
                                finish()
                            }, onSnooze = {
                                cancelAutoSnooze(alarmId)
                                viewModel.snoozeAlarm()
                                finish()
                            })
                        }

                        is Resource.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Error: ${resource.message}", modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        is Resource.Empty -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Alarm data not available.", modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scheduleAutoSnooze(alarmId: Long) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        val snoozeIntent = Intent(this, AutoSnoozeReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this, alarmId.toInt(), // unique per alarm
            snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + AUTO_SNOOZE_DELAY_MILLIS
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
    }

    private fun cancelAutoSnooze(alarmId: Long) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val snoozeIntent = Intent(this, AutoSnoozeReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            alarmId.toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
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
