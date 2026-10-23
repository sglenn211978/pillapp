package com.example.ui.edit

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
import com.example.data.Medication
import com.example.data.MedicationReference
import com.example.data.ScheduleType
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationScreen(
    medicationId: Long,
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allReferences by viewModel.allReferences.collectAsState(initial = emptyList())

    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var iconType by remember { mutableStateOf("tablet") }
    var instructions by remember { mutableStateOf("") }
    var remainingPills by remember { mutableStateOf("30") }
    var selectedColor by remember { mutableLongStateOf(PillColorPalette[0]) }
    var times by remember { mutableStateOf(listOf("08:00")) }
    var selectedRingtoneName by remember { mutableStateOf("Default System Alarm") }

    // Auto-complete suggestion matches from 50+ clinical database
    val suggestedRefs = remember(name, allReferences) {
        if (name.length >= 2) {
            allReferences.filter { it.name.contains(name, ignoreCase = true) }.take(3)
        } else emptyList()
    }

    LaunchedEffect(medicationId) {
        if (medicationId != -1L) {
            val meds = viewModel.allActiveMedications.first()
            val existing = meds.find { it.id == medicationId }
            if (existing != null) {
                name = existing.name
                dosage = existing.dosage
                iconType = existing.iconType
                instructions = existing.instructions
                remainingPills = existing.remainingPills.toString()
                selectedColor = existing.colorHex
                times = if (existing.scheduledTimes.isNotEmpty()) existing.scheduledTimes else listOf("08:00")
            }
        }
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
                                val med = Medication(id = medicationId, name = name, dosage = dosage)
                                viewModel.deleteMedication(context, med)
                                Toast.makeText(context, "Medication deleted", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SoftMistSlate)
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceWhite,
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
                            dosage = dosage.trim().ifBlank { "Standard dose" },
                            iconType = iconType,
                            colorHex = selectedColor,
                            scheduleType = ScheduleType.DAILY,
                            scheduledTimes = times,
                            instructions = instructions.trim(),
                            remainingPills = remainingPills.toIntOrNull() ?: 30,
                            lowStockThreshold = 7,
                            isActive = true
                        )

                        viewModel.saveMedication(context, med) {
                            Toast.makeText(context, "Medication saved with active alarm schedule!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MedicalTeal),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                ) {
                    Text("Save & Activate Alarms", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = SoftMistSlate
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // MEDICATION DETAILS & AUTOCOMPLETE
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Medication Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimaryLight)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Medication Name (e.g. Lisinopril)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Auto-complete suggestions pills
                    if (suggestedRefs.isNotEmpty()) {
                        Text("Suggested from clinical database:", fontSize = 11.sp, color = TextSecondaryLight)
                        suggestedRefs.forEach { ref ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CyanContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        name = ref.name.substringBefore(" (")
                                        if (ref.commonDosages.isNotEmpty()) dosage = ref.commonDosages.first()
                                        instructions = ref.intakeGuidance
                                        iconType = ref.defaultForm
                                        selectedColor = ref.suggestedColorHex
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MedicalTeal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${ref.name} (${ref.category})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MedicalTealDark
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Dosage / Strength (e.g. 10 mg, 1 capsule)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Instructions (e.g. With food and water)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // COLOR PALETTE
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Badge Color", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimaryLight)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PillColorPalette.forEach { hex ->
                            val color = Color(hex)
                            val isSelected = selectedColor == hex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, TextPrimaryLight, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            }

            // RINGTONE & ALARM CUSTOMIZATION
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MedicalTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Alarm Ringtone & Audio", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimaryLight)
                    }

                    val tones = listOf("Default System Alarm", "Gentle Chime", "Clinical Beep", "Urgent Siren")
                    tones.forEach { tone ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedRingtoneName == tone) CyanContainer else Color(0xFFF8FAFC))
                                .clickable { selectedRingtoneName = tone }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            RadioButton(selected = selectedRingtoneName == tone, onClick = { selectedRingtoneName = tone })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(tone, fontSize = 14.sp, fontWeight = if (selectedRingtoneName == tone) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // INVENTORY & REFILL TRACKING
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pill Inventory & Low-Stock Alerts", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimaryLight)

                    OutlinedTextField(
                        value = remainingPills,
                        onValueChange = { remainingPills = it },
                        label = { Text("Remaining pills in bottle") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text("PillPulse automatically decrements this count each time a dose is confirmed taken.", fontSize = 12.sp, color = TextSecondaryLight)
                }
            }

            // SCHEDULE TIMES
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Alarm Scheduled Times", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimaryLight)

                    times.forEachIndexed { index, timeStr ->
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
                                color = MedicalTeal,
                                modifier = Modifier.clickable {
                                    val parts = timeStr.split(":")
                                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                    TimePickerDialog(context, { _, hour, minute ->
                                        val formatted = String.format("%02d:%02d", hour, minute)
                                        times = times.toMutableList().also { it[index] = formatted }
                                    }, h, m, true).show()
                                }
                            )

                            if (times.size > 1) {
                                IconButton(onClick = { times = times.toMutableList().also { it.removeAt(index) } }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMutedLight)
                                }
                            }
                        }
                    }

                    TextButton(onClick = {
                        TimePickerDialog(context, { _, hour, minute ->
                            val formatted = String.format("%02d:%02d", hour, minute)
                            if (!times.contains(formatted)) times = (times + formatted).sorted()
                        }, 12, 0, true).show()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Another Dose Time", color = MedicalTeal, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
