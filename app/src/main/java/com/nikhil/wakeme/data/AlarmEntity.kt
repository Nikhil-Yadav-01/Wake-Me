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
    var createdAt: Long = System.currentTimeMillis()
) {
    fun calculateNextTrigger(): Long {
        val now = Calendar.getInstance()
        val alarmTime = Calendar.getInstance().apply {
            timeInMillis = this@AlarmEntity.timeMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarmTime.before(now)) {
            alarmTime.add(Calendar.DAY_OF_YEAR, 1)
        }

        return alarmTime.timeInMillis
    }
}
