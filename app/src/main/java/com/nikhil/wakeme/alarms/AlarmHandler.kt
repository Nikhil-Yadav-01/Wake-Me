package com.nikhil.wakeme.alarms

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.nikhil.wakeme.data.AlarmRepository
import com.nikhil.wakeme.data.calculateNextTrigger
import com.nikhil.wakeme.util.NotificationHelper

object AlarmHandler {
    /**
     * Central place to perform the alarm trigger flow for both Receiver & Worker.
     * - Cancels any pre-alarm notifications
     * - Starts the foreground AlarmService (required on API 26+)
     * - Reschedules if recurring, or disables if one-shot
     */
    suspend fun handleTrigger(context: Context, alarmId: Long) {
        val repo = AlarmRepository(context)
        val alarm = repo.getById(alarmId) ?: return
        if (!alarm.enabled) return

        // Cancel upcoming if shown, just in case
        NotificationHelper.cancelNotification(context, alarm.id.toInt())

        // Start the foreground service that plays sound + launches full-screen UI
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START
            putExtra("ALARM_ID", alarm.id)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // Update next occurrence or disable if one-shot
        if (alarm.isRecurring) {
            val next = alarm.calculateNextTrigger().timeInMillis
            val updated = alarm.copy(
                updatedAt = System.currentTimeMillis(),
                upcomingShown = false,
                nextTriggerAt = next
            )
            repo.update(updated)
            AlarmScheduler.scheduleAlarm(context, updated)
        } else {
            repo.update(alarm.copy(enabled = false))
        }
    }

    /**
     * Show the T-5 min upcoming notification (idempotent).
     */
    suspend fun handleUpcoming(context: Context, alarmId: Long) {
        val repo = AlarmRepository(context)
        val alarm = repo.getById(alarmId) ?: return
        if (!alarm.enabled) return
        if (!alarm.upcomingShown) {
            NotificationHelper.showUpcomingAlarmNotification(context, alarm)
            repo.update(alarm.copy(upcomingShown = true))
        }
    }
}