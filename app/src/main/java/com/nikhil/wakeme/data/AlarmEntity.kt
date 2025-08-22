package com.nikhil.wakeme.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    var ringTime: Calendar, // stored via converter
    var timezoneId: String = TimeZone.getDefault().id,

    var enabled: Boolean = true,
    var label: String? = null,

    // Snooze
    var snoozeDuration: Int = 10,
    var snoozeCount: Int = 0,

    // Recurrence
    var daysOfWeekMask: Int = 0, // bitmask: Sunday=1<<0 ... Saturday=1<<6

    // Sound & Vibration
    var ringtoneUri: String? = null,
    var ringtoneTitle: String? = null,
    var volume: Float = 1f,
    var vibration: Boolean = true,

    // State
    var upcomingShown: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var nextTriggerAt: Long? = null,
    var missedCount: Int = 0
)
