package com.nikhil.wakeme.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Insert
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity): Int

    @Delete
    suspend fun delete(alarm: AlarmEntity): Int

    @Query("SELECT * FROM alarms WHERE enabled = 1 ORDER BY ringTime")
    fun getEnabledAlarmsFlow(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY ringTime")
    fun getAllFlow(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getEnabledAlarmsList(): List<AlarmEntity>
}
