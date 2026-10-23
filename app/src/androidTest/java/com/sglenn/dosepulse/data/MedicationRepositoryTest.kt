package com.sglenn.dosepulse.data

import androidx.room.Room
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sglenn.dosepulse.alarm.MedicationAlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class MedicationRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MedicationRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = MedicationRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun repeatedTakenAndUndoOnlyChangeInventoryOnce() = runBlocking {
        val id = addMedication(remainingPills = 8)
        val today = LocalDate.now().toString()

        repository.markTaken(id, today, "08:00")
        repository.markTaken(id, today, "08:00")
        assertEquals(7, repository.getMedicationById(id)?.remainingPills)

        repository.undoDose(id, today, "08:00")
        repository.undoDose(id, today, "08:00")
        assertEquals(8, repository.getMedicationById(id)?.remainingPills)
    }

    @Test
    fun snoozedDoseCanBeTakenAndOnlyConsumesOnePill() = runBlocking {
        val id = addMedication(remainingPills = 4)
        val today = LocalDate.now().toString()

        repository.markSnoozed(id, today, "08:00", snoozeMinutes = 1)
        val snoozed = repository.getDoseLog(id, today, "08:00")!!
        assertEquals(DoseStatus.SNOOZED, snoozed.status)
        assertTrue((snoozed.snoozedUntil ?: 0L) > System.currentTimeMillis())

        repository.markTaken(id, today, "08:00")
        repository.markTaken(id, today, "08:00")
        assertEquals(DoseStatus.TAKEN, repository.getDoseLog(id, today, "08:00")?.status)
        assertEquals(3, repository.getMedicationById(id)?.remainingPills)
    }

    @Test
    fun missedDoseCanBeCorrectedToTaken() = runBlocking {
        val id = addMedication(remainingPills = 6)
        val today = LocalDate.now().toString()

        repository.markMissed(id, today, "08:00")
        assertEquals(DoseStatus.MISSED, repository.getDoseLog(id, today, "08:00")?.status)
        repository.markTaken(id, today, "08:00")
        assertEquals(DoseStatus.TAKEN, repository.getDoseLog(id, today, "08:00")?.status)
        assertEquals(5, repository.getMedicationById(id)?.remainingPills)
    }

    @Test
    fun historyQueryDoesNotTruncateAtThreeHundredRows() = runBlocking {
        repeat(301) { index ->
            database.doseLogDao().insertLog(
                DoseLog(
                    medicationId = 1L,
                    medicationName = "Test",
                    scheduledDate = LocalDate.now().minusDays(index.toLong()).toString(),
                    scheduledTime = "08:00",
                    status = DoseStatus.MISSED
                )
            )
        }
        assertEquals(301, repository.allLogs.first().size)
    }

    @Test
    fun emptyScheduleDoesNotReportFullCompliance() {
        val stats = repository.calculateCompliance(emptyList())
        assertEquals(0, stats.percentage)
        assertEquals(0, stats.totalCount)
    }

    @Test
    fun dailyAlarmPlannerMovesToNextDoseThenNextDay() {
        val medication = Medication(name = "Test medication", dosage = "1 tablet", scheduledTimes = listOf("08:00", "20:00"))
        assertEquals(
            LocalDateTime.of(2026, 10, 23, 20, 0),
            MedicationAlarmScheduler.findNextTarget(medication, LocalDateTime.of(2026, 10, 23, 8, 1))
        )
        assertEquals(
            LocalDateTime.of(2026, 10, 24, 8, 0),
            MedicationAlarmScheduler.findNextTarget(medication, LocalDateTime.of(2026, 10, 23, 20, 1))
        )
    }

    private suspend fun addMedication(remainingPills: Int): Long = database.medicationDao().insertMedication(
        Medication(name = "Test medication", dosage = "1 tablet", remainingPills = remainingPills)
    )
}
