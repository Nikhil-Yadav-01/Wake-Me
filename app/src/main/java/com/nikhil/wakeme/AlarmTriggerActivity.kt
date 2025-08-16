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
import com.nikhil.wakeme.util.Resource // Import the Resource class

class AlarmTriggerActivity : ComponentActivity() {
    private val viewModel: AlarmTriggerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alarmId = intent.getLongExtra("ALARM_ID", -1L)
        
        // Load alarm data using the ViewModel
        viewModel.loadAlarm(alarmId)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        setContent {
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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is Resource.Success -> {
                        val alarm = resource.data
                        AlarmTriggerScreen(
                            label = alarm.label ?: "Alarm",
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
                    is Resource.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${resource.message}", modifier = Modifier.padding(16.dp))
                        }
                    }
                    is Resource.Empty -> {
                        // This state is unlikely for a single alarm, but handled for completeness
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Alarm data not available.", modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}
