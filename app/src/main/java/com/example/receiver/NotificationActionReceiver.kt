package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.PillApplication
import com.example.alarm.AlarmSoundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val medId = intent.getLongExtra(EXTRA_MED_ID, -1L)
        val schedDate = intent.getStringExtra(EXTRA_SCHED_DATE) ?: ""
        val schedTime = intent.getStringExtra(EXTRA_SCHED_TIME) ?: ""

        // Instantly silence audio and stop alarm service
        AlarmSoundService.stop(context)

        val repository = PillApplication.instance.repository
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_TAKE_MED -> {
                        if (medId != -1L) {
                            repository.markTaken(medId, schedDate, schedTime)
                        }
                    }
                    ACTION_SNOOZE_MED -> {
                        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                        if (medId != -1L) {
                            repository.markSnoozed(medId, schedDate, schedTime)

                            // Schedule snooze exact alarm
                            val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medication"
                            val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: ""
                            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            val triggerTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

                            val snoozeIntent = Intent(context, MedicationAlarmReceiver::class.java).apply {
                                this.action = MedicationAlarmReceiver.ACTION_MEDICATION_ALARM
                                putExtra(AlarmSoundService.EXTRA_MED_ID, medId)
                                putExtra(AlarmSoundService.EXTRA_MED_NAME, medName)
                                putExtra(AlarmSoundService.EXTRA_DOSAGE, dosage)
                                putExtra(AlarmSoundService.EXTRA_SCHED_DATE, schedDate)
                                putExtra(AlarmSoundService.EXTRA_SCHED_TIME, schedTime)
                            }

                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                (medId * 1000 + snoozeMinutes).toInt(),
                                snoozeIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                            } else {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                            }
                        }
                    }
                    ACTION_SKIP_MED -> {
                        val reason = intent.getStringExtra(EXTRA_SKIP_REASON) ?: "Skipped by user"
                        if (medId != -1L) {
                            repository.markSkipped(medId, schedDate, schedTime, reason)
                        }
                    }
                    ACTION_SILENCE_ALARM -> {
                        // Audio already stopped
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TAKE_MED = "com.example.ACTION_TAKE_MED"
        const val ACTION_SNOOZE_MED = "com.example.ACTION_SNOOZE_MED"
        const val ACTION_SKIP_MED = "com.example.ACTION_SKIP_MED"
        const val ACTION_SILENCE_ALARM = "com.example.ACTION_SILENCE_ALARM"

        const val EXTRA_MED_ID = "extra_med_id"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_SCHED_DATE = "extra_sched_date"
        const val EXTRA_SCHED_TIME = "extra_sched_time"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_SKIP_REASON = "extra_skip_reason"
    }
}
