package com.sglenn.dosepulse.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.sglenn.dosepulse.data.Medication
import com.sglenn.dosepulse.data.ScheduleType
import com.sglenn.dosepulse.receiver.MedicationAlarmReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object MedicationAlarmScheduler {
    const val TEST_MED_ID = 999999L
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return manager.canScheduleExactAlarms()
    }

    fun scheduleTestAlarm(context: Context, delaySeconds: Int = 5): Boolean {
        val triggerTime = System.currentTimeMillis() + delaySeconds * 1000L
        val intent = alarmIntent(
            context, MedicationAlarmReceiver.ACTION_TEST_ALARM, TEST_MED_ID,
            "Test Alarm", "", LocalDate.now().toString(),
            LocalTime.now().format(timeFormat), "Test reminder", 0xFF00796BL
        )
        return scheduleExact(context, triggerTime, PendingIntent.getBroadcast(
            context, TEST_MED_ID.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ))
    }

    /** Keeps one future scheduled dose per medication; called after edits, delivery, and device changes. */
    fun scheduleNextAlarm(context: Context, medication: Medication): Boolean {
        cancelAlarms(context, medication.id)
        if (!medication.isActive || medication.scheduleType == ScheduleType.AS_NEEDED_PRN || medication.scheduledTimes.isEmpty()) return true

        val target = findNextTarget(medication, LocalDateTime.now()) ?: return true
        val intent = alarmIntent(
            context, MedicationAlarmReceiver.ACTION_MEDICATION_ALARM, medication.id,
            medication.name, medication.dosage, target.toLocalDate().toString(),
            target.toLocalTime().format(timeFormat), medication.instructions,
            medication.colorHex, medication.ringtoneUri, isSnooze = false
        )
        val pendingIntent = PendingIntent.getBroadcast(
            context, scheduledRequestCode(medication.id), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return scheduleExact(context, target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), pendingIntent)
    }

    fun scheduleSnoozeAlarm(
        context: Context,
        medication: Medication,
        scheduledDate: String,
        scheduledTime: String,
        snoozeMinutes: Int
    ): Boolean {
        val triggerAt = System.currentTimeMillis() + snoozeMinutes.coerceAtLeast(1) * 60_000L
        val intent = alarmIntent(
            context, MedicationAlarmReceiver.ACTION_MEDICATION_ALARM, medication.id,
            medication.name, medication.dosage, scheduledDate, scheduledTime,
            medication.instructions, medication.colorHex, medication.ringtoneUri, isSnooze = true
        ).apply {
            data = Uri.parse("pillpulse://snooze/${medication.id}/$scheduledDate/$scheduledTime")
        }
        val requestCode = intent.data.toString().hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return scheduleExact(context, triggerAt, pendingIntent)
    }

    fun cancelAlarms(context: Context, medicationId: Long) {
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = MedicationAlarmReceiver.ACTION_MEDICATION_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, scheduledRequestCode(medicationId), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            manager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduledRequestCode(medicationId: Long) = (medicationId * 100).toInt()

    internal fun findNextTarget(medication: Medication, now: LocalDateTime): LocalDateTime? {
        if (!medication.isActive || medication.scheduleType == ScheduleType.AS_NEEDED_PRN || medication.scheduledTimes.isEmpty()) return null
        for (dayOffset in 0..370) {
            val date = now.toLocalDate().plusDays(dayOffset.toLong())
            if (!isScheduledOn(medication, date)) continue
            val next = medication.scheduledTimes.mapNotNull { value ->
                runCatching { LocalDateTime.of(date, LocalTime.parse(value, timeFormat)) }.getOrNull()
            }.filter { it.isAfter(now) }.minOrNull()
            if (next != null) return next
        }
        return null
    }

    internal fun isScheduledOn(medication: Medication, date: LocalDate): Boolean = when (medication.scheduleType) {
        ScheduleType.DAILY -> true
        ScheduleType.SPECIFIC_DAYS -> medication.daysOfWeek.contains(date.dayOfWeek.value)
        ScheduleType.INTERVAL_CYCLE -> {
            if (medication.cycleStartDate.isBlank()) true else try {
                val diff = ChronoUnit.DAYS.between(LocalDate.parse(medication.cycleStartDate), date)
                val cycle = (medication.cycleDaysOn + medication.cycleDaysOff).coerceAtLeast(1)
                diff >= 0 && (diff % cycle).toInt() < medication.cycleDaysOn
            } catch (_: Exception) { true }
        }
        ScheduleType.AS_NEEDED_PRN -> false
    }

    private fun scheduleExact(context: Context, triggerAt: Long, pendingIntent: PendingIntent): Boolean {
        if (!canScheduleExactAlarms(context)) return false
        return try {
            val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                manager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun alarmIntent(
        context: Context,
        action: String,
        medicationId: Long,
        name: String,
        dosage: String,
        date: String,
        time: String,
        instructions: String,
        colorHex: Long,
        ringtoneUri: String? = null,
        isSnooze: Boolean = false
    ) = Intent(context, MedicationAlarmReceiver::class.java).apply {
        this.action = action
        putExtra(AlarmSoundService.EXTRA_MED_ID, medicationId)
        putExtra(AlarmSoundService.EXTRA_MED_NAME, name)
        putExtra(AlarmSoundService.EXTRA_DOSAGE, dosage)
        putExtra(AlarmSoundService.EXTRA_SCHED_DATE, date)
        putExtra(AlarmSoundService.EXTRA_SCHED_TIME, time)
        putExtra(AlarmSoundService.EXTRA_INSTRUCTIONS, instructions)
        putExtra(AlarmSoundService.EXTRA_COLOR_HEX, colorHex)
        putExtra(AlarmSoundService.EXTRA_RINGTONE_URI, ringtoneUri)
        putExtra(AlarmSoundService.EXTRA_IS_SNOOZE, isSnooze)
    }
}
