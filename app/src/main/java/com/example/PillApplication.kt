package com.example

import android.app.Application
import com.example.alarm.AlarmNotificationHelper
import com.example.data.AppDatabase
import com.example.data.MedicationRepository

class PillApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        MedicationRepository(
            database.medicationDao(),
            database.doseLogDao(),
            database.medicationReferenceDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize dedicated high-priority alarm channels
        AlarmNotificationHelper.createChannels(this)
    }

    companion object {
        lateinit var instance: PillApplication
            private set
    }
}
