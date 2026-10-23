package com.sglenn.dosepulse.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications ORDER BY isActive DESC, name ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: Long): Medication?

    @Query("SELECT * FROM medications WHERE isActive = 1")
    suspend fun getActiveMedicationsList(): List<Medication>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("UPDATE medications SET remainingPills = CASE WHEN remainingPills > 0 THEN remainingPills - 1 ELSE 0 END WHERE id = :id")
    suspend fun decrementPillStock(id: Long)

    @Query("UPDATE medications SET remainingPills = :count WHERE id = :id")
    suspend fun updatePillStock(id: Long, count: Int)
}

@Dao
interface DoseLogDao {
    @Query("SELECT * FROM dose_logs WHERE scheduledDate = :date ORDER BY scheduledTime ASC, id ASC")
    fun getLogsForDate(date: String): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs ORDER BY scheduledDate DESC, scheduledTime DESC, id DESC")
    fun getAllLogs(): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE medicationId = :medicationId AND scheduledDate = :date AND scheduledTime = :time LIMIT 1")
    suspend fun getLogForDose(medicationId: Long, date: String, time: String): DoseLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DoseLog): Long

    @Update
    suspend fun updateLog(log: DoseLog)

    @Query("DELETE FROM dose_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("SELECT COUNT(*) FROM dose_logs WHERE scheduledDate = :date AND status = 'TAKEN'")
    suspend fun getTakenCountForDate(date: String): Int

    @Query("SELECT COUNT(*) FROM dose_logs WHERE scheduledDate = :date")
    suspend fun getTotalScheduledForDate(date: String): Int
}

@Dao
interface MedicationReferenceDao {
    @Query("SELECT * FROM medication_references ORDER BY category ASC, name ASC")
    fun getAllReferences(): Flow<List<MedicationReference>>

    @Query("SELECT * FROM medication_references WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchReferences(query: String): Flow<List<MedicationReference>>

    @Query("SELECT DISTINCT category FROM medication_references ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(references: List<MedicationReference>)

    @Query("SELECT COUNT(*) FROM medication_references")
    suspend fun getReferenceCount(): Int
}
