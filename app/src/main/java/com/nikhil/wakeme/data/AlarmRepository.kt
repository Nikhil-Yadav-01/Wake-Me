package com.nikhil.wakeme.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AlarmRepository(context: Context) {
    private val db = AlarmDatabase.getInstance(context)
    private val dao = db.alarmDao()

    fun getAllFlow(): Flow<List<Alarm>> = dao.getAllFlow().map { it.map(AlarmEntity::toAlarm) }
    fun getEnabledFlow(): Flow<List<Alarm>> = dao.getEnabledAlarmsFlow().map { it.map(AlarmEntity::toAlarm) }

    suspend fun insert(alarm: AlarmEntity): Long {
        return dao.insert(alarm)
    }

    suspend fun update(alarm: AlarmEntity) {
        dao.update(alarm)
    }

    suspend fun delete(alarm: AlarmEntity) {
        dao.delete(alarm)
    }

    suspend fun getById(id: Long): Alarm? = dao.getById(id)?.toAlarm()
    suspend fun getEnabledList(): List<Alarm> = dao.getEnabledAlarmsList().map(AlarmEntity::toAlarm)
}
