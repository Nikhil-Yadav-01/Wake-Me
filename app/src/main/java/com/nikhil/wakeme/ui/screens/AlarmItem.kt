package com.nikhil.wakeme.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nikhil.wakeme.data.AlarmEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmItem(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onClick: (AlarmEntity) -> Unit
) {

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick(alarm) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box {
            Image(
                painter = painterResource(com.nikhil.wakeme.R.drawable.bg),
                contentDescription = "Home background",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxSize()
                    .height(96.dp)
                    .alpha(0.7f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    Text(
                        text = timeFormat.format(Date(alarm.timeMillis)),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = alarm.label ?: "Alarm",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}
