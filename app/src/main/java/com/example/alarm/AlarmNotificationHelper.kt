package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.receiver.NotificationActionReceiver
import com.example.ui.alarm.AlarmAlertActivity

object AlarmNotificationHelper {

    const val CHANNEL_ID = "pillpulse_med_alarms_v3"
    const val CHANNEL_NAME = "Medication Alarms"
    const val MISSED_CHANNEL_ID = "pillpulse_missed_doses"
    const val MISSED_CHANNEL_NAME = "Missed Medication Alerts"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Critical Alarm Channel
            val alarmChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Unignorable full-screen alarms when medications are due"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(alarmChannel)

            // Missed Dose Channel
            val missedChannel = NotificationChannel(
                MISSED_CHANNEL_ID,
                MISSED_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for doses that were not confirmed"
                setShowBadge(true)
            }
            manager.createNotificationChannel(missedChannel)
        }
    }

    fun buildAlarmNotification(
        context: Context,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        scheduledDate: String,
        scheduledTime: String,
        instructions: String
    ): Notification {
        createChannels(context)

        // 1. Full-Screen Intent to AlarmAlertActivity (wakes screen / displays over lock screen)
        val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmAlertActivity.EXTRA_MED_ID, medicationId)
            putExtra(AlarmAlertActivity.EXTRA_MED_NAME, medicationName)
            putExtra(AlarmAlertActivity.EXTRA_DOSAGE, dosage)
            putExtra(AlarmAlertActivity.EXTRA_SCHED_DATE, scheduledDate)
            putExtra(AlarmAlertActivity.EXTRA_SCHED_TIME, scheduledTime)
            putExtra(AlarmAlertActivity.EXTRA_INSTRUCTIONS, instructions)
        }

        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            medicationId.toInt() * 100 + 1,
            fullScreenIntent,
            pendingFlags
        )

        // 2. Action: "Taken"
        val takeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TAKE_MED
            putExtra(NotificationActionReceiver.EXTRA_MED_ID, medicationId)
            putExtra(NotificationActionReceiver.EXTRA_SCHED_DATE, scheduledDate)
            putExtra(NotificationActionReceiver.EXTRA_SCHED_TIME, scheduledTime)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.toInt() * 100 + 2,
            takeIntent,
            pendingFlags
        )

        // 3. Action: "Snooze 10m"
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE_MED
            putExtra(NotificationActionReceiver.EXTRA_MED_ID, medicationId)
            putExtra(NotificationActionReceiver.EXTRA_MED_NAME, medicationName)
            putExtra(NotificationActionReceiver.EXTRA_DOSAGE, dosage)
            putExtra(NotificationActionReceiver.EXTRA_SCHED_DATE, scheduledDate)
            putExtra(NotificationActionReceiver.EXTRA_SCHED_TIME, scheduledTime)
            putExtra(NotificationActionReceiver.EXTRA_SNOOZE_MINUTES, 10)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.toInt() * 100 + 3,
            snoozeIntent,
            pendingFlags
        )

        // 4. Action: "Silence"
        val silenceIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SILENCE_ALARM
        }
        val silencePendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.toInt() * 100 + 4,
            silenceIntent,
            pendingFlags
        )

        val subtext = if (instructions.isNotBlank()) "$dosage • $instructions" else dosage

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setContentTitle("Dose Due: $medicationName")
            .setContentText(subtext)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(R.drawable.ic_check, "✓ Taken", takePendingIntent)
            .addAction(0, "⏰ Snooze 10m", snoozePendingIntent)
            .addAction(0, "Silence", silencePendingIntent)
            .build()
    }

    fun showMissedNotification(context: Context, medicationName: String, dosage: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, MISSED_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setContentTitle("Missed Dose: $medicationName")
            .setContentText("The scheduled dose of $dosage was not acknowledged.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
