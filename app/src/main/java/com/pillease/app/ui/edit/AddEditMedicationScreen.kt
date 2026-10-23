package com.pillease.app.ui.edit

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pillease.app.data.Medication
import com.pillease.app.data.ScheduleType
import com.pillease.app.ui.theme.*
import com.pillease.app.ui.viewmodel.MedicationViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationScreen(
    medicationId: Long,
    viewModel: MedicationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var scheduleType by remember { mutableStateOf(ScheduleType.DAILY) }
    var timesOfDay by remember { mutableStateOf(listOf("08:00")) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) } // 1=Mon .. 7=Sun
    var cycleDaysOn by remember { mutableStateOf("21") }
    var cycleDaysOff by remember { mutableStateOf("7") }
    var cycleStartDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var minHoursPRN by remember { mutableStateOf("4") }
    var selectedColor by remember { mutableStateOf(PillPalette[0]) }
    var isLoaded by remember { mutableStateOf(false) }

    // Load existing medication if editing
    LaunchedEffect(medicationId) {
        if (medicationId != -1L) {
            val meds = viewModel.allActiveMedications.first()
            val existing = meds.find { it.id == medicationId }
            if (existing != null) {
                name = existing.name
                dosage = existing.dosage
                instructions = existing.instructions
                scheduleType = existing.scheduleType
                timesOfDay = if (existing.timesOfDay.isNotEmpty()) existing.timesOfDay else listOf("08:00")
                selectedDays = existing.daysOfWeek.toSet().ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
                cycleDaysOn = existing.cycleDaysOn.toString()
                cycleDaysOff = existing.cycleDaysOff.toString()
                cycleStartDate = existing.cycleStartDate.ifBlank { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
                minHoursPRN = existing.minHoursBetweenDoses.toString()
                selectedColor = existing.colorHex
            }
        }
        isLoaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (medicationId == -1L) "Add Medication" else "Edit Medication",
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
                    if (medicationId != -1L) {
                        IconButton(
                            onClick = {
                                val medToDelete = Medication(id = medicationId, name = name, dosage = dosage)
                                viewModel.deleteMedication(context, medToDelete)
                                Toast.makeText(context, "Medication removed", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SoftBackground)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Please enter medication name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val med = Medication(
                            id = if (medicationId == -1L) 0L else medicationId,
                            name = name.trim(),
                            dosage = dosage.trim(),
                            instructions = instructions.trim(),
                            scheduleType = scheduleType,
                            timesOfDay = if (scheduleType == ScheduleType.AS_NEEDED_PRN) emptyList() else timesOfDay,
                            daysOfWeek = if (scheduleType == ScheduleType.SPECIFIC_DAYS) selectedDays.toList().sorted() else emptyList(),
                            cycleDaysOn = cycleDaysOn.toIntOrNull() ?: 21,
                            cycleDaysOff = cycleDaysOff.toIntOrNull() ?: 7,
                            cycleStartDate = cycleStartDate,
                            minHoursBetweenDoses = minHoursPRN.toIntOrNull() ?: 4,
                            colorHex = selectedColor,
                            isActive = true
                        )

                        viewModel.saveMedication(context, med) {
                            Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                ) {
                    Text("Save Medication", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = SoftBackground
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // NAME & DOSAGE CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Medication Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Medication Name (e.g. Sertraline)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Dosage (e.g. 50mg, 1 tablet)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Instructions (e.g. With food, Before bed)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // COLOR SELECTOR CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Color Tag", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PillPalette.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, TextPrimary, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            }

            // SCHEDULE TYPE CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Schedule Type", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)

                    // Type Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScheduleTypeChip(
                            label = "Daily",
                            selected = scheduleType == ScheduleType.DAILY,
                            onClick = { scheduleType = ScheduleType.DAILY }
                        )
                        ScheduleTypeChip(
                            label = "Specific Days",
                            selected = scheduleType == ScheduleType.SPECIFIC_DAYS,
                            onClick = { scheduleType = ScheduleType.SPECIFIC_DAYS }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScheduleTypeChip(
                            label = "Cycle (e.g. 21/7)",
                            selected = scheduleType == ScheduleType.CYCLE_DAYS,
                            onClick = { scheduleType = ScheduleType.CYCLE_DAYS }
                        )
                        ScheduleTypeChip(
                            label = "As Needed (PRN)",
                            selected = scheduleType == ScheduleType.AS_NEEDED_PRN,
                            onClick = { scheduleType = ScheduleType.AS_NEEDED_PRN }
                        )
                    }

                    // SPECIFIC DAYS SELECTOR
                    if (scheduleType == ScheduleType.SPECIFIC_DAYS) {
                        Text("Repeat On", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        val days = listOf("M" to 1, "T" to 2, "W" to 3, "T" to 4, "F" to 5, "S" to 6, "S" to 7)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            days.forEach { (label, dayNum) ->
                                val isSelected = selectedDays.contains(dayNum)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) TealPrimary else Color(0xFFF1F5F9))
                                        .clickable {
                                            selectedDays = if (isSelected) {
                                                if (selectedDays.size > 1) selectedDays - dayNum else selectedDays
                                            } else {
                                                selectedDays + dayNum
                                            }
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // CYCLE SELECTOR
                    if (scheduleType == ScheduleType.CYCLE_DAYS) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = cycleDaysOn,
                                onValueChange = { cycleDaysOn = it },
                                label = { Text("Days Active") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = cycleDaysOff,
                                onValueChange = { cycleDaysOff = it },
                                label = { Text("Days Off") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // TIMES OF DAY SELECTOR (For non-PRN)
                    if (scheduleType != ScheduleType.AS_NEEDED_PRN) {
                        Text("Reminder Times", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                        timesOfDay.forEachIndexed { index, timeStr ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timeStr,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary,
                                    modifier = Modifier.clickable {
                                        val parts = timeStr.split(":")
                                        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                        val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0

                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                val formatted = String.format("%02d:%02d", hour, minute)
                                                timesOfDay = timesOfDay.toMutableList().also { it[index] = formatted }
                                            },
                                            initialHour,
                                            initialMin,
                                            true
                                        ).show()
                                    }
                                )

                                if (timesOfDay.size > 1) {
                                    IconButton(
                                        onClick = {
                                            timesOfDay = timesOfDay.toMutableList().also { it.removeAt(index) }
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove Time", tint = TextMuted)
                                    }
                                }
                            }
                        }

                        // Add another dose time
                        TextButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        val formatted = String.format("%02d:%02d", hour, minute)
                                        if (!timesOfDay.contains(formatted)) {
                                            timesOfDay = (timesOfDay + formatted).sorted()
                                        }
                                    },
                                    12,
                                    0,
                                    true
                                ).show()
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Another Time", color = TealPrimary, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // PRN Safety Cooldown
                        OutlinedTextField(
                            value = minHoursPRN,
                            onValueChange = { minHoursPRN = it },
                            label = { Text("Minimum hours between doses (Safety timer)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ScheduleTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) TealPrimary else Color(0xFFF1F5F9),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}
