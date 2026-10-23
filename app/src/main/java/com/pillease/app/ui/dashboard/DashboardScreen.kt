package com.pillease.app.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pillease.app.data.DailyDoseItem
import com.pillease.app.data.Medication
import com.pillease.app.ui.theme.*
import com.pillease.app.ui.viewmodel.MedicationViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MedicationViewModel,
    onNavigateToAddEdit: (Long) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dailyDoses by viewModel.dailyDoses.collectAsState()
    val asNeededMeds by viewModel.asNeededMeds.collectAsState()

    val isToday = selectedDate == LocalDate.now()
    val dueCount = dailyDoses.count { it.isDueNow }
    val takenCount = dailyDoses.count { it.isTaken }
    val totalCount = dailyDoses.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TealPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PillEase",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Dose History",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoftBackground
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAddEdit(-1L) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Med", fontWeight = FontWeight.SemiBold) },
                containerColor = TealPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        },
        containerColor = SoftBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // DATE NAVIGATOR (Yesterday < TODAY > Tomorrow)
            item {
                DateNavigator(
                    selectedDate = selectedDate,
                    onPreviousDay = { viewModel.setSelectedDate(selectedDate.minusDays(1)) },
                    onNextDay = { viewModel.setSelectedDate(selectedDate.plusDays(1)) },
                    onToday = { viewModel.setSelectedDate(LocalDate.now()) }
                )
            }

            // DAILY PROGRESS STATS
            if (totalCount > 0) {
                item {
                    ProgressSummaryCard(
                        takenCount = takenCount,
                        totalCount = totalCount,
                        isToday = isToday
                    )
                }
            }

            // TAKE ALL BANNER (If multiple due now)
            if (dueCount > 0) {
                item {
                    TakeAllBanner(
                        dueCount = dueCount,
                        onTakeAllClicked = { viewModel.takeAllDueDoses() }
                    )
                }
            }

            // EMPTY STATE (No medications at all)
            if (totalCount == 0 && asNeededMeds.isEmpty()) {
                item {
                    EmptyStateCard(onAddMedication = { onNavigateToAddEdit(-1L) })
                }
            }

            // SCHEDULED DOSES LIST
            if (totalCount > 0) {
                item {
                    Text(
                        text = if (isToday) "Today's Schedule" else selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                items(dailyDoses, key = { "${it.medication.id}_${it.scheduledDateTime}" }) { item ->
                    MedicationCard(
                        item = item,
                        onToggleTaken = { viewModel.toggleDose(item) },
                        onSkip = { viewModel.markDoseSkipped(item, "Skipped from app") },
                        onEdit = { onNavigateToAddEdit(item.medication.id) }
                    )
                }
            }

            // AS NEEDED (PRN) SECTION
            if (asNeededMeds.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "As Needed (PRN)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }

                items(asNeededMeds, key = { "prn_${it.id}" }) { med ->
                    AsNeededCard(
                        medication = med,
                        onLogDose = { viewModel.logAsNeeded(med) },
                        onEdit = { onNavigateToAddEdit(med.id) }
                    )
                }
            }

            // Extra padding at bottom for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun DateNavigator(
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = selectedDate == LocalDate.now()
    val dateText = when {
        isToday -> "Today, " + selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))
        selectedDate == LocalDate.now().minusDays(1) -> "Yesterday, " + selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))
        selectedDate == LocalDate.now().plusDays(1) -> "Tomorrow, " + selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))
        else -> selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = TextPrimary)
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isToday) TealContainer else Color.White,
            modifier = Modifier.clickable { onToday() }
        ) {
            Text(
                text = dateText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (isToday) TealDark else TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        IconButton(
            onClick = onNextDay,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = TextPrimary)
        }
    }
}

@Composable
fun ProgressSummaryCard(
    takenCount: Int,
    totalCount: Int,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (totalCount > 0) takenCount.toFloat() / totalCount.toFloat() else 0f
    val isComplete = takenCount == totalCount && totalCount > 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isComplete) "All caught up! 🎉" else "$takenCount of $totalCount taken",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isComplete) StatusGreen else TextPrimary
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isComplete) StatusGreen else TealPrimary,
                trackColor = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
fun AsNeededCard(
    medication: Medication,
    onLogDose: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medication.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${medication.dosage}${if (medication.instructions.isNotBlank()) " • " + medication.instructions else ""}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onLogDose,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealContainer,
                    contentColor = TealDark
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Dose", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    onAddMedication: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(TealContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = null,
                    tint = TealDark,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No medications scheduled",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Add your medications with daily times, cycles, or as-needed rules to get 1-tap reminders.",
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAddMedication,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Your First Medication", fontWeight = FontWeight.Bold)
            }
        }
    }
}
