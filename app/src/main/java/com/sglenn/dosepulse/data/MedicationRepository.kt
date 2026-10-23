package com.sglenn.dosepulse.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class ScheduleItem(
    val medication: Medication,
    val scheduledTime: String,
    val scheduledDate: String,
    val log: DoseLog?,
    val status: DoseStatus,
    val isDueNow: Boolean
)

data class ComplianceStats(val percentage: Int, val takenCount: Int, val totalCount: Int)

class MedicationRepository(private val database: AppDatabase) {
    private val medicationDao = database.medicationDao()
    private val doseLogDao = database.doseLogDao()
    private val referenceDao = database.medicationReferenceDao()

    val allActiveMedications: Flow<List<Medication>> = medicationDao.getAllActiveMedications()
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()
    val allLogs: Flow<List<DoseLog>> = doseLogDao.getAllLogs()
    val allReferences: Flow<List<MedicationReference>> = referenceDao.getAllReferences()
    val referenceCategories: Flow<List<String>> = referenceDao.getCategories()

    fun searchReferences(query: String): Flow<List<MedicationReference>> = referenceDao.searchReferences(query)

    fun getLogsForDate(date: LocalDate): Flow<List<DoseLog>> =
        doseLogDao.getLogsForDate(date.format(DateTimeFormatter.ISO_LOCAL_DATE))

    suspend fun getMedicationById(id: Long): Medication? = medicationDao.getMedicationById(id)

    suspend fun getDoseLog(medicationId: Long, scheduledDate: String, scheduledTime: String): DoseLog? =
        doseLogDao.getLogForDose(medicationId, scheduledDate, scheduledTime)

    suspend fun saveMedication(medication: Medication): Long = if (medication.id == 0L) {
        medicationDao.insertMedication(medication)
    } else {
        medicationDao.updateMedication(medication)
        medication.id
    }

    suspend fun deleteMedication(medication: Medication) = medicationDao.deleteMedication(medication)

    suspend fun markTaken(medicationId: Long, scheduledDate: String, scheduledTime: String, notes: String = ""): DoseLog? =
        database.withTransaction {
            val med = medicationDao.getMedicationById(medicationId) ?: return@withTransaction null
            val existing = doseLogDao.getLogForDose(medicationId, scheduledDate, scheduledTime)
            val now = System.currentTimeMillis()
            val log = DoseLog(
                id = existing?.id ?: 0,
                medicationId = medicationId,
                medicationName = med.name,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                takenTime = now,
                status = DoseStatus.TAKEN,
                notes = notes,
                snoozedUntil = null
            )
            val newId = doseLogDao.insertLog(log)
            if (existing?.status != DoseStatus.TAKEN) medicationDao.decrementPillStock(medicationId)
            log.copy(id = if (log.id == 0L) newId else log.id)
        }

    suspend fun markSnoozed(
        medicationId: Long,
        scheduledDate: String,
        scheduledTime: String,
        snoozeMinutes: Int = 10
    ) = database.withTransaction {
        val med = medicationDao.getMedicationById(medicationId) ?: return@withTransaction
        val existing = doseLogDao.getLogForDose(medicationId, scheduledDate, scheduledTime)
        if (existing?.status == DoseStatus.TAKEN || existing?.status == DoseStatus.SKIPPED) return@withTransaction
        val now = System.currentTimeMillis()
        doseLogDao.insertLog(
            DoseLog(
                id = existing?.id ?: 0,
                medicationId = medicationId,
                medicationName = med.name,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                takenTime = now,
                status = DoseStatus.SNOOZED,
                notes = "Snoozed",
                snoozedUntil = now + snoozeMinutes.coerceAtLeast(1) * 60_000L
            )
        )
    }

    suspend fun markSkipped(medicationId: Long, scheduledDate: String, scheduledTime: String, reason: String) =
        database.withTransaction {
            val med = medicationDao.getMedicationById(medicationId) ?: return@withTransaction
            val existing = doseLogDao.getLogForDose(medicationId, scheduledDate, scheduledTime)
            if (existing?.status == DoseStatus.TAKEN) medicationDao.updatePillStock(medicationId, med.remainingPills + 1)
            doseLogDao.insertLog(
                DoseLog(
                    id = existing?.id ?: 0,
                    medicationId = medicationId,
                    medicationName = med.name,
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime,
                    takenTime = System.currentTimeMillis(),
                    status = DoseStatus.SKIPPED,
                    skipReason = reason,
                    notes = reason,
                    snoozedUntil = null
                )
            )
        }

    suspend fun markMissed(medicationId: Long, scheduledDate: String, scheduledTime: String) =
        database.withTransaction {
            val med = medicationDao.getMedicationById(medicationId) ?: return@withTransaction
            val existing = doseLogDao.getLogForDose(medicationId, scheduledDate, scheduledTime)
            if (existing?.status == DoseStatus.TAKEN || existing?.status == DoseStatus.SKIPPED) return@withTransaction
            doseLogDao.insertLog(
                DoseLog(
                    id = existing?.id ?: 0,
                    medicationId = medicationId,
                    medicationName = med.name,
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime,
                    takenTime = System.currentTimeMillis(),
                    status = DoseStatus.MISSED,
                    notes = "Reminder timed out",
                    snoozedUntil = null
                )
            )
        }

    suspend fun undoDose(medicationId: Long, scheduledDate: String, scheduledTime: String) = database.withTransaction {
        val existing = doseLogDao.getLogForDose(medicationId, scheduledDate, scheduledTime) ?: return@withTransaction
        if (existing.status == DoseStatus.TAKEN) {
            medicationDao.getMedicationById(medicationId)?.let {
                medicationDao.updatePillStock(medicationId, it.remainingPills + 1)
            }
        }
        doseLogDao.deleteLogById(existing.id)
    }

    fun computeSchedule(
        medications: List<Medication>,
        logs: List<DoseLog>,
        date: LocalDate,
        currentTime: LocalTime = LocalTime.now()
    ): List<ScheduleItem> {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val logMap = logs.associateBy { "${it.medicationId}_${it.scheduledDate}_${it.scheduledTime}" }
        val isToday = date == LocalDate.now()
        val nowMillis = System.currentTimeMillis()
        val items = mutableListOf<ScheduleItem>()

        for (med in medications) {
            if (!med.isActive) continue
            val shouldTake = when (med.scheduleType) {
                ScheduleType.DAILY -> true
                ScheduleType.SPECIFIC_DAYS -> med.daysOfWeek.contains(date.dayOfWeek.value)
                ScheduleType.INTERVAL_CYCLE -> {
                    if (med.cycleStartDate.isBlank()) true else try {
                        val diff = ChronoUnit.DAYS.between(LocalDate.parse(med.cycleStartDate), date)
                        val cycle = (med.cycleDaysOn + med.cycleDaysOff).coerceAtLeast(1)
                        diff >= 0 && (diff % cycle).toInt() < med.cycleDaysOn
                    } catch (_: Exception) { true }
                }
                ScheduleType.AS_NEEDED_PRN -> false
            }
            if (!shouldTake) continue

            for (time in med.scheduledTimes) {
                val log = logMap["${med.id}_${dateStr}_$time"]
                val status = if (log?.status == DoseStatus.SNOOZED && (log.snoozedUntil ?: 0L) <= nowMillis) {
                    DoseStatus.PENDING
                } else log?.status ?: DoseStatus.PENDING
                val scheduledLocalTime = try {
                    LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"))
                } catch (_: Exception) { LocalTime.of(8, 0) }
                val due = isToday && status == DoseStatus.PENDING && currentTime >= scheduledLocalTime
                items.add(ScheduleItem(med, time, dateStr, log, status, due))
            }
        }
        return items.sortedBy { it.scheduledTime }
    }

    fun calculateCompliance(items: List<ScheduleItem>): ComplianceStats {
        val total = items.size
        val taken = items.count { it.status == DoseStatus.TAKEN }
        val percentage = if (total == 0) 0 else (taken * 100) / total
        return ComplianceStats(percentage, taken, total)
    }
}
