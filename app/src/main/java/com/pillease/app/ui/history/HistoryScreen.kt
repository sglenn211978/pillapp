package com.pillease.app.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.pillease.app.data.DoseLog
import com.pillease.app.data.DoseStatus
import com.pillease.app.ui.theme.*
import com.pillease.app.ui.viewmodel.MedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MedicationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allLogs by viewModel.allLogs.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dose History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (allLogs.isEmpty()) {
                                Toast.makeText(context, "No log entries to export yet", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            val report = buildString {
                                appendLine("💊 PillEase Medication Log Report")
                                appendLine("Generated on: ${java.time.LocalDateTime.now()}")
                                appendLine("====================================")
                                allLogs.forEach { log ->
                                    val statusEmoji = if (log.status == DoseStatus.TAKEN) "✓" else "✕"
                                    appendLine("$statusEmoji [${log.loggedDateTime}] ${log.medicationName} (${log.dosage}) - ${log.status.name} ${if (log.notes.isNotBlank()) "(${log.notes})" else ""}")
                                }
                            }

                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Medication Report", report)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Log copied to clipboard!", Toast.LENGTH_LONG).show()
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export Log", tint = TealPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SoftBackground)
            )
        },
        containerColor = SoftBackground
    ) { paddingValues ->
        if (allLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No doses logged yet.\nWhen doses are taken or skipped, they'll show here.",
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 22.sp
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
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recent Activity (${allLogs.size} records)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                items(allLogs, key = { it.id }) { log ->
                    HistoryItemCard(log = log)
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    log: DoseLog,
    modifier: Modifier = Modifier
) {
    val isTaken = log.status == DoseStatus.TAKEN

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
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
                    .background(if (isTaken) StatusGreenBg else Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isTaken) "✓" else "✕",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isTaken) StatusGreen else Color(0xFFDC2626)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.medicationName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = log.dosage,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = log.loggedDateTime.substringAfter(" ").take(5),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = log.dateOnly,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}
