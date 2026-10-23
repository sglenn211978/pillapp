package com.pillease.app.data

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

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteMedicationById(id: Long)
}
