package com.nikhil.wakeme.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(context: Context) {
    private val dao = AlarmDatabase.getInstance(context).alarmDao()

    fun getAllFlow(): Flow<List<Alarm>> =
        dao.getAllFlow().map { it.map(AlarmEntity::toAlarm) }

    suspend fun getEnabledAlarmsList(): List<Alarm> =
        dao.getEnabledAlarmsList().map(AlarmEntity::toAlarm)

    suspend fun insert(alarm: Alarm): Long = dao.insert(alarm.toAlarmEntity())

    suspend fun update(alarm: Alarm): Int = dao.update(alarm.toAlarmEntity())

    suspend fun delete(alarm: Alarm): Int = dao.delete(alarm.toAlarmEntity())

    suspend fun getById(id: Long): Alarm? = dao.getById(id)?.toAlarm()
}
