package com.sglenn.dosepulse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun DosePulseTheme(
    themeState: AppThemeState = AppThemeState(),
    content: @Composable () -> Unit
) {
    val isDark = when (themeState.mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val primary = themeState.preset.primaryColor

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.3f),
            onPrimaryContainer = Color.White,
            secondary = BrightCyan,
            background = DarkSlateBg,
            surface = Color(0xFF1E293B),
            outline = Color(0xFF334155),
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = primary,
            secondary = BrightCyan,
            background = SoftMistSlate,
            surface = SurfaceWhite,
            outline = OutlineBorder,
            onBackground = TextPrimaryLight,
            onSurface = TextPrimaryLight
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
