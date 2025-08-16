package com.nikhil.wakeme.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var timeMillis: Long,
    var enabled: Boolean = true,
    var label: String? = null,
    var snoozeDuration: Int = 10, // in minutes
    var daysOfWeek: Set<Int> = emptySet(), // Calendar.MONDAY, Calendar.TUESDAY, etc.
    var ringtoneUri: String? = null,
    // Store the original hour and minute for recurring alarms.
    // This is set when the alarm is created and should not be updated on snooze.
    var originalHour: Int? = null,
    var originalMinute: Int? = null,
    var createdAt: Long = System.currentTimeMillis()
) {
    fun calculateNextTrigger(): Long {
        val now = Calendar.getInstance()

        // Use the dedicated originalHour and originalMinute for recurring alarms.
        val alarmHour = originalHour ?: Calendar.getInstance().apply { timeInMillis = this@AlarmEntity.timeMillis }.get(Calendar.HOUR_OF_DAY)
        val alarmMinute = originalMinute ?: Calendar.getInstance().apply { timeInMillis = this@AlarmEntity.timeMillis }.get(Calendar.MINUTE)

        if (daysOfWeek.isEmpty()) {
            // For one-time alarms, after they fire, they should be disabled.
            // No next trigger calculation is needed. Return the current time to fulfill the function's
            // contract, but the AlarmWorker is responsible for disabling the alarm.
            return timeMillis
        }

        // For recurring alarms, find the next valid day.
        val nextTriggerCal = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, alarmHour)
            set(Calendar.MINUTE, alarmMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the calculated time for today has already passed, start checking from tomorrow.
        if (nextTriggerCal.before(now)) {
            nextTriggerCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Loop for a maximum of 7 days to find the next scheduled day.
        for (i in 0..7) {
            val dayOfWeek = nextTriggerCal.get(Calendar.DAY_OF_WEEK)
            if (daysOfWeek.contains(dayOfWeek)) {
                // We've found the next valid day and time.
                return nextTriggerCal.timeInMillis
            }
            // Move to the next day and check again.
            nextTriggerCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Fallback: This should be unreachable if daysOfWeek is not empty.
        // It returns the original alarm time set for the next day as a safeguard.
        val fallbackCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarmHour)
            set(Calendar.MINUTE, alarmMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return fallbackCal.timeInMillis
    }
}
