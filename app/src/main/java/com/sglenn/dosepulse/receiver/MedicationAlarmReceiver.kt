package com.sglenn.dosepulse.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.sglenn.dosepulse.PillApplication
import com.sglenn.dosepulse.alarm.AlarmSoundService
import com.sglenn.dosepulse.alarm.MedicationAlarmScheduler
import com.sglenn.dosepulse.data.DoseStatus
import com.sglenn.dosepulse.data.Medication
import com.sglenn.dosepulse.ui.alarm.AlarmAlertActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DosePulse:AlarmWakeLock")
            try {
                val medId = intent.getLongExtra(AlarmSoundService.EXTRA_MED_ID, -1L)
                val date = intent.getStringExtra(AlarmSoundService.EXTRA_SCHED_DATE).orEmpty()
                val time = intent.getStringExtra(AlarmSoundService.EXTRA_SCHED_TIME).orEmpty()
                val isTest = intent.action == ACTION_TEST_ALARM
                val isSnooze = intent.getBooleanExtra(AlarmSoundService.EXTRA_IS_SNOOZE, false)
                val app = context.applicationContext as PillApplication
                val repo = app.repository
                var currentMedication: Medication? = null

                if (!isTest) {
                    val medication = repo.getMedicationById(medId)
                    if (medication == null || !medication.isActive) return@launch
                    currentMedication = medication
                    // Always rebuild the next normal alarm, even when this pending intent is stale.
                    MedicationAlarmScheduler.scheduleNextAlarm(context, medication)
                    if (time !in medication.scheduledTimes) return@launch
                    val scheduledDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return@launch
                    if (!MedicationAlarmScheduler.isScheduledOn(medication, scheduledDate)) return@launch
                    if (!isSnooze && date != LocalDate.now().toString()) return@launch
                    if (!isSnooze && !isScheduledTimeDue(time)) return@launch

                    val log = repo.getDoseLog(medId, date, time)
                    if (isSnooze) {
                        if (log?.status != DoseStatus.SNOOZED || (log.snoozedUntil ?: Long.MAX_VALUE) > System.currentTimeMillis()) return@launch
                    } else if (log != null && log.status != DoseStatus.PENDING) {
                        return@launch
                    }
                }

                val medName = currentMedication?.name ?: intent.getStringExtra(AlarmSoundService.EXTRA_MED_NAME) ?: "Medication Due"
                val dosage = currentMedication?.dosage ?: intent.getStringExtra(AlarmSoundService.EXTRA_DOSAGE).orEmpty()
                val instructions = currentMedication?.instructions ?: intent.getStringExtra(AlarmSoundService.EXTRA_INSTRUCTIONS).orEmpty()
                val colorHex = currentMedication?.colorHex ?: intent.getLongExtra(AlarmSoundService.EXTRA_COLOR_HEX, 0xFF00796B)
                val ringtoneUri = currentMedication?.ringtoneUri ?: intent.getStringExtra(AlarmSoundService.EXTRA_RINGTONE_URI)

                wakeLock.acquire(60_000L)
                val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
                    action = AlarmSoundService.ACTION_START_ALARM
                    putExtra(AlarmSoundService.EXTRA_MED_ID, medId)
                    putExtra(AlarmSoundService.EXTRA_MED_NAME, medName)
                    putExtra(AlarmSoundService.EXTRA_DOSAGE, dosage)
                    putExtra(AlarmSoundService.EXTRA_SCHED_DATE, date)
                    putExtra(AlarmSoundService.EXTRA_SCHED_TIME, time)
                    putExtra(AlarmSoundService.EXTRA_INSTRUCTIONS, instructions)
                    putExtra(AlarmSoundService.EXTRA_COLOR_HEX, colorHex)
                    putExtra(AlarmSoundService.EXTRA_RINGTONE_URI, ringtoneUri)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent)
                else context.startService(serviceIntent)

                val activityIntent = Intent(context, AlarmAlertActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(AlarmAlertActivity.EXTRA_MED_ID, medId)
                    putExtra(AlarmAlertActivity.EXTRA_MED_NAME, medName)
                    putExtra(AlarmAlertActivity.EXTRA_DOSAGE, dosage)
                    putExtra(AlarmAlertActivity.EXTRA_SCHED_DATE, date)
                    putExtra(AlarmAlertActivity.EXTRA_SCHED_TIME, time)
                    putExtra(AlarmAlertActivity.EXTRA_INSTRUCTIONS, instructions)
                    putExtra(AlarmAlertActivity.EXTRA_COLOR_HEX, colorHex)
                }
                context.startActivity(activityIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                pending.finish()
            }
        }
    }

    private fun isScheduledTimeDue(time: String): Boolean {
        val scheduled = runCatching { LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull() ?: return false
        return LocalTime.now() >= scheduled
    }

    companion object {
        const val ACTION_MEDICATION_ALARM = "com.sglenn.dosepulse.ACTION_MEDICATION_ALARM"
        const val ACTION_TEST_ALARM = "com.sglenn.dosepulse.ACTION_TEST_ALARM"
    }
}
