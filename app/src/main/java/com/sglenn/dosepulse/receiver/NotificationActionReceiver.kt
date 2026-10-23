package com.sglenn.dosepulse.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sglenn.dosepulse.PillApplication
import com.sglenn.dosepulse.alarm.DoseActionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == ACTION_SILENCE_ALARM) {
            DoseActionHandler.silence(context)
            return
        }

        val id = intent.getLongExtra(EXTRA_MED_ID, -1L)
        val date = intent.getStringExtra(EXTRA_SCHED_DATE).orEmpty()
        val time = intent.getStringExtra(EXTRA_SCHED_TIME).orEmpty()
        if (id <= 0 || date.isBlank() || time.isBlank()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = (context.applicationContext as PillApplication).repository
                when (action) {
                    ACTION_TAKE_MED -> DoseActionHandler.take(context, repository, id, date, time)
                    ACTION_SNOOZE_MED -> {
                        val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                        val scheduled = DoseActionHandler.snooze(context, repository, id, date, time, minutes)
                        if (!scheduled) DoseActionHandler.silence(context)
                    }
                    ACTION_SKIP_MED -> DoseActionHandler.skip(
                        context, repository, id, date, time,
                        intent.getStringExtra(EXTRA_SKIP_REASON) ?: "Skipped by user"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TAKE_MED = "com.sglenn.dosepulse.ACTION_TAKE_MED"
        const val ACTION_SNOOZE_MED = "com.sglenn.dosepulse.ACTION_SNOOZE_MED"
        const val ACTION_SKIP_MED = "com.sglenn.dosepulse.ACTION_SKIP_MED"
        const val ACTION_SILENCE_ALARM = "com.sglenn.dosepulse.ACTION_SILENCE_ALARM"
        const val EXTRA_MED_ID = "extra_med_id"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_SCHED_DATE = "extra_sched_date"
        const val EXTRA_SCHED_TIME = "extra_sched_time"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_SKIP_REASON = "extra_skip_reason"
    }
}
