package com.sglenn.dosepulse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_references")
data class MedicationReference(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,                             // e.g. "Cardiology & Blood Pressure", "Mental Health"
    val commonDosages: List<String>,                 // e.g. ["10 mg", "20 mg", "40 mg"]
    val defaultForm: String,                          // tablet, capsule, inhaler, drops
    val intakeGuidance: String,                       // e.g. "Take with food to prevent GI upset"
    val clinicalWarning: String,                      // e.g. "Avoid alcohol and grapefruit juice"
    val suggestedColorHex: Long = 0xFF00796B
)
