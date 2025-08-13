package com.nikhil.wakeme.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class AlarmRepository(context: Context) {
    private val db = AlarmDatabase.getInstance(context)
    private val dao = db.alarmDao()

    fun getAllFlow(): Flow<List<AlarmEntity>> = dao.getAllFlow()
    fun getEnabledFlow(): Flow<List<AlarmEntity>> = dao.getEnabledAlarmsFlow()

    suspend fun insert(alarm: AlarmEntity): Long {
        return dao.insert(alarm)
    }

    suspend fun update(alarm: AlarmEntity) {
        dao.update(alarm)
    }

    suspend fun delete(alarm: AlarmEntity) {
        dao.delete(alarm)
    }

    suspend fun getById(id: Long): AlarmEntity? = dao.getById(id)
    suspend fun getEnabledList(): List<AlarmEntity> = dao.getEnabledAlarmsList()
}
