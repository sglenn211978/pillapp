package com.pillease.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pillease.app.PillApplication
import com.pillease.app.data.Medication
import com.pillease.app.data.ScheduleType
import com.pillease.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medication"
        val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: ""
        val scheduledDateTime = intent.getStringExtra(EXTRA_SCHEDULED_DATETIME) ?: ""
        val instructions = intent.getStringExtra(EXTRA_INSTRUCTIONS) ?: ""

        val notificationId = if (medicationId != -1L) medicationId.toInt() else System.currentTimeMillis().toInt()

        // Show the high-priority notification with action buttons
        NotificationHelper.showMedicationNotification(
            context = context,
            notificationId = notificationId,
            medicationId = medicationId,
            medicationName = medicationName,
            dosage = dosage,
            scheduledDateTime = scheduledDateTime,
            instructions = instructions
        )

        // Reschedule next occurrence for this medication
        if (medicationId != -1L) {
            val repository = PillApplication.instance.repository
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val med = repository.getMedicationById(medicationId)
                    if (med != null && med.isActive) {
                        scheduleNextAlarmForMedication(context, med)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_MEDICATION_ALARM = "com.pillease.app.ACTION_MEDICATION_ALARM"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_SCHEDULED_DATETIME = "extra_scheduled_datetime"
        const val EXTRA_INSTRUCTIONS = "extra_instructions"

        fun scheduleNextAlarmForMedication(context: Context, medication: Medication) {
            if (!medication.isActive || medication.scheduleType == ScheduleType.AS_NEEDED_PRN) return
            if (medication.timesOfDay.isEmpty()) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val now = LocalDateTime.now()

            // Find the closest future scheduled time
            var targetDateTime: LocalDateTime? = null

            // Check upcoming 7 days to find next valid day & time
            for (dayOffset in 0..7) {
                val candidateDate = now.toLocalDate().plusDays(dayOffset.toLong())

                val isValidDay = when (medication.scheduleType) {
                    ScheduleType.DAILY -> true
                    ScheduleType.SPECIFIC_DAYS -> medication.daysOfWeek.contains(candidateDate.dayOfWeek.value)
                    ScheduleType.CYCLE_DAYS -> {
                        if (medication.cycleStartDate.isBlank()) true
                        else {
                            try {
                                val start = LocalDate.parse(medication.cycleStartDate)
                                val diff = java.time.temporal.ChronoUnit.DAYS.between(start, candidateDate)
                                if (diff < 0) false
                                else {
                                    val cycleLength = (medication.cycleDaysOn + medication.cycleDaysOff).coerceAtLeast(1)
                                    (diff % cycleLength).toInt() < medication.cycleDaysOn
                                }
                            } catch (e: Exception) {
                                true
                            }
                        }
                    }
                    ScheduleType.AS_NEEDED_PRN -> false
                }

                if (isValidDay) {
                    for (timeStr in medication.timesOfDay) {
                        val time = try {
                            LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (e: Exception) {
                            LocalTime.of(8, 0)
                        }
                        val candidateDateTime = LocalDateTime.of(candidateDate, time)
                        if (candidateDateTime.isAfter(now)) {
                            if (targetDateTime == null || candidateDateTime.isBefore(targetDateTime)) {
                                targetDateTime = candidateDateTime
                            }
                        }
                    }
                }
                if (targetDateTime != null) break
            }

            if (targetDateTime != null) {
                val triggerMillis = targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val dateStr = targetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_MEDICATION_ALARM
                    putExtra(EXTRA_MEDICATION_ID, medication.id)
                    putExtra(EXTRA_MEDICATION_NAME, medication.name)
                    putExtra(EXTRA_DOSAGE, medication.dosage)
                    putExtra(EXTRA_SCHEDULED_DATETIME, dateStr)
                    putExtra(EXTRA_INSTRUCTIONS, medication.instructions)
                }

                val requestCode = (medication.id * 100).toInt()
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            }
        }

        fun cancelAlarmsForMedication(context: Context, medicationId: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_MEDICATION_ALARM
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
}
