package com.pillease.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pillease.app.data.DailyDoseItem
import com.pillease.app.ui.theme.*

@Composable
fun MedicationCard(
    item: DailyDoseItem,
    onToggleTaken: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val pillColor = try {
        Color(android.graphics.Color.parseColor(item.medication.colorHex))
    } catch (e: Exception) {
        TealPrimary
    }

    val cardBg = if (item.isTaken) Color(0xFFF1F5F9) else Color.White
    val borderStroke = if (item.isDueNow) 1.5.dp else 0.dp
    val borderColor = if (item.isDueNow) TealPrimary else Color.Transparent

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isTaken) 0.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (item.isDueNow) Modifier.border(borderStroke, borderColor, RoundedCornerShape(16.dp))
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill Color Capsule indicator
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(pillColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.scheduledTime,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isDueNow) CoralAccent else TextSecondary
                    )

                    if (item.isDueNow) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StatusAmberBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DUE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusAmber
                            )
                        }
                    } else if (item.isSkipped) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SKIPPED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.medication.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isTaken) TextMuted else TextPrimary,
                    textDecoration = if (item.isTaken) TextDecoration.LineThrough else TextDecoration.None
                )

                Text(
                    text = "${item.medication.dosage}${if (item.medication.instructions.isNotBlank()) " • " + item.medication.instructions else ""}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                if (item.isTaken && item.log != null) {
                    Text(
                        text = "Taken at ${item.log.loggedDateTime.substringAfter(" ").take(5)}",
                        fontSize = 11.sp,
                        color = StatusGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Actions: 3-dot overflow menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextMuted
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Skip Dose") },
                        onClick = {
                            showMenu = false
                            onSkip()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Medication") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // 1-TAP BIG LOG BUTTON
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isTaken) StatusGreen
                        else Color(0xFFF1F5F9)
                    )
                    .border(
                        width = if (item.isTaken) 0.dp else 2.dp,
                        color = if (item.isTaken) Color.Transparent else Color(0xFFCBD5E1),
                        shape = CircleShape
                    )
                    .clickable { onToggleTaken() }
            ) {
                if (item.isTaken) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Taken",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
