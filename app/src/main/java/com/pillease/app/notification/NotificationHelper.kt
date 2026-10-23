package com.pillease.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pillease.app.MainActivity
import com.pillease.app.R
import com.pillease.app.receiver.NotificationActionReceiver

object NotificationHelper {

    const val CHANNEL_ID = "medication_reminders_channel"
    const val CHANNEL_NAME = "Medication Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders when your medications are due"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMedicationNotification(
        context: Context,
        notificationId: Int,
        medicationId: Long,
        medicationName: String,
        dosage: String,
        scheduledDateTime: String,
        instructions: String = ""
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Content intent: Opens the app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: "Taken" (1-click direct log from notification shade!)
        val takeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TAKE_MED
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULED_DATETIME, scheduledDateTime)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: "Snooze 15m"
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE_MED
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(NotificationActionReceiver.EXTRA_MEDICATION_NAME, medicationName)
            putExtra(NotificationActionReceiver.EXTRA_DOSAGE, dosage)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULED_DATETIME, scheduledDateTime)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: "Skip"
        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SKIP_MED
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULED_DATETIME, scheduledDateTime)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 3,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtext = if (instructions.isNotBlank()) "$dosage • $instructions" else dosage

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time for $medicationName")
            .setContentText(subtext)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_check, "✓ Taken", takePendingIntent)
            .addAction(0, "⏰ Snooze 15m", snoozePendingIntent)
            .addAction(0, "Skip", skipPendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
