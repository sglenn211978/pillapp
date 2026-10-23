package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DoseStatus {
    PENDING,
    TAKEN,
    SNOOZED,
    SKIPPED
}

enum class ScheduleType {
    DAILY,
    SPECIFIC_DAYS,
    INTERVAL_CYCLE,
    AS_NEEDED_PRN
}

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,                               // e.g. "500 mg", "1 tablet"
    val iconType: String = "tablet",                 // tablet, capsule, drops, injection, inhaler, syrup
    val colorHex: Long = 0xFF00796B,                  // Deep Medical Teal default
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val scheduledTimes: List<String> = listOf("08:00"),// ["08:00", "20:00"]
    val daysOfWeek: List<Int> = emptyList(),          // 1 = Mon .. 7 = Sun
    val cycleDaysOn: Int = 21,
    val cycleDaysOff: Int = 7,
    val cycleStartDate: String = "",                  // "YYYY-MM-DD"
    val instructions: String = "",                    // e.g. "Take with food and a full glass of water"
    val remainingPills: Int = 30,                     // Inventory count
    val lowStockThreshold: Int = 7,                   // Warning threshold
    val ringtoneUri: String = "",                     // Custom ringtone URI (or default)
    val minHoursBetweenDoses: Int = 4,                // Cooldown for PRN
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "dose_logs")
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val scheduledDate: String,                        // YYYY-MM-DD
    val scheduledTime: String,                        // HH:mm
    val takenTime: Long? = null,                      // epoch millis when action occurred
    val status: DoseStatus = DoseStatus.PENDING,
    val skipReason: String? = null,
    val notes: String = ""
)
