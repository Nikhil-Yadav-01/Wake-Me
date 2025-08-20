package com.nikhil.wakeme.data

import android.net.Uri
import java.util.Calendar
import androidx.core.net.toUri

data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val label: String?,
    val enabled: Boolean,
    val snoozeDuration: Int,
    val daysOfWeek: Set<Int>,
    val ringtoneUri: Uri?,
    val ringtoneTitle: String,
    val originalHour: Int,
    val originalMinute: Int,
    val createdAt: Long,
    val upcomingShown: Boolean
)

fun AlarmEntity.toAlarm(): Alarm {
    val calendar = Calendar.getInstance().apply { timeInMillis = ringTime }
    val actualHour = calendar.get(Calendar.HOUR_OF_DAY)
    val actualMinute = calendar.get(Calendar.MINUTE)

    return Alarm(
        id = id,
        hour = actualHour,
        minute = actualMinute,
        label = label,
        enabled = enabled,
        snoozeDuration = snoozeDuration,
        daysOfWeek = daysOfWeek,
        ringtoneUri = ringtoneUri?.toUri(),
        ringtoneTitle = ringtoneTitle,
        originalHour = originalHour ?: -1,
        originalMinute = originalMinute ?: -1,
        createdAt = createdAt,
        upcomingShown = upcomingShown
    )
}

fun Alarm.toAlarmEntity(): AlarmEntity {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

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
        ringtoneTitle = ringtoneTitle,
        originalHour = hour,
        originalMinute = minute,
        createdAt = createdAt,
        upcomingShown = upcomingShown
    )
}
