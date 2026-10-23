package com.sglenn.dosepulse.alarm

import android.app.NotificationManager
import android.content.Context
import com.sglenn.dosepulse.data.MedicationRepository

/** Shared dose actions for the dashboard, lock-screen activity, notification, and alarm timeout. */
object DoseActionHandler {
    suspend fun take(context: Context, repository: MedicationRepository, id: Long, date: String, time: String) {
        repository.markTaken(id, date, time)
        silenceDose(context, id, date, time)
    }

    suspend fun snooze(
        context: Context,
        repository: MedicationRepository,
        id: Long,
        date: String,
        time: String,
        minutes: Int
    ): Boolean {
        val medication = repository.getMedicationById(id) ?: return false
        repository.markSnoozed(id, date, time, minutes)
        val scheduled = MedicationAlarmScheduler.scheduleSnoozeAlarm(context, medication, date, time, minutes)
        silenceDose(context, id, date, time)
        return scheduled
    }

    suspend fun skip(context: Context, repository: MedicationRepository, id: Long, date: String, time: String, reason: String) {
        repository.markSkipped(id, date, time, reason)
        silenceDose(context, id, date, time)
    }

    suspend fun markMissed(repository: MedicationRepository, id: Long, date: String, time: String) {
        if (id > 0) repository.markMissed(id, date, time)
    }

    fun silence(context: Context) {
        context.stopService(android.content.Intent(context, AlarmSoundService::class.java))
        val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifications.cancel(AlarmSoundService.NOTIFICATION_ID)
    }

    private fun silenceDose(context: Context, id: Long, date: String, time: String) {
        val active = AlarmSoundService.activeAlarmState.value
        if (active.isRinging && active.medicationId == id && active.scheduledDate == date && active.scheduledTime == time) {
            silence(context)
        }
    }
}
