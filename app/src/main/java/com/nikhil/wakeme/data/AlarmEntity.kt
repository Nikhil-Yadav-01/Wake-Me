package com.nikhil.wakeme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

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
)
