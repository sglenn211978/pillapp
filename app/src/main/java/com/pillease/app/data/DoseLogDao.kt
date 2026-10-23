package com.pillease.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {

    @Query("SELECT * FROM dose_logs WHERE dateOnly = :dateOnly ORDER BY scheduledDateTime ASC, loggedDateTime ASC")
    fun getLogsForDate(dateOnly: String): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs ORDER BY scheduledDateTime DESC, loggedDateTime DESC LIMIT 200")
    fun getAllLogs(): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId ORDER BY loggedDateTime DESC LIMIT 1")
    suspend fun getLatestLogForMedication(medicationId: Long): DoseLog?

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId AND scheduledDateTime = :scheduledDateTime LIMIT 1")
    suspend fun getLogForMedicationAndTime(medicationId: Long, scheduledDateTime: String): DoseLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DoseLog): Long

    @Update
    suspend fun updateLog(log: DoseLog)

    @Delete
    suspend fun deleteLog(log: DoseLog)

    @Query("DELETE FROM dose_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM dose_logs WHERE medicationId = :medicationId AND scheduledDateTime = :scheduledDateTime")
    suspend fun deleteLogForMedicationAndTime(medicationId: Long, scheduledDateTime: String)
}
