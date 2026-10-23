package com.sglenn.dosepulse.ui.dashboard

import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sglenn.dosepulse.data.DoseStatus
import com.sglenn.dosepulse.data.ScheduleItem
import com.sglenn.dosepulse.ui.components.ComplianceRing
import com.sglenn.dosepulse.ui.theme.*
import com.sglenn.dosepulse.ui.viewmodel.DashboardViewModel
import com.sglenn.dosepulse.alarm.MedicationAlarmScheduler
import androidx.core.app.NotificationManagerCompat
import com.sglenn.dosepulse.util.TimeFormatHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAddEdit: (Long) -> Unit,
    permissionRefreshKey: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val schedule by viewModel.dailySchedule.collectAsState()
    val complianceStats by viewModel.complianceStats.collectAsState()
    val activeAlarm by viewModel.activeAlarm.collectAsState()
    val themeState by ThemeManager.themeState.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var undoItem by remember { mutableStateOf<ScheduleItem?>(null) }

    val isToday = selectedDate == LocalDate.now()
    val notificationsAllowed = remember(permissionRefreshKey) { NotificationManagerCompat.from(context).areNotificationsEnabled() }
    val exactAlarmsAllowed = remember(permissionRefreshKey) { MedicationAlarmScheduler.canScheduleExactAlarms(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DosePulse",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Offline Medication Tracker",
                                fontSize = 11.sp,
                                color = TextSecondaryLight
                            )
                        }
                    }
                },
                actions = {
                    // Theme / Settings Dialog Icon
                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme & Appearance",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Test Alarm (5s) button
                    FilledTonalButton(
                        onClick = {
                            if (!notificationsAllowed) {
                                Toast.makeText(context, "Allow notifications in Android settings before testing reminders.", Toast.LENGTH_LONG).show()
                            } else if (viewModel.testAlarm5s(context)) {
                                Toast.makeText(context, "Test alarm scheduled for 5 seconds.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Allow exact alarms in Android settings to test reminders.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test (5s)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!notificationsAllowed || !exactAlarmsAllowed) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Reminder access needs attention", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Allow notifications and exact alarms so scheduled reminders can alert you on time.", color = MaterialTheme.colorScheme.onErrorContainer)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!notificationsAllowed) TextButton(onClick = {
                                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                                }) { Text("Notifications") }
                                if (!exactAlarmsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) TextButton(onClick = {
                                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                                }) { Text("Exact alarms") }
                            }
                        }
                    }
                }
            }
            // 1. LIVE EMERGENCY STOP BANNER (if alarm is actively ringing)
            item {
                AnimatedVisibility(visible = activeAlarm.isRinging) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusUrgent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🚨 ALARM RINGING NOW",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${activeAlarm.medicationName} (${activeAlarm.dosage})",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(
                                onClick = { viewModel.silenceAlarm(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = StatusUrgent
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Silence", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. DATE NAVIGATOR (< Yesterday | Today | Tomorrow >)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setSelectedDate(selectedDate.minusDays(1)) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { viewModel.setSelectedDate(LocalDate.now()) }
                    ) {
                        Text(
                            text = if (isToday) "Today, " + selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))
                            else selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setSelectedDate(selectedDate.plusDays(1)) },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                    }
                }
            }

            // 3. DAILY COMPLIANCE RING GAUGE
            item {
                ComplianceRing(stats = complianceStats, date = selectedDate)
            }

            // 4. TIMELINE HEADER
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scheduled Doses (${schedule.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = { onNavigateToAddEdit(-1L) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Med", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. DOSE CARDS
            if (schedule.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No medications scheduled for this day.", color = TextSecondaryLight)
                        }
                    }
                }
            } else {
                items(schedule, key = { "${it.medication.id}_${it.scheduledDate}_${it.scheduledTime}" }) { item ->
                    ScheduleDoseCard(
                        item = item,
                        use24Hour = themeState.use24HourFormat,
                        onToggleTaken = {
                            if (item.status == DoseStatus.TAKEN) undoItem = item
                            else viewModel.toggleTaken(context, item)
                        },
                        onEdit = { onNavigateToAddEdit(item.medication.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // THEME CUSTOMIZATION MODAL
    if (showThemeDialog) {
        ThemeSelectionDialog(
            themeState = themeState,
            onDismiss = { showThemeDialog = false },
            onThemeModeChanged = { ThemeManager.setThemeMode(it) },
            onColorPresetChanged = { ThemeManager.setColorPreset(it) },
            onTimeFormatChanged = { ThemeManager.set24HourFormat(it) }
        )
    }

    undoItem?.let { item ->
        AlertDialog(
            onDismissRequest = { undoItem = null },
            title = { Text("Undo taken dose?") },
            text = { Text("This removes the taken record and restores one pill to ${item.medication.name} inventory.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleTaken(context, item)
                    undoItem = null
                }) { Text("Undo dose") }
            },
            dismissButton = { TextButton(onClick = { undoItem = null }) { Text("Keep dose") } }
        )
    }
}

@Composable
fun ScheduleDoseCard(
    item: ScheduleItem,
    use24Hour: Boolean,
    onToggleTaken: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTaken = item.status == DoseStatus.TAKEN
    val isSkipped = item.status == DoseStatus.SKIPPED
    val pillColor = Color(item.medication.colorHex)

    val cardBg = if (isTaken) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface
    val isLowStock = item.medication.remainingPills <= item.medication.lowStockThreshold

    // Friendly 12-Hour AM/PM format
    val displayTime = TimeFormatHelper.formatDisplayTime(item.scheduledTime, use24Hour)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTaken) 0.dp else 1.5.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (item.isDueNow) Modifier.border(1.5.dp, StatusUrgent, RoundedCornerShape(16.dp))
                else Modifier.border(1.dp, OutlineBorder, RoundedCornerShape(16.dp))
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill Form / Color Indicator Pill
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 48.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(pillColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isDueNow) StatusUrgent else MaterialTheme.colorScheme.onSurface
                    )

                    if (item.isDueNow) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StatusUrgentBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("DUE NOW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusUrgent)
                        }
                    } else if (item.status == DoseStatus.SNOOZED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StatusSnoozedBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SNOOZED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusSnoozed)
                        }
                    } else if (isSkipped) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFEE2E2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SKIPPED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    } else if (item.status == DoseStatus.MISSED) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFFEE2E2)).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text("MISSED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.medication.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isTaken) TextMutedLight else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None
                )

                Text(
                    text = "${item.medication.dosage}${if (item.medication.instructions.isNotBlank()) " • " + item.medication.instructions else ""}",
                    fontSize = 13.sp,
                    color = TextSecondaryLight
                )

                // Low inventory stock warning tag
                if (isLowStock && !isTaken) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ ${item.medication.remainingPills} pills left (Refill soon)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusSnoozed
                    )
                }
            }

            // Edit button
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMutedLight, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 1-Tap Circular Taken Checkmark
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isTaken) StatusTaken else Color(0xFFF1F5F9))
                    .border(
                        width = if (isTaken) 0.dp else 2.dp,
                        color = if (isTaken) Color.Transparent else Color(0xFFCBD5E1),
                        shape = CircleShape
                    )
                    .clickable { onToggleTaken() }
            ) {
                if (isTaken) {
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
