package com.example.util

object TimeFormatHelper {

    /**
     * Converts a 24h string ("08:00", "20:30") to friendly 12h AM/PM ("8:00 AM", "8:30 PM")
     * or preserves 24h format if use24Hour is true.
     */
    fun formatDisplayTime(time24: String, use24Hour: Boolean = false): String {
        return try {
            val parts = time24.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            if (use24Hour) {
                String.format("%02d:%02d", hour, minute)
            } else {
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                String.format("%d:%02d %s", displayHour, minute, amPm)
            }
        } catch (e: Exception) {
            time24
        }
    }

    fun parseHourMinute(time24: String): Pair<Int, Int> {
        return try {
            val parts = time24.split(":")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            Pair(8, 0)
        }
    }

    fun to24HourString(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }

    /**
     * Toggles between AM and PM for a given 24h time string.
     */
    fun toggleAmPm(time24: String): String {
        val (h, m) = parseHourMinute(time24)
        val newHour = if (h >= 12) h - 12 else h + 12
        return to24HourString(newHour, m)
    }
}
