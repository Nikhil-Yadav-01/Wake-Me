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
    var createdAt: Long = System.currentTimeMillis()
) {
    fun calculateNextTrigger(): Long {
        val now = Calendar.getInstance()
        val alarmTime = Calendar.getInstance().apply {
            timeInMillis = this@AlarmEntity.timeMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek.isEmpty()) {
            // One-time alarm
            if (alarmTime.before(now)) {
                alarmTime.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // Recurring alarm
            var foundNext = false
            // Try to find a trigger today or in the near future within a week
            for (i in 0..7) { // Check today and next 6 days
                val candidateTime = Calendar.getInstance().apply {
                    timeInMillis = alarmTime.timeInMillis
                    add(Calendar.DAY_OF_YEAR, i)
                }
                if (daysOfWeek.contains(candidateTime.get(Calendar.DAY_OF_WEEK))) {
                    // If candidate day is today and time has passed, or any future day
                    if (candidateTime.after(now)) {
                        alarmTime.timeInMillis = candidateTime.timeInMillis
                        foundNext = true
                        break
                    } else if (i == 0 && !alarmTime.before(now)) {
                        // If today and alarm time is now or in future, use it
                        alarmTime.timeInMillis = candidateTime.timeInMillis
                        foundNext = true
                        break
                    }
                }
            }
            if (!foundNext) {
                // If no trigger found in the next 7 days, this should ideally not happen
                // but as a fallback, advance to next day from current alarmTime if it's in the past
                if (alarmTime.before(now)) {
                    alarmTime.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }
        return alarmTime.timeInMillis
    }
}
