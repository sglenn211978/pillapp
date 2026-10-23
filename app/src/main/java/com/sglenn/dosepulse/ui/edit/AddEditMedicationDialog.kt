package com.sglenn.dosepulse.ui.edit

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sglenn.dosepulse.data.Medication
import com.sglenn.dosepulse.data.ScheduleType
import com.sglenn.dosepulse.ui.theme.*
import com.sglenn.dosepulse.ui.viewmodel.DashboardViewModel
import com.sglenn.dosepulse.util.TimeFormatHelper
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationScreen(
    medicationId: Long,
    viewModel: DashboardViewModel,
    onNavigateBack: () -> Unit,
    initialName: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allReferences by viewModel.allReferences.collectAsState(initial = emptyList())
    val themeState by ThemeManager.themeState.collectAsState()

    var name by remember(medicationId, initialName) { mutableStateOf(initialName) }
    var dosage by remember { mutableStateOf("") }
    var iconType by remember { mutableStateOf("tablet") }
    var instructions by remember { mutableStateOf("") }
    var remainingPills by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(PillColorPalette[0]) }
    var times by remember { mutableStateOf(emptyList<String>()) }
    var existingMedication by remember { mutableStateOf<Medication?>(null) }

    // Match local catalog entries by name without filling a dose recommendation.
    val suggestedRefs = remember(name, allReferences) {
        if (name.length >= 2) {
            allReferences.filter { it.name.contains(name, ignoreCase = true) }.take(3)
        } else emptyList()
    }

    LaunchedEffect(medicationId) {
        if (medicationId != -1L) {
            val existing = viewModel.getMedicationById(medicationId)
            if (existing != null) {
                existingMedication = existing
                name = existing.name
                dosage = existing.dosage
                iconType = existing.iconType
                instructions = existing.instructions
                remainingPills = existing.remainingPills.toString()
                selectedColor = existing.colorHex
                times = existing.scheduledTimes
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Please enter medication name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (medicationId != -1L && existingMedication == null) {
                            Toast.makeText(context, "Medication could not be loaded. Go back and try again.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (dosage.isBlank()) {
                            Toast.makeText(context, "Please enter the prescribed dose.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val inventory = remainingPills.toIntOrNull()
                        if (inventory == null || inventory < 0) {
                            Toast.makeText(context, "Enter a non-negative whole pill count.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val normalizedTimes = times.map(String::trim)
                        val validTimes = normalizedTimes.all {
                            runCatching { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm")) }.isSuccess
                        }
                        if (normalizedTimes.isEmpty() || !validTimes || normalizedTimes.distinct().size != normalizedTimes.size) {
                            Toast.makeText(context, "Add at least one valid, unique dose time.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val existing = existingMedication
                        val med = if (existing != null) {
                            existing.copy(
                                name = name.trim(), dosage = dosage.trim(), iconType = iconType,
                                colorHex = selectedColor, scheduledTimes = normalizedTimes.sorted(),
                                instructions = instructions.trim(), remainingPills = inventory
                            )
                        } else Medication(
                            name = name.trim(), dosage = dosage.trim(), iconType = iconType,
                            colorHex = selectedColor, scheduleType = ScheduleType.DAILY,
                            scheduledTimes = normalizedTimes.sorted(), instructions = instructions.trim(),
                            remainingPills = inventory, lowStockThreshold = 7
                        )

                        viewModel.saveMedication(context, med) { scheduled ->
                            val notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
                            val message = when {
                                !scheduled && !notificationsAllowed -> "Saved, but allow notifications and exact alarms in Android settings."
                                !scheduled -> "Saved, but allow exact alarms in Android settings for reminders."
                                !notificationsAllowed -> "Saved and scheduled, but allow notifications in Android settings for alerts."
                                else -> "Medication saved and reminders scheduled."
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
        containerColor = MaterialTheme.colorScheme.background
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Medication Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (initialName.isNotBlank() && medicationId == -1L) {
                        Text("Name filled from the reference catalog. Enter the dose and times from your prescription label.", fontSize = 12.sp, color = TextSecondaryLight)
                    }

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
                        Text("Reference catalog matches:", fontSize = 11.sp, color = TextSecondaryLight)
                        suggestedRefs.forEach { ref ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        name = ref.name.substringBefore(" (")
                                        iconType = ref.defaultForm
                                        selectedColor = ref.suggestedColorHex
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${ref.name} (${ref.category})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Badge Color", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
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
                                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                                    .clickable { selectedColor = hex }
                            )
                        }
                    }
                }
            }

            // Alarm audio follows the device's configured alarm sound.
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reminder audio uses your phone’s default alarm sound and vibration settings.", fontSize = 13.sp, color = TextSecondaryLight)
                }
            }

            // INVENTORY & REFILL TRACKING
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pill Inventory & Low-Stock Alerts", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)

                    OutlinedTextField(
                        value = remainingPills,
                        onValueChange = { remainingPills = it },
                        label = { Text("Remaining pills in bottle") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text("PillPulse automatically decrements this count each time a dose is confirmed taken.", fontSize = 12.sp, color = TextSecondaryLight)
                }
            }

            // SCHEDULE TIMES WITH EXPLICIT 12-HOUR AM / PM CONTROLS
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dose Times (AM / PM)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Tap to edit or toggle AM/PM", fontSize = 11.sp, color = TextSecondaryLight)
                    }

                    if (times.isEmpty()) {
                        Text("Add at least one reminder time from your prescription instructions.", fontSize = 12.sp, color = TextSecondaryLight)
                    }

                    // Quick Preset Chips for 1-tap addition
                    Text("Quick Presets:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondaryLight)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickTimePresetChip("🌅 Morning (8:00 AM)") {
                            if (!times.contains("08:00")) times = (times + "08:00").sorted()
                        }
                        QuickTimePresetChip("☀️ Noon (12:00 PM)") {
                            if (!times.contains("12:00")) times = (times + "12:00").sorted()
                        }
                        QuickTimePresetChip("🌆 Evening (6:00 PM)") {
                            if (!times.contains("18:00")) times = (times + "18:00").sorted()
                        }
                        QuickTimePresetChip("🌙 Bedtime (10:00 PM)") {
                            if (!times.contains("22:00")) times = (times + "22:00").sorted()
                        }
                    }

                    // Configured Time List
                    times.forEachIndexed { index, timeStr ->
                        val (hour, minute) = TimeFormatHelper.parseHourMinute(timeStr)
                        val isPm = hour >= 12
                        val displayTime = TimeFormatHelper.formatDisplayTime(timeStr, themeState.use24HourFormat)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Clickable Display Time (Opens 12-hour AM/PM TimePickerDialog)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        TimePickerDialog(
                                            context,
                                            { _, pickedHour, pickedMinute ->
                                                val formatted = TimeFormatHelper.to24HourString(pickedHour, pickedMinute)
                                                times = times.toMutableList().also { it[index] = formatted }
                                            },
                                            hour,
                                            minute,
                                            false // FALSE = 12-Hour view with AM / PM selector!
                                        ).show()
                                    }
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = displayTime,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 1-Tap AM / PM Quick Toggle Button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPm) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                                    modifier = Modifier.clickable {
                                        times = times.toMutableList().also {
                                            it[index] = TimeFormatHelper.toggleAmPm(timeStr)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (isPm) "PM" else "AM",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isPm) Color.White else TextPrimaryLight,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                if (times.size > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { times = times.toMutableList().also { it.removeAt(index) } },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMutedLight)
                                    }
                                }
                            }
                        }
                    }

                    // Add Custom Dose Time Button
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, pickedHour, pickedMinute ->
                                    val formatted = TimeFormatHelper.to24HourString(pickedHour, pickedMinute)
                                    if (!times.contains(formatted)) times = (times + formatted).sorted()
                                },
                                12,
                                0,
                                false // FALSE = 12-Hour view with explicit AM / PM!
                            ).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Custom Time (Pick AM/PM)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun QuickTimePresetChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
