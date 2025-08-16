package com.nikhil.wakeme.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nikhil.wakeme.R
import com.nikhil.wakeme.alarms.AlarmScheduler
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.util.Resource
import com.nikhil.wakeme.viewmodels.AlarmListViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    nav: NavController,
    viewModel: AlarmListViewModel = viewModel()
) {
    val context = LocalContext.current
    val repo = remember { AlarmRepository(context) }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    var hasExactAlarmPermission by remember { mutableStateOf(AlarmScheduler.canScheduleExactAlarms(context)) }

    fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Wake Me", style = MaterialTheme.typography.titleLarge) })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { nav.navigate("edit/0") },
                modifier = Modifier.padding(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.bg),
                        contentDescription = "Home background",
                        contentScale = ContentScale.FillBounds,
                    )
                    Icon(Icons.Default.Add, contentDescription = "Add Alarm")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.home_bg),
                contentDescription = "Home background",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (hasExactAlarmPermission) {
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
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(resource.data, key = { it.id }) { alarm ->
                                    AlarmItem(
                                        alarm = alarm,
                                        onToggle = { enabled ->
                                            val updatedAlarm = alarm.copy(enabled = enabled)
                                            scope.launch {
                                                repo.update(updatedAlarm)
                                                if (enabled) {
                                                    AlarmScheduler.scheduleAlarm(
                                                        context,
                                                        updatedAlarm
                                                    )
                                                } else {
                                                    AlarmScheduler.cancelAlarm(context, alarm.id)
                                                }
                                            }
                                        },
                                        onClick = {
                                            nav.navigate("edit/${alarm.id}")
                                        }
                                    )
                                }
                            }
                        }

                        is Resource.Empty -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No alarms set.",
                                    style = MaterialTheme.typography.headlineLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        is Resource.Error -> {
                            Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
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
                                Text("Grant Permission", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
