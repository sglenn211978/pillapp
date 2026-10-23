package com.sglenn.dosepulse.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThemeSelectionDialog(
    themeState: AppThemeState,
    onDismiss: () -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onColorPresetChanged: (ThemeColorPreset) -> Unit,
    onTimeFormatChanged: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = themeState.preset.primaryColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Appearance & Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. THEME MODE SELECTOR
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Display Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeModeChip(
                            title = "System",
                            selected = themeState.mode == ThemeMode.SYSTEM,
                            onClick = { onThemeModeChanged(ThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeModeChip(
                            title = "☀️ Light",
                            selected = themeState.mode == ThemeMode.LIGHT,
                            onClick = { onThemeModeChanged(ThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeModeChip(
                            title = "🌙 Dark",
                            selected = themeState.mode == ThemeMode.DARK,
                            onClick = { onThemeModeChanged(ThemeMode.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. ACCENT COLOR PALETTE
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Primary Accent Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ThemeColorPreset.values().forEach { preset ->
                            val isSelected = themeState.preset == preset
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(preset.primaryColor)
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { onColorPresetChanged(preset) }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Divider()

                // 3. TIME FORMAT SELECTOR (12-Hour AM/PM vs 24-Hour)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("12-Hour AM / PM Format", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = if (!themeState.use24HourFormat) "Displays 8:00 AM / 8:00 PM" else "24-Hour Military (08:00 / 20:00)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }

                    Switch(
                        checked = !themeState.use24HourFormat, // checked means 12-hour AM/PM
                        onCheckedChange = { is12Hour ->
                            onTimeFormatChanged(!is12Hour)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ThemeModeChip(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
