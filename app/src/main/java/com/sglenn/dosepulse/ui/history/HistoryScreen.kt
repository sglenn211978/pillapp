package com.sglenn.dosepulse.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sglenn.dosepulse.data.DoseLog
import com.sglenn.dosepulse.data.DoseStatus
import com.sglenn.dosepulse.ui.theme.*
import com.sglenn.dosepulse.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allLogs by viewModel.allLogs.collectAsState(initial = emptyList())

    val takenCount = allLogs.count { it.status == DoseStatus.TAKEN }
    val skippedCount = allLogs.count { it.status == DoseStatus.SKIPPED }
    val snoozedCount = allLogs.count { it.status == DoseStatus.SNOOZED }
    val missedCount = allLogs.count { it.status == DoseStatus.MISSED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Dose History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimaryLight
                        )
                        Text(
                            text = "$takenCount taken • $skippedCount skipped • $snoozedCount snoozed • $missedCount missed",
                            fontSize = 11.sp,
                            color = TextSecondaryLight
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (allLogs.isEmpty()) {
                                Toast.makeText(context, "No log records available to export", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            val report = buildString {
                                appendLine("PillPulse Dose History")
                                appendLine("Generated: ${java.time.LocalDateTime.now()}")
                                appendLine("Taken: $takenCount | Skipped: $skippedCount | Snoozed: $snoozedCount | Missed: $missedCount")
                                appendLine("=================================================")
                                allLogs.forEach { log ->
                                    val statusMark = when (log.status) {
                                        DoseStatus.TAKEN -> "✓ [TAKEN]"
                                        DoseStatus.SKIPPED -> "– [SKIPPED]"
                                        DoseStatus.SNOOZED -> "↻ [SNOOZED]"
                                        DoseStatus.MISSED -> "! [MISSED]"
                                        DoseStatus.PENDING -> "… [PENDING]"
                                    }
                                    val timeTaken = if (log.takenTime != null) {
                                        java.time.Instant.ofEpochMilli(log.takenTime).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                                    } else "N/A"
                                    appendLine("$statusMark ${log.scheduledDate} ${log.scheduledTime} - ${log.medicationName} (Action at: $timeTaken)")
                                    if (!log.skipReason.isNullOrBlank()) {
                                        appendLine("    Reason: ${log.skipReason}")
                                    }
                                }
                            }

                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("PillPulse Adherence Report", report))
                            Toast.makeText(context, "Report copied to clipboard for your doctor!", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export Report", tint = MedicalTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SoftMistSlate)
            )
        },
        containerColor = SoftMistSlate
    ) { paddingValues ->
        if (allLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No dose history yet.\nTaken, snoozed, skipped, and missed doses will appear here.",
                    color = TextSecondaryLight,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(allLogs, key = { it.id }) { log ->
                    LogItemCard(log = log)
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun LogItemCard(
    log: DoseLog,
    modifier: Modifier = Modifier
) {
    val statusColor = when (log.status) {
        DoseStatus.TAKEN -> StatusTaken
        DoseStatus.SNOOZED -> StatusSnoozed
        DoseStatus.SKIPPED, DoseStatus.MISSED -> Color(0xFFDC2626)
        DoseStatus.PENDING -> TextMutedLight
    }
    val statusBackground = when (log.status) {
        DoseStatus.TAKEN -> StatusTakenBg
        DoseStatus.SNOOZED -> StatusSnoozedBg
        DoseStatus.SKIPPED, DoseStatus.MISSED -> Color(0xFFFEE2E2)
        DoseStatus.PENDING -> Color(0xFFE2E8F0)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, OutlineBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (log.status) {
                        DoseStatus.TAKEN -> "✓"
                        DoseStatus.SNOOZED -> "↻"
                        DoseStatus.SKIPPED -> "–"
                        DoseStatus.MISSED -> "!"
                        DoseStatus.PENDING -> "…"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.medicationName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimaryLight
                )
                Text(
                    text = "Scheduled: ${log.scheduledDate} at ${log.scheduledTime}",
                    fontSize = 12.sp,
                    color = TextSecondaryLight
                )

                if (!log.skipReason.isNullOrBlank()) {
                    Text(
                        text = "Reason: ${log.skipReason}",
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = log.status.name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = statusColor
            )
        }
    }
}
