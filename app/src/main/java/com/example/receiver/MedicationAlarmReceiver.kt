package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.example.alarm.AlarmSoundService
import com.example.ui.alarm.AlarmAlertActivity

class MedicationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "PillPulse:AlarmWakeLock"
        )
        wakeLock.acquire(60 * 1000L) // 1 minute max

        val medId = intent.getLongExtra(AlarmSoundService.EXTRA_MED_ID, -1L)
        val medName = intent.getStringExtra(AlarmSoundService.EXTRA_MED_NAME) ?: "Medication Due"
        val dosage = intent.getStringExtra(AlarmSoundService.EXTRA_DOSAGE) ?: ""
        val schedDate = intent.getStringExtra(AlarmSoundService.EXTRA_SCHED_DATE) ?: ""
        val schedTime = intent.getStringExtra(AlarmSoundService.EXTRA_SCHED_TIME) ?: ""
        val instructions = intent.getStringExtra(AlarmSoundService.EXTRA_INSTRUCTIONS) ?: ""
        val colorHex = intent.getLongExtra(AlarmSoundService.EXTRA_COLOR_HEX, 0xFF00796B)
        val ringtoneUri = intent.getStringExtra(AlarmSoundService.EXTRA_RINGTONE_URI)

        // 1. Start persistent looping AlarmSoundService
        val serviceIntent = Intent(context, AlarmSoundService::class.java).apply {
            action = AlarmSoundService.ACTION_START_ALARM
            putExtra(AlarmSoundService.EXTRA_MED_ID, medId)
            putExtra(AlarmSoundService.EXTRA_MED_NAME, medName)
            putExtra(AlarmSoundService.EXTRA_DOSAGE, dosage)
            putExtra(AlarmSoundService.EXTRA_SCHED_DATE, schedDate)
            putExtra(AlarmSoundService.EXTRA_SCHED_TIME, schedTime)
            putExtra(AlarmSoundService.EXTRA_INSTRUCTIONS, instructions)
            putExtra(AlarmSoundService.EXTRA_COLOR_HEX, colorHex)
            putExtra(AlarmSoundService.EXTRA_RINGTONE_URI, ringtoneUri)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // 2. Launch Full-Screen Lock Screen Activity directly
        val activityIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmAlertActivity.EXTRA_MED_ID, medId)
            putExtra(AlarmAlertActivity.EXTRA_MED_NAME, medName)
            putExtra(AlarmAlertActivity.EXTRA_DOSAGE, dosage)
            putExtra(AlarmAlertActivity.EXTRA_SCHED_DATE, schedDate)
            putExtra(AlarmAlertActivity.EXTRA_SCHED_TIME, schedTime)
            putExtra(AlarmAlertActivity.EXTRA_INSTRUCTIONS, instructions)
            putExtra(AlarmAlertActivity.EXTRA_COLOR_HEX, colorHex)
        }
        context.startActivity(activityIntent)
    }

    companion object {
        const val ACTION_MEDICATION_ALARM = "com.example.ACTION_MEDICATION_ALARM"
        const val ACTION_TEST_ALARM = "com.example.ACTION_TEST_ALARM"
    }
}
