package com.pillease.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScheduleType {
    DAILY,          // Daily at specific times (e.g., 8:00 AM, 8:00 PM)
    SPECIFIC_DAYS,  // Specific days of week (e.g., Mon, Wed, Fri)
    CYCLE_DAYS,     // E.g., 21 days active, 7 days off (birth control, cyclic regimens)
    AS_NEEDED_PRN   // As-needed (pain relief, allergies, etc. with safety cooldown timer)
}

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val timesOfDay: List<String> = listOf("08:00"), // "HH:mm" 24h format
    val daysOfWeek: List<Int> = emptyList(),        // 1 = Mon .. 7 = Sun
    val cycleDaysOn: Int = 21,
    val cycleDaysOff: Int = 7,
    val cycleStartDate: String = "",                // "yyyy-MM-dd"
    val minHoursBetweenDoses: Int = 4,              // Cooldown timer for PRN
    val instructions: String = "",                  // E.g. "Take with food"
    val colorHex: String = "#0D9488",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
