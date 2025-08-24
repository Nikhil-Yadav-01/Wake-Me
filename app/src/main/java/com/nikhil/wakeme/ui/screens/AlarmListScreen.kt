package com.nikhil.wakeme.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikhil.wakeme.R
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.ui.components.AlarmItem
import com.nikhil.wakeme.viewmodels.AlarmListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onItemClick: (Long) -> Unit, viewModel: AlarmListViewModel = viewModel()
) {
    val context = LocalContext.current
    val repo = remember { AlarmRepository(context) }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasExactAlarmPermission by remember {
        mutableStateOf(
            AlarmScheduler.canScheduleExactAlarms(
                context
            )
        )
    }

    fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Wake Me", style = MaterialTheme.typography.titleLarge) })
    }, floatingActionButton = {
        FloatingActionButton(
            onClick = { onItemClick(0L) }, modifier = Modifier
                .size(64.dp)
                .border(
                    width = 2.dp, brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    ), shape = RoundedCornerShape(percent = 50)
                )
        ) {
            Image(
                painter = painterResource(R.drawable.add_alarm),
                contentDescription = "Home background",
                contentScale = ContentScale.Crop,
            )
        }
    }) { padding ->
        if (hasExactAlarmPermission) {
            AppScreen(
                resource = uiState,
                backgroundResId = R.drawable.alarm_home_bg,
                modifier = Modifier.padding(padding)
            ) { lst ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(lst, key = { it.id }) { alarm ->
                        AlarmItem(
                            alarm = alarm,
                            onToggle = { enabled ->
                                val updatedAlarm = alarm.copy(enabled = enabled)
                                scope.launch {
                                    repo.update(updatedAlarm)
                                    if (enabled) {
                                        AlarmScheduler.scheduleAlarm(context, updatedAlarm)
                                    } else {
                                        AlarmScheduler.cancelAlarm(context, alarm)
                                    }
                                }
                            },
                            onClick = {
                                onItemClick(alarm.id)
                            }
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(R.drawable.alarm_home_bg),
                    contentDescription = "Home background",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To ensure alarms work correctly, please grant the permission to schedule exact alarms.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { requestExactAlarmPermission() }) {
                            Text(
                                "Grant Permission", style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
