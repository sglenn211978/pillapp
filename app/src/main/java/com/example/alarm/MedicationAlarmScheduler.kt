package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.Medication
import com.example.data.ScheduleType
import com.example.receiver.MedicationAlarmReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object MedicationAlarmScheduler {

    const val TEST_MED_ID = 999999L

    fun scheduleTestAlarm(context: Context, delaySeconds: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)

        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = MedicationAlarmReceiver.ACTION_TEST_ALARM
            putExtra(AlarmSoundService.EXTRA_MED_ID, TEST_MED_ID)
            putExtra(AlarmSoundService.EXTRA_MED_NAME, "Test Alarm (Lisinopril)")
            putExtra(AlarmSoundService.EXTRA_DOSAGE, "10 mg")
            putExtra(AlarmSoundService.EXTRA_SCHED_DATE, LocalDate.now().toString())
            putExtra(AlarmSoundService.EXTRA_SCHED_TIME, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
            putExtra(AlarmSoundService.EXTRA_INSTRUCTIONS, "Slide right to confirm dose and stop alarm.")
            putExtra(AlarmSoundService.EXTRA_COLOR_HEX, 0xFF00796BL)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            TEST_MED_ID.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun scheduleNextAlarm(context: Context, medication: Medication) {
        if (!medication.isActive || medication.scheduleType == ScheduleType.AS_NEEDED_PRN) return
        if (medication.scheduledTimes.isEmpty()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDateTime.now()
        var nextTarget: LocalDateTime? = null

        for (dayOffset in 0..7) {
            val candidateDate = now.toLocalDate().plusDays(dayOffset.toLong())

            val isValidDay = when (medication.scheduleType) {
                ScheduleType.DAILY -> true
                ScheduleType.SPECIFIC_DAYS -> medication.daysOfWeek.contains(candidateDate.dayOfWeek.value)
                ScheduleType.INTERVAL_CYCLE -> {
                    if (medication.cycleStartDate.isBlank()) true
                    else {
                        try {
                            val start = LocalDate.parse(medication.cycleStartDate)
                            val diff = java.time.temporal.ChronoUnit.DAYS.between(start, candidateDate)
                            if (diff < 0) false
                            else {
                                val cycle = (medication.cycleDaysOn + medication.cycleDaysOff).coerceAtLeast(1)
                                (diff % cycle).toInt() < medication.cycleDaysOn
                            }
                        } catch (e: Exception) {
                            true
                        }
                    }
                }
                ScheduleType.AS_NEEDED_PRN -> false
            }

            if (isValidDay) {
                for (timeStr in medication.scheduledTimes) {
                    val time = try {
                        LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
                    } catch (e: Exception) {
                        LocalTime.of(8, 0)
                    }
                    val candidate = LocalDateTime.of(candidateDate, time)
                    if (candidate.isAfter(now)) {
                        if (nextTarget == null || candidate.isBefore(nextTarget)) {
                            nextTarget = candidate
                        }
                    }
                }
            }
            if (nextTarget != null) break
        }

        if (nextTarget != null) {
            val triggerMillis = nextTarget.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                action = MedicationAlarmReceiver.ACTION_MEDICATION_ALARM
                putExtra(AlarmSoundService.EXTRA_MED_ID, medication.id)
                putExtra(AlarmSoundService.EXTRA_MED_NAME, medication.name)
                putExtra(AlarmSoundService.EXTRA_DOSAGE, medication.dosage)
                putExtra(AlarmSoundService.EXTRA_SCHED_DATE, nextTarget.toLocalDate().toString())
                putExtra(AlarmSoundService.EXTRA_SCHED_TIME, nextTarget.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                putExtra(AlarmSoundService.EXTRA_INSTRUCTIONS, medication.instructions)
                putExtra(AlarmSoundService.EXTRA_COLOR_HEX, medication.colorHex)
                putExtra(AlarmSoundService.EXTRA_RINGTONE_URI, medication.ringtoneUri)
            }

            val requestCode = (medication.id * 100).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        }
    }

    fun cancelAlarms(context: Context, medicationId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            action = MedicationAlarmReceiver.ACTION_MEDICATION_ALARM
        }
        val requestCode = (medicationId * 100).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
