package com.nikhil.wakeme.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nikhil.wakeme.R
import com.nikhil.wakeme.data.Alarm
import com.nikhil.wakeme.ui.theme.AlarmActive
import com.nikhil.wakeme.ui.theme.AlarmIdle

@Composable
fun AlarmItem(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val color = if (alarm.enabled) AlarmActive else AlarmIdle
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 10.dp,
            pressedElevation = 16.dp
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(width = 2.dp, color = color.copy(0.7f)),
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(R.drawable.alarm_bg),
                contentDescription = "Background Image",
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
                    .then(
                        if (alarm.enabled) Modifier.blur(8.dp).alpha(0.7f) else Modifier
                    ),
                contentScale = ContentScale.Crop,
            )

            // Foreground content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alarm.timeFormatted,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        if (!alarm.label.isNullOrEmpty()) {
                            Text(
                                text = alarm.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Switch(checked = alarm.enabled, onCheckedChange = onToggle)
                }

                // Next Trigger and Repeat Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = alarm.timeUntilFormatted(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = alarm.daysFormatted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

