package com.pillease.app.data

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return value?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    @TypeConverter
    fun fromScheduleType(value: ScheduleType?): String {
        return value?.name ?: ScheduleType.DAILY.name
    }

    @TypeConverter
    fun toScheduleType(value: String?): ScheduleType {
        return try {
            value?.let { ScheduleType.valueOf(it) } ?: ScheduleType.DAILY
        } catch (e: Exception) {
            ScheduleType.DAILY
        }
    }

    @TypeConverter
    fun fromDoseStatus(value: DoseStatus?): String {
        return value?.name ?: DoseStatus.TAKEN.name
    }

    @TypeConverter
    fun toDoseStatus(value: String?): DoseStatus {
        return try {
            value?.let { DoseStatus.valueOf(it) } ?: DoseStatus.TAKEN
        } catch (e: Exception) {
            DoseStatus.TAKEN
        }
    }
}
