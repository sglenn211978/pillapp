package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = MedicalTeal,
    onPrimary = Color.White,
    primaryContainer = CyanContainer,
    onPrimaryContainer = MedicalTealDark,
    secondary = BrightCyan,
    background = SoftMistSlate,
    surface = SurfaceWhite,
    outline = OutlineBorder,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight
)

private val DarkColorScheme = darkColorScheme(
    primary = BrightCyan,
    onPrimary = MedicalTealDark,
    primaryContainer = MedicalTealDark,
    onPrimaryContainer = Color.White,
    secondary = BrightCyan,
    background = DarkSlateBg,
    surface = Color(0xFF1E293B),
    outline = Color(0xFF334155),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

@Composable
fun PillPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
