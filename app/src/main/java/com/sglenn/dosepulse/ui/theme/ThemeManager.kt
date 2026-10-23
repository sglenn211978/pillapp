package com.sglenn.dosepulse.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ThemeColorPreset(
    val title: String,
    val primaryColor: Color,
    val hexValue: Long
) {
    MEDICAL_TEAL("Medical Teal", Color(0xFF00796B), 0xFF00796B),
    OCEAN_CYAN("Ocean Cyan", Color(0xFF00B4D8), 0xFF00B4D8),
    FOREST_EMERALD("Emerald Health", Color(0xFF059669), 0xFF059669),
    ROYAL_INDIGO("Royal Indigo", Color(0xFF6366F1), 0xFF6366F1),
    AMETHYST("Amethyst Purple", Color(0xFF8B5CF6), 0xFF8B5CF6),
    ROSE_CORAL("Rose Coral", Color(0xFFE11D48), 0xFFE11D48),
    SUNSET_AMBER("Warm Amber", Color(0xFFD97706), 0xFFD97706)
}

data class AppThemeState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val preset: ThemeColorPreset = ThemeColorPreset.MEDICAL_TEAL,
    val use24HourFormat: Boolean = false // Default to standard 12-Hour AM/PM
)

object ThemeManager {
    private const val PREFS_NAME = "dosepulse_theme_prefs"
    private const val KEY_THEME_MODE = "key_theme_mode"
    private const val KEY_COLOR_PRESET = "key_color_preset"
    private const val KEY_USE_24H = "key_use_24h"

    private lateinit var prefs: SharedPreferences
    private val _themeState = MutableStateFlow(AppThemeState())
    val themeState: StateFlow<AppThemeState> = _themeState.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val presetStr = prefs.getString(KEY_COLOR_PRESET, ThemeColorPreset.MEDICAL_TEAL.name) ?: ThemeColorPreset.MEDICAL_TEAL.name
        val use24H = prefs.getBoolean(KEY_USE_24H, false)

        val mode = try { ThemeMode.valueOf(modeStr) } catch (e: Exception) { ThemeMode.SYSTEM }
        val preset = try { ThemeColorPreset.valueOf(presetStr) } catch (e: Exception) { ThemeColorPreset.MEDICAL_TEAL }

        _themeState.value = AppThemeState(
            mode = mode,
            preset = preset,
            use24HourFormat = use24H
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeState.value = _themeState.value.copy(mode = mode)
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setColorPreset(preset: ThemeColorPreset) {
        _themeState.value = _themeState.value.copy(preset = preset)
        prefs.edit().putString(KEY_COLOR_PRESET, preset.name).apply()
    }

    fun set24HourFormat(use24Hour: Boolean) {
        _themeState.value = _themeState.value.copy(use24HourFormat = use24Hour)
        prefs.edit().putBoolean(KEY_USE_24H, use24Hour).apply()
    }
}
