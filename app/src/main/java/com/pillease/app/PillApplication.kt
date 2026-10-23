package com.pillease.app

import android.app.Application
import com.pillease.app.data.AppDatabase
import com.pillease.app.data.MedicationRepository
import com.pillease.app.notification.NotificationHelper

class PillApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { MedicationRepository(database.medicationDao(), database.doseLogDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Ensure notification channels are registered on launch
        NotificationHelper.createNotificationChannel(this)
    }

    companion object {
        lateinit var instance: PillApplication
            private set
    }
}
