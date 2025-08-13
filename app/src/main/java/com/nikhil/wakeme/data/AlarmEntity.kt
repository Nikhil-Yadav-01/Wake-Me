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
    var toneUri: String? = null,
    var vibrate: Boolean = true,
    var repeatMask: Int = 0,
    var snoozeMinutes: Int = 5,
    var autoSnoozeEnabled: Boolean = true,
    var autoSnoozeMaxCycles: Int = 0, // 0 = unlimited
    var autoSnoozeCount: Int = 0,
    var createdAt: Long = System.currentTimeMillis()
) {
    fun calculateNextTrigger(): Long {
        val now = Calendar.getInstance()
        val alarmTime = Calendar.getInstance().apply { timeInMillis = this@AlarmEntity.timeMillis }

        if (repeatMask == 0) { // Not repeating
            if (alarmTime.before(now)) {
                // If it's a one-time alarm in the past, it shouldn't be rescheduled.
                // However, boot receiver logic might want to reschedule for the next day.
                // Let's stick to the principle that one-time past alarms don't re-fire.
                return timeMillis 
            }
            return timeMillis
        }

        // For repeating alarms
        val days = booleanArrayOf(
            (repeatMask and MONDAY) > 0,
            (repeatMask and TUESDAY) > 0,
            (repeatMask and WEDNESDAY) > 0,
            (repeatMask and THURSDAY) > 0,
            (repeatMask and FRIDAY) > 0,
            (repeatMask and SATURDAY) > 0,
            (repeatMask and SUNDAY) > 0
        )

        // Set alarm time to today
        alarmTime.set(Calendar.YEAR, now.get(Calendar.YEAR))
        alarmTime.set(Calendar.MONTH, now.get(Calendar.MONTH))
        alarmTime.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR))

        // Find the next day it should ring
        for (i in 0..7) {
            val dayOfWeek = (now.get(Calendar.DAY_OF_WEEK) + i -1) % 7 // Calendar.SUNDAY is 1
            if (days[dayOfWeek] && (alarmTime.after(now) || i > 0)) { // if it's today, make sure time hasn't passed
                return alarmTime.timeInMillis
            }
            alarmTime.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return timeMillis // fallback
    }

    companion object {
        const val MONDAY = 1
        const val TUESDAY = 2
        const val WEDNESDAY = 4
        const val THURSDAY = 8
        const val FRIDAY = 16
        const val SATURDAY = 32
        const val SUNDAY = 64
    }
}
