package com.nikhil.wakeme.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var ringTime: Long,
    var enabled: Boolean = true,
    var label: String? = null,
    var snoozeDuration: Int = 10, // in minutes
    var daysOfWeek: Set<Int> = emptySet(), // Calendar.MONDAY, Calendar.TUESDAY, etc.
    var ringtoneUri: String? = null,
    var originalHour: Int? = null,
    var originalMinute: Int? = null,
    var upcomingShown: Boolean = false,
    var createdAt: Long = System.currentTimeMillis()
) {
    fun calculateNextTrigger(): Long {
        val now = Calendar.getInstance()

        val alarmHour = originalHour
            ?: Calendar.getInstance().apply { timeInMillis = this@AlarmEntity.ringTime }
                .get(Calendar.HOUR_OF_DAY)
        val alarmMinute = originalMinute
            ?: Calendar.getInstance().apply { timeInMillis = this@AlarmEntity.ringTime }
                .get(Calendar.MINUTE)

        if (daysOfWeek.isEmpty()) {
            // One-time alarm: no new trigger after firing.
            return ringTime
        }

        val nextTriggerCal = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, alarmHour)
            set(Calendar.MINUTE, alarmMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (nextTriggerCal.before(now)) {
            nextTriggerCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        for (i in 0..7) {
            val dayOfWeek = nextTriggerCal.get(Calendar.DAY_OF_WEEK)
            if (daysOfWeek.contains(dayOfWeek)) {
                return nextTriggerCal.timeInMillis
            }
            nextTriggerCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Fallback
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
