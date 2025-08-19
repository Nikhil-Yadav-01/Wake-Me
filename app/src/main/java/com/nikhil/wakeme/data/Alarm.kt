package com.nikhil.wakeme.data

import android.net.Uri
import java.util.Calendar

data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String?,
    val enabled: Boolean,
    val snoozeDuration: Int,
    val daysOfWeek: Set<Int>,
    val ringtoneUri: Uri?,
    val originalHour: Int,
    val originalMinute: Int,
    val createdAt: Long
)

fun AlarmEntity.toAlarm(): Alarm {
    val calendar = Calendar.getInstance().apply { timeInMillis = ringTime }
    val actualHour = originalHour ?: calendar.get(Calendar.HOUR_OF_DAY)
    val actualMinute = originalMinute ?: calendar.get(Calendar.MINUTE)

    return Alarm(
        id = id,
        hour = actualHour,
        minute = actualMinute,
        label = label,
        enabled = enabled,
        snoozeDuration = snoozeDuration,
        daysOfWeek = daysOfWeek,
        ringtoneUri = ringtoneUri?.let { Uri.parse(it) },
        originalHour = originalHour ?: -1, // Provide a default if null
        originalMinute = originalMinute ?: -1, // Provide a default if null
        createdAt = createdAt
    )
}

fun Alarm.toAlarmEntity(): AlarmEntity {
    // When converting back to AlarmEntity, we use the 'hour' and 'minute'
    // from the Alarm object directly as 'originalHour' and 'originalMinute'.
    // The 'ringTime' will be calculated based on these values.
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // If the alarm is for a past time today, set it for tomorrow.
    if (calendar.before(Calendar.getInstance()) && daysOfWeek.isEmpty()) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    return AlarmEntity(
        id = id,
        ringTime = calendar.timeInMillis,
        enabled = enabled,
        label = label,
        snoozeDuration = snoozeDuration,
        daysOfWeek = daysOfWeek,
        ringtoneUri = ringtoneUri?.toString(),
        originalHour = hour,
        originalMinute = minute,
        createdAt = createdAt
    )
}
