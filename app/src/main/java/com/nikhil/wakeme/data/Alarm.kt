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

    // Determine base time: originalDateTime for new alarms or nextTriggerAt if still valid
    val baseTime = if (nextTriggerAt <= now.timeInMillis) originalDateTime else nextTriggerAt

    // Extract original hour & minute once to avoid repeated Calendar instances
    val originalCal = Calendar.getInstance(tz).apply { timeInMillis = originalDateTime }
    val originalHour = originalCal.get(Calendar.HOUR_OF_DAY)
    val originalMinute = originalCal.get(Calendar.MINUTE)

    // Start from base time, but reset time to original hour/minute
    val nextTrigger = Calendar.getInstance(tz).apply {
        timeInMillis = baseTime
        set(Calendar.HOUR_OF_DAY, originalHour)
        set(Calendar.MINUTE, originalMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // If calculated time is still in the past, move to next day
    if (nextTrigger.before(now)) {
        nextTrigger.add(Calendar.DAY_OF_YEAR, 1)
    }

    // One-time alarm: return this calculated next trigger
    if (daysOfWeek.isEmpty()) {
        return nextTrigger
    }

    // Recurring alarm: find the next valid day from the set
    for (i in 0 until 7) {  // Check for next 7 days max
        val dayOfWeek = nextTrigger.get(Calendar.DAY_OF_WEEK) // 1=Sun ... 7=Sat
        if (daysOfWeek.contains(dayOfWeek) && nextTrigger.after(now)) {
            return nextTrigger
        }
        nextTrigger.add(Calendar.DAY_OF_YEAR, 1)
        nextTrigger.set(Calendar.HOUR_OF_DAY, originalHour)
        nextTrigger.set(Calendar.MINUTE, originalMinute)
        nextTrigger.set(Calendar.SECOND, 0)
        nextTrigger.set(Calendar.MILLISECOND, 0)
    }

    // Fallback: return the calculated time (shouldn't normally hit this)
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
