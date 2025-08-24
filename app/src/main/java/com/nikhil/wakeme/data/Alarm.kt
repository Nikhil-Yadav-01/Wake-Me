package com.nikhil.wakeme.data

import android.net.Uri
import androidx.core.net.toUri
import java.util.*

data class Alarm(
    val id: Long = 0L,
    val label: String? = null,
    val enabled: Boolean = true,
    val snoozeDuration: Int = 10,
    val snoozeCount: Int = 0,
    val daysOfWeek: Set<Int> = emptySet(), // Calendar.MONDAY..SUNDAY
    val ringtoneUri: Uri? = null,
    val ringtoneTitle: String? = "Default Ringtone",
    val volume: Float = 1f,
    val vibration: Boolean = true,
    val upcomingShown: Boolean = false,
    val originalDateTime: Long = Calendar.getInstance().timeInMillis,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val nextTriggerAt: Long = Calendar.getInstance().timeInMillis,
    val missedCount: Int = 0,
    val timezoneId: String = TimeZone.getDefault().id
) {
    // Derived properties (computed from `date`)
    val hour: Int
        get() = Calendar.getInstance().apply { timeInMillis = originalDateTime }.get(Calendar.HOUR_OF_DAY)

    val minute: Int
        get() = Calendar.getInstance().apply { timeInMillis = originalDateTime }.get(Calendar.MINUTE)

    val timeFormatted: String
        get() = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

    val daysFormatted: String
        get() = if (daysOfWeek.isEmpty()) "Once"
        else daysOfWeek.sorted().joinToString { dayOfWeekToShortName(it) }

    val isRecurring: Boolean
        get() = daysOfWeek.isNotEmpty()

    fun timeUntilFormatted(): String {
        val diffMillis = nextTriggerAt - System.currentTimeMillis()
        return if (diffMillis > 0) {
            val days = diffMillis / (1000 * 60 * 60 * 24)
            val hours = (diffMillis / (1000 * 60 * 60)) % 24
            val minutes = (diffMillis / (1000 * 60)) % 60
            if (days > 0) "Rings in ${days}d ${hours}h ${minutes}m"
            else "Rings in ${hours}h ${minutes}m"
        } else "Expired"
    }
}

fun Alarm.calculateNextTrigger(): Calendar {
    val tz = TimeZone.getTimeZone(timezoneId)
    val now = Calendar.getInstance(tz)

    // For new alarms or when nextTriggerAt is in the past, calculate from originalDateTime
    val baseTime = if (nextTriggerAt <= now.timeInMillis) {
        originalDateTime
    } else {
        nextTriggerAt
    }

    // Start from the base time
    val nextTrigger = Calendar.getInstance(tz).apply {
        timeInMillis = baseTime
        set(Calendar.HOUR_OF_DAY, Calendar.getInstance(tz).apply { timeInMillis = originalDateTime }.get(Calendar.HOUR_OF_DAY))
        set(Calendar.MINUTE, Calendar.getInstance(tz).apply { timeInMillis = originalDateTime }.get(Calendar.MINUTE))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // If the calculated time is in the past, move to the next day
    if (nextTrigger.before(now)) {
        nextTrigger.add(Calendar.DAY_OF_YEAR, 1)
    }

    // One-time alarm - just return the next day if it's in the past
    if (daysOfWeek.isEmpty()) {
        return nextTrigger
    }

    // Recurring alarm: find the next enabled day
    for (i in 0..7) {
        val dayOfWeek = nextTrigger.get(Calendar.DAY_OF_WEEK) // 1=Sun ... 7=Sat
        if (daysOfWeek.contains(dayOfWeek) && nextTrigger.after(now)) {
            return nextTrigger
        }
        nextTrigger.add(Calendar.DAY_OF_YEAR, 1)
        nextTrigger.set(Calendar.HOUR_OF_DAY, Calendar.getInstance(tz).apply { timeInMillis = originalDateTime }.get(Calendar.HOUR_OF_DAY))
        nextTrigger.set(Calendar.MINUTE, Calendar.getInstance(tz).apply { timeInMillis = originalDateTime }.get(Calendar.MINUTE))
        nextTrigger.set(Calendar.SECOND, 0)
        nextTrigger.set(Calendar.MILLISECOND, 0)
    }

    // Fallback: return the calculated time
    return nextTrigger
}

/**
 * Converts AlarmEntity (DB) -> Alarm (UI)
 */
fun AlarmEntity.toAlarm(): Alarm {
    val daySet = daysOfWeekMaskToSet(daysOfWeekMask)

    return Alarm(
        id = id,
        originalDateTime = ringTime.timeInMillis,
        label = label,
        enabled = enabled,
        snoozeDuration = snoozeDuration,
        snoozeCount = snoozeCount,
        daysOfWeek = daySet,
        ringtoneUri = ringtoneUri?.toUri(),
        ringtoneTitle = ringtoneTitle ?: "Default",
        vibration = vibration,
        upcomingShown = upcomingShown,
        createdAt = createdAt,
        updatedAt = updatedAt,
        nextTriggerAt = nextTriggerAt,
        missedCount = missedCount,
        timezoneId = timezoneId
    )
}

/**
 * Converts Alarm (UI) -> AlarmEntity (DB)
 */
fun Alarm.toAlarmEntity(): AlarmEntity {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone(timezoneId)).apply {
        timeInMillis = originalDateTime
    }

    return AlarmEntity(
        id = id,
        ringTime = calendar,
        timezoneId = timezoneId,
        enabled = enabled,
        label = label,
        snoozeDuration = snoozeDuration,
        snoozeCount = snoozeCount,
        daysOfWeekMask = daysOfWeekSetToMask(daysOfWeek),
        ringtoneUri = ringtoneUri?.toString(),
        ringtoneTitle = ringtoneTitle,
        vibration = vibration,
        upcomingShown = upcomingShown,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis(),
        nextTriggerAt = nextTriggerAt,
        missedCount = missedCount
    )
}

/** Helpers */
fun daysOfWeekSetToMask(days: Set<Int>): Int {
    var mask = 0
    days.forEach { mask = mask or (1 shl ((it - 1) % 7)) }
    return mask
}

fun daysOfWeekMaskToSet(mask: Int): Set<Int> {
    val days = mutableSetOf<Int>()
    for (i in 0..6) {
        if (mask and (1 shl i) != 0) {
            days.add(i + 1) // Calendar.SUNDAY=1 ... Calendar.SATURDAY=7
        }
    }
    return days
}

fun dayOfWeekToShortName(day: Int): String {
    return when (day) {
        Calendar.SUNDAY -> "Sun"
        Calendar.MONDAY -> "Mon"
        Calendar.TUESDAY -> "Tue"
        Calendar.WEDNESDAY -> "Wed"
        Calendar.THURSDAY -> "Thu"
        Calendar.FRIDAY -> "Fri"
        Calendar.SATURDAY -> "Sat"
        else -> ""
    }
}
