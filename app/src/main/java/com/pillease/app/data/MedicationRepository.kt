package com.pillease.app.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class DailyDoseItem(
    val medication: Medication,
    val scheduledTime: String,      // "HH:mm"
    val scheduledDateTime: String,  // "yyyy-MM-dd HH:mm"
    val log: DoseLog?,
    val isTaken: Boolean,
    val isSkipped: Boolean,
    val isDueNow: Boolean           // past scheduled time and not yet taken/skipped
)

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val doseLogDao: DoseLogDao
) {
    val allActiveMedications: Flow<List<Medication>> = medicationDao.getAllActiveMedications()
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()
    val allLogs: Flow<List<DoseLog>> = doseLogDao.getAllLogs()

    fun getLogsForDate(date: LocalDate): Flow<List<DoseLog>> {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return doseLogDao.getLogsForDate(dateStr)
    }

    suspend fun getMedicationById(id: Long): Medication? {
        return medicationDao.getMedicationById(id)
    }

    suspend fun saveMedication(medication: Medication): Long {
        return if (medication.id == 0L) {
            medicationDao.insertMedication(medication)
        } else {
            medicationDao.updateMedication(medication)
            medication.id
        }
    }

    suspend fun deleteMedication(medication: Medication) {
        medicationDao.deleteMedication(medication)
    }

    suspend fun deleteMedicationById(id: Long) {
        medicationDao.deleteMedicationById(id)
    }

    // 1-Tap Mark Taken
    suspend fun markDoseTaken(medicationId: Long, scheduledDateTime: String, customLoggedTime: LocalDateTime? = null): DoseLog? {
        val med = medicationDao.getMedicationById(medicationId) ?: return null
        val now = customLoggedTime ?: LocalDateTime.now()
        val dateOnly = scheduledDateTime.substringBefore(" ")

        // Check if existing log exists to update or insert
        val existing = doseLogDao.getLogForMedicationAndTime(medicationId, scheduledDateTime)
        val log = DoseLog(
            id = existing?.id ?: 0,
            medicationId = medicationId,
            medicationName = med.name,
            dosage = med.dosage,
            scheduledDateTime = scheduledDateTime,
            loggedDateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            status = DoseStatus.TAKEN,
            dateOnly = dateOnly
        )
        val newId = doseLogDao.insertLog(log)
        return log.copy(id = if (log.id == 0L) newId else log.id)
    }

    // Mark Dose Skipped
    suspend fun markDoseSkipped(medicationId: Long, scheduledDateTime: String, reason: String = "") {
        val med = medicationDao.getMedicationById(medicationId) ?: return
        val now = LocalDateTime.now()
        val dateOnly = scheduledDateTime.substringBefore(" ")

        val existing = doseLogDao.getLogForMedicationAndTime(medicationId, scheduledDateTime)
        val log = DoseLog(
            id = existing?.id ?: 0,
            medicationId = medicationId,
            medicationName = med.name,
            dosage = med.dosage,
            scheduledDateTime = scheduledDateTime,
            loggedDateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            status = DoseStatus.SKIPPED,
            dateOnly = dateOnly,
            notes = reason
        )
        doseLogDao.insertLog(log)
    }

    // Undo Dose (e.g. if accidentally clicked)
    suspend fun undoDose(medicationId: Long, scheduledDateTime: String) {
        doseLogDao.deleteLogForMedicationAndTime(medicationId, scheduledDateTime)
    }

    // 1-Tap Log PRN / As Needed Dose
    suspend fun logAsNeededDose(medicationId: Long): DoseLog? {
        val med = medicationDao.getMedicationById(medicationId) ?: return null
        val now = LocalDateTime.now()
        val dateStr = now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val timeStr = now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        val scheduledDateTime = "$dateStr $timeStr (PRN)"

        val log = DoseLog(
            medicationId = medicationId,
            medicationName = med.name,
            dosage = med.dosage,
            scheduledDateTime = scheduledDateTime,
            loggedDateTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            status = DoseStatus.TAKEN,
            dateOnly = dateStr,
            notes = "As Needed"
        )
        val id = doseLogDao.insertLog(log)
        return log.copy(id = id)
    }

    suspend fun getLatestLogForMedication(medicationId: Long): DoseLog? {
        return doseLogDao.getLatestLogForMedication(medicationId)
    }

    // Computes scheduled doses for a given date
    fun computeDosesForDate(
        medications: List<Medication>,
        logs: List<DoseLog>,
        date: LocalDate,
        currentTime: LocalTime = LocalTime.now()
    ): List<DailyDoseItem> {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val items = mutableListOf<DailyDoseItem>()
        val logMap = logs.associateBy { "${it.medicationId}_${it.scheduledDateTime}" }

        val isToday = date == LocalDate.now()

        for (med in medications) {
            if (!med.isActive) continue

            val shouldTakeOnDate = when (med.scheduleType) {
                ScheduleType.DAILY -> true
                ScheduleType.SPECIFIC_DAYS -> {
                    // Day of week in Java Time: 1 = Mon ... 7 = Sun
                    med.daysOfWeek.contains(date.dayOfWeek.value)
                }
                ScheduleType.CYCLE_DAYS -> {
                    if (med.cycleStartDate.isBlank()) true
                    else {
                        try {
                            val startDate = LocalDate.parse(med.cycleStartDate)
                            val daysBetween = ChronoUnit.DAYS.between(startDate, date)
                            if (daysBetween < 0) false
                            else {
                                val cycleLength = (med.cycleDaysOn + med.cycleDaysOff).coerceAtLeast(1)
                                val cycleDay = (daysBetween % cycleLength).toInt()
                                cycleDay < med.cycleDaysOn
                            }
                        } catch (e: Exception) {
                            true
                        }
                    }
                }
                ScheduleType.AS_NEEDED_PRN -> false // PRN handled in separate section
            }

            if (shouldTakeOnDate) {
                for (timeStr in med.timesOfDay) {
                    val scheduledDateTime = "$dateStr $timeStr"
                    val log = logMap["${med.id}_$scheduledDateTime"]
                    val isTaken = log?.status == DoseStatus.TAKEN
                    val isSkipped = log?.status == DoseStatus.SKIPPED

                    val scheduledLocalTime = try {
                        LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
                    } catch (e: Exception) {
                        LocalTime.of(8, 0)
                    }

                    val isDueNow = isToday && !isTaken && !isSkipped && currentTime >= scheduledLocalTime

                    items.add(
                        DailyDoseItem(
                            medication = med,
                            scheduledTime = timeStr,
                            scheduledDateTime = scheduledDateTime,
                            log = log,
                            isTaken = isTaken,
                            isSkipped = isSkipped,
                            isDueNow = isDueNow
                        )
                    )
                }
            }
        }

        return items.sortedBy { it.scheduledTime }
    }
}
