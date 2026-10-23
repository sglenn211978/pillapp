package com.pillease.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DoseStatus {
    TAKEN,
    SNOOZED,
    SKIPPED,
    PENDING
}

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("medicationId"),
        Index("dateOnly"),
        Index("scheduledDateTime")
    ]
)
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    val scheduledDateTime: String,  // "yyyy-MM-dd HH:mm"
    val loggedDateTime: String,     // "yyyy-MM-dd HH:mm:ss"
    val status: DoseStatus = DoseStatus.TAKEN,
    val dateOnly: String,           // "yyyy-MM-dd"
    val notes: String = ""
)
