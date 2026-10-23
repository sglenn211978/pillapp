package com.pillease.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pillease.app.PillApplication
import com.pillease.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val scheduledDateTime = intent.getStringExtra(EXTRA_SCHEDULED_DATETIME) ?: ""

        if (notificationId != -1) {
            NotificationHelper.cancelNotification(context, notificationId)
        }

        if (medicationId == -1L || scheduledDateTime.isBlank()) return

        val repository = PillApplication.instance.repository
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_TAKE_MED -> {
                        repository.markDoseTaken(medicationId, scheduledDateTime)
                    }
                    ACTION_SNOOZE_MED -> {
                        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: "Medication"
                        val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: ""

                        // Schedule a snooze alarm for 15 minutes from now
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val snoozeTriggerAtMillis = System.currentTimeMillis() + (15 * 60 * 1000)

                        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                            this.action = AlarmReceiver.ACTION_MEDICATION_ALARM
                            putExtra(AlarmReceiver.EXTRA_MEDICATION_ID, medicationId)
                            putExtra(AlarmReceiver.EXTRA_MEDICATION_NAME, medicationName)
                            putExtra(AlarmReceiver.EXTRA_DOSAGE, dosage)
                            putExtra(AlarmReceiver.EXTRA_SCHEDULED_DATETIME, scheduledDateTime)
                        }

                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            notificationId,
                            alarmIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                snoozeTriggerAtMillis,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                snoozeTriggerAtMillis,
                                pendingIntent
                            )
                        }
                    }
                    ACTION_SKIP_MED -> {
                        repository.markDoseSkipped(medicationId, scheduledDateTime, reason = "Skipped from notification")
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
        const val ACTION_TAKE_MED = "com.pillease.app.ACTION_TAKE_MED"
        const val ACTION_SNOOZE_MED = "com.pillease.app.ACTION_SNOOZE_MED"
        const val ACTION_SKIP_MED = "com.pillease.app.ACTION_SKIP_MED"
        const val ACTION_TAKE_ALL = "com.pillease.app.ACTION_TAKE_ALL"

        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_SCHEDULED_DATETIME = "extra_scheduled_datetime"
    }
}
