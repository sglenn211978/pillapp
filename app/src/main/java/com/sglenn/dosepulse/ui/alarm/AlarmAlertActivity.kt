package com.sglenn.dosepulse.ui.alarm

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sglenn.dosepulse.PillApplication
import com.sglenn.dosepulse.alarm.DoseActionHandler
import com.sglenn.dosepulse.ui.components.SlideToConfirm
import com.sglenn.dosepulse.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class AlarmAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake screen, bypass keyguard, keep screen awake
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val medId = intent.getLongExtra(EXTRA_MED_ID, -1L)
        val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medication Due"
        val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: ""
        val schedDate = intent.getStringExtra(EXTRA_SCHED_DATE) ?: ""
        val schedTime = intent.getStringExtra(EXTRA_SCHED_TIME) ?: ""
        val instructions = intent.getStringExtra(EXTRA_INSTRUCTIONS) ?: ""
        val colorHex = intent.getLongExtra(EXTRA_COLOR_HEX, 0xFF00796B)

        setContent {
            DosePulseTheme(darkTheme = true) {
                AlarmAlertScreen(
                    medicationName = medName,
                    dosage = dosage,
                    scheduledTime = schedTime,
                    instructions = instructions,
                    colorHex = colorHex,
                    onConfirmTaken = {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                DoseActionHandler.take(this@AlarmAlertActivity, PillApplication.instance.repository, medId, schedDate, schedTime)
                            }
                            finishAndRemoveTask()
                        }
                    },
                    onSnooze = { minutes ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                DoseActionHandler.snooze(this@AlarmAlertActivity, PillApplication.instance.repository, medId, schedDate, schedTime, minutes)
                            }
                            finishAndRemoveTask()
                        }
                    },
                    onSkip = { reason ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                DoseActionHandler.skip(this@AlarmAlertActivity, PillApplication.instance.repository, medId, schedDate, schedTime, reason)
                            }
                            finishAndRemoveTask()
                        }
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MED_ID = "extra_med_id"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_SCHED_DATE = "extra_sched_date"
        const val EXTRA_SCHED_TIME = "extra_sched_time"
        const val EXTRA_INSTRUCTIONS = "extra_instructions"
        const val EXTRA_COLOR_HEX = "extra_color_hex"
    }
}

@Composable
fun AlarmAlertScreen(
    medicationName: String,
    dosage: String,
    scheduledTime: String,
    instructions: String,
    colorHex: Long,
    onConfirmTaken: () -> Unit,
    onSnooze: (Int) -> Unit,
    onSkip: (String) -> Unit
) {
    var showSkipDialog by remember { mutableStateOf(false) }

    // Pulsing Animation around Icon
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val pillColor = Color(colorHex)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkSlateBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header: Alarm Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = StatusUrgent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SCHEDULED ALARM • ${com.sglenn.dosepulse.util.TimeFormatHelper.formatDisplayTime(scheduledTime)}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Center: Pulsing Ring + Medication Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Pulsing Icon Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(pillColor.copy(alpha = 0.25f))
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(pillColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bold Medication Details Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = medicationName,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dosage,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrightCyan
                        )

                        if (instructions.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF334155),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = instructions,
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Actions: SlideToConfirm & Snooze / Skip Pills
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Interactive Slide-to-Confirm Taken
                SlideToConfirm(
                    onConfirmed = onConfirmTaken,
                    text = "Slide to Confirm Taken",
                    thumbColor = StatusTaken,
                    trackColor = Color(0xFF1E293B)
                )

                // Snooze & Skip Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSnooze(10) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Snooze 10m", color = Color.White, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onSnooze(15) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Snooze 15m", color = Color.White, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { showSkipDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip", color = Color(0xFFEF4444), fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Skip with Reason Dialog
    if (showSkipDialog) {
        var reason by remember { mutableStateOf("Physician instructed") }
        val reasons = listOf("Physician instructed", "Experiencing side effects", "Forgot medication while away", "Feeling unwell / vomiting")

        AlertDialog(
            onDismissRequest = { showSkipDialog = false },
            title = { Text("Skip this dose?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select reason for adherence records:")
                    reasons.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (reason == item) CyanContainer else Color.Transparent)
                                .padding(8.dp)
                        ) {
                            RadioButton(selected = reason == item, onClick = { reason = item })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(item, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSkipDialog = false
                        onSkip(reason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Confirm Skip")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
