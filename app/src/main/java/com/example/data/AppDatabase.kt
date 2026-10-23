package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Medication::class, DoseLog::class, MedicationReference::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun medicationReferenceDao(): MedicationReferenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pillpulse_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-seed 50+ clinical medications in background
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getDatabase(context)
                            database.medicationReferenceDao().insertAll(ClinicalSeeds.items)

                            // Pre-seed 3 active sample medications for immediate out-of-the-box experience
                            val medDao = database.medicationDao()
                            medDao.insertMedication(
                                Medication(
                                    name = "Lisinopril",
                                    dosage = "10 mg",
                                    iconType = "tablet",
                                    colorHex = 0xFF00796B,
                                    scheduleType = ScheduleType.DAILY,
                                    scheduledTimes = listOf("08:00"),
                                    instructions = "Take with water in the morning",
                                    remainingPills = 28,
                                    lowStockThreshold = 7
                                )
                            )
                            medDao.insertMedication(
                                Medication(
                                    name = "Amoxicillin",
                                    dosage = "500 mg",
                                    iconType = "capsule",
                                    colorHex = 0xFFE11D48,
                                    scheduleType = ScheduleType.DAILY,
                                    scheduledTimes = listOf("08:00", "20:00"),
                                    instructions = "Take with food to prevent GI upset",
                                    remainingPills = 14,
                                    lowStockThreshold = 4
                                )
                            )
                            medDao.insertMedication(
                                Medication(
                                    name = "Vitamin D3",
                                    dosage = "2000 IU",
                                    iconType = "capsule",
                                    colorHex = 0xFF059669,
                                    scheduleType = ScheduleType.DAILY,
                                    scheduledTimes = listOf("09:00"),
                                    instructions = "Take with breakfast",
                                    remainingPills = 60,
                                    lowStockThreshold = 10
                                )
                            )
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
