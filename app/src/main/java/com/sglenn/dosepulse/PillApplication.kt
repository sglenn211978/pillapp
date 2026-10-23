package com.sglenn.dosepulse

import android.app.Application
import com.sglenn.dosepulse.alarm.AlarmNotificationHelper
import com.sglenn.dosepulse.data.AppDatabase
import com.sglenn.dosepulse.data.MedicationRepository
import com.sglenn.dosepulse.ui.theme.ThemeManager

class PillApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        MedicationRepository(database)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Theme & User Appearance preferences
        ThemeManager.init(this)

        // Initialize dedicated high-priority alarm channels
        AlarmNotificationHelper.createChannels(this)
    }

    companion object {
        lateinit var instance: PillApplication
            private set
    }
}
