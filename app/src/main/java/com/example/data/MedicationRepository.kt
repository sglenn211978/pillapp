package com.example.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class ScheduleItem(
    val medication: Medication,
    val scheduledTime: String,      // "HH:mm"
    val scheduledDate: String,      // "YYYY-MM-DD"
    val log: DoseLog?,
    val status: DoseStatus,
    val isDueNow: Boolean
)

data class ComplianceStats(
    val percentage: Int,            // 0 - 100%
    val takenCount: Int,
    val totalCount: Int,
    val streakDays: Int
)

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val doseLogDao: DoseLogDao,
    private val referenceDao: MedicationReferenceDao
) {
    val allActiveMedications: Flow<List<Medication>> = medicationDao.getAllActiveMedications()
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()
    val allLogs: Flow<List<DoseLog>> = doseLogDao.getAllLogs()
    val allReferences: Flow<List<MedicationReference>> = referenceDao.getAllReferences()
    val referenceCategories: Flow<List<String>> = referenceDao.getCategories()

    fun searchReferences(query: String): Flow<List<MedicationReference>> {
        return referenceDao.searchReferences(query)
    }

    fun getLogsForDate(date: LocalDate): Flow<List<DoseLog>> {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return doseLogDao.getLogsForDate(dateStr)
    }

    suspend fun getMedicationById(id: Long): Medication? = medicationDao.getMedicationById(id)

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

    suspend fun markTaken(
        medicationId: Long,
        scheduledDate: String,
        scheduledTime: String,
        notes: String = ""
    ): DoseLog? {
        val med = medicationDao.getMedicationById(medicationId) ?: return null
        val existing = doseLogDao.getLogForDose(medicationId, scheduledDate, scheduledTime)

        val log = DoseLog(
            id = existing?.id ?: 0,
            medicationId = medicationId,
            medicationName = med.name,
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            takenTime = System.currentTimeMillis(),
            status = DoseStatus.TAKEN,
            notes = notes
        )
        val newId = doseLogDao.insertLog(log)

        // Decrement pill stock
        medicationDao.decrementPillStock(medicationId)

        return log.copy(id = if (log.id == 0L) newId else log.id)
    }

    suspend fun markSnoozed(
        medicationId: Long,
        scheduledDate: String,
        scheduledTime: String
    ) {
        val med = medicationDao.getMedicationById(medicationId) ?: return
        val existing = doseLogDao.getLogForDose(medicationId, scheduledDate, scheduledTime)

        val log = DoseLog(
            id = existing?.id ?: 0,
            medicationId = medicationId,
            medicationName = med.name,
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            takenTime = System.currentTimeMillis(),
            status = DoseStatus.SNOOZED,
            notes = "Snoozed"
        )
        doseLogDao.insertLog(log)
    }

    suspend fun markSkipped(
        medicationId: Long,
        scheduledDate: String,
        scheduledTime: String,
        reason: String
    ) {
        val med = medicationDao.getMedicationById(medicationId) ?: return
        val existing = doseLogDao.getLogForDose(medicationId, scheduledDate, scheduledTime)

        val log = DoseLog(
            id = existing?.id ?: 0,
            medicationId = medicationId,
            medicationName = med.name,
            scheduledDate = scheduledDate,
            scheduledTime = scheduledTime,
            takenTime = System.currentTimeMillis(),
            status = DoseStatus.SKIPPED,
            skipReason = reason,
            notes = reason
        )
        doseLogDao.insertLog(log)
    }

    suspend fun undoDose(medicationId: Long, scheduledDate: String, scheduledTime: String) {
        val existing = doseLogDao.getLogForDose(medicationId, scheduledDate, scheduledTime)
        if (existing != null) {
            doseLogDao.deleteLogById(existing.id)
            // Restore inventory pill count if it was taken
            if (existing.status == DoseStatus.TAKEN) {
                val med = medicationDao.getMedicationById(medicationId)
                if (med != null) {
                    medicationDao.updatePillStock(medicationId, med.remainingPills + 1)
                }
            }
        }
    }

    // Compute doses for date
    fun computeSchedule(
        medications: List<Medication>,
        logs: List<DoseLog>,
        date: LocalDate,
        currentTime: LocalTime = LocalTime.now()
    ): List<ScheduleItem> {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val logMap = logs.associateBy { "${it.medicationId}_${it.scheduledDate}_${it.scheduledTime}" }
        val isToday = date == LocalDate.now()
        val items = mutableListOf<ScheduleItem>()

        for (med in medications) {
            if (!med.isActive) continue

            val shouldTake = when (med.scheduleType) {
                ScheduleType.DAILY -> true
                ScheduleType.SPECIFIC_DAYS -> med.daysOfWeek.contains(date.dayOfWeek.value)
                ScheduleType.INTERVAL_CYCLE -> {
                    if (med.cycleStartDate.isBlank()) true
                    else {
                        try {
                            val start = LocalDate.parse(med.cycleStartDate)
                            val diff = ChronoUnit.DAYS.between(start, date)
                            if (diff < 0) false
                            else {
                                val cycle = (med.cycleDaysOn + med.cycleDaysOff).coerceAtLeast(1)
                                (diff % cycle).toInt() < med.cycleDaysOn
                            }
                        } catch (e: Exception) {
                            true
                        }
                    }
                }
                ScheduleType.AS_NEEDED_PRN -> false
            }

            if (shouldTake) {
                for (time in med.scheduledTimes) {
                    val key = "${med.id}_${dateStr}_$time"
                    val log = logMap[key]
                    val status = log?.status ?: DoseStatus.PENDING

                    val scheduledLocalTime = try {
                        LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
                    } catch (e: Exception) {
                        LocalTime.of(8, 0)
                    }

                    val isDue = isToday && status == DoseStatus.PENDING && currentTime >= scheduledLocalTime

                    items.add(
                        ScheduleItem(
                            medication = med,
                            scheduledTime = time,
                            scheduledDate = dateStr,
                            log = log,
                            status = status,
                            isDueNow = isDue
                        )
                    )
                }
            }
        }
        return items.sortedBy { it.scheduledTime }
    }

    fun calculateCompliance(items: List<ScheduleItem>): ComplianceStats {
        val total = items.size
        val taken = items.count { it.status == DoseStatus.TAKEN }
        val percentage = if (total > 0) ((taken.toFloat() / total.toFloat()) * 100).toInt() else 100
        // Sample streak: 5 days active
        return ComplianceStats(
            percentage = percentage,
            takenCount = taken,
            totalCount = total,
            streakDays = 5
        )
    }
}
