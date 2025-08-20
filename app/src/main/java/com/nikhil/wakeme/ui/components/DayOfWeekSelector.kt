package com.nikhil.wakeme.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.util.Calendar

@Composable
fun DayOfWeekSelector(selectedDays: Set<Int>, onDaySelected: (Int, Boolean) -> Unit) {
    Row(
        modifier = Modifier.Companion.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEachIndexed { index, dayLabel ->
            val calendarDay = calendarDays[index]
            val isSelected = selectedDays.contains(calendarDay)
            ElevatedFilterChip(
                selected = isSelected,
                onClick = { onDaySelected(calendarDay, !isSelected) },
                label = { Text(dayLabel) },
                colors = FilterChipDefaults.elevatedFilterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                )
            )
        }
    }
}

val calendarDays = listOf(
    Calendar.SUNDAY,
    Calendar.MONDAY,
    Calendar.TUESDAY,
    Calendar.WEDNESDAY,
    Calendar.THURSDAY,
    Calendar.FRIDAY,
    Calendar.SATURDAY
)
val days = listOf("S", "M", "T", "W", "T", "F", "S")