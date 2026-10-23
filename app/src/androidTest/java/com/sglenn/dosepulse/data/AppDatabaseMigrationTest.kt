package com.sglenn.dosepulse.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @Test
    fun versionOneDoseHistorySurvivesAddingSnoozeColumn() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "pillpulse_migration_test.db"
        context.deleteDatabase(name)
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE dose_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        medicationId INTEGER NOT NULL,
                        medicationName TEXT NOT NULL,
                        scheduledDate TEXT NOT NULL,
                        scheduledTime TEXT NOT NULL,
                        takenTime INTEGER,
                        status TEXT NOT NULL,
                        skipReason TEXT,
                        notes TEXT NOT NULL
                    )""".trimIndent()
                )
                db.execSQL(
                    "INSERT INTO dose_logs (medicationId, medicationName, scheduledDate, scheduledTime, status, notes) " +
                        "VALUES (7, 'Test medication', '2026-10-23', '08:00', 'TAKEN', 'existing log')"
                )
            }

            override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(callback)
                .build()
        )
        try {
            val db = helper.writableDatabase
            AppDatabase.MIGRATION_1_2.migrate(db)
            db.query("SELECT status, notes, snoozedUntil FROM dose_logs WHERE id = 1").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("TAKEN", cursor.getString(0))
                assertEquals("existing log", cursor.getString(1))
                assertEquals(true, cursor.isNull(2))
            }
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }
}
