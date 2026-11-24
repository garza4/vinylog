package com.example.vinylog.objects.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.Dispatchers

object DatabaseSingleton {
    @Volatile
    private var INSTANCE: AppDb? = null

    fun getDatabase(context: Context): AppDb {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDb::class.java,
                "library"
            )
                // Version 1 keyed rows by album title and had no barcode column.
                // Those installs could not store a scanned entry reliably.
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
            INSTANCE = instance
            instance
        }
    }
}

private val MIGRATION_2_3 = Migration(2, 3) { connection: SQLiteConnection ->
    connection.prepare("ALTER TABLE albums ADD COLUMN cover_url TEXT NOT NULL DEFAULT ''").use { it.step() }
    connection.prepare("ALTER TABLE albums ADD COLUMN record_label TEXT NOT NULL DEFAULT ''").use { it.step() }
}

private val MIGRATION_3_4 = Migration(3, 4) { connection: SQLiteConnection ->
    connection.prepare("ALTER TABLE albums ADD COLUMN runtime TEXT NOT NULL DEFAULT ''").use { it.step() }
    connection.prepare("ALTER TABLE albums ADD COLUMN cast_names TEXT NOT NULL DEFAULT ''").use { it.step() }
    connection.prepare("ALTER TABLE albums ADD COLUMN page_count TEXT NOT NULL DEFAULT ''").use { it.step() }
    connection.prepare("ALTER TABLE albums ADD COLUMN format_name TEXT NOT NULL DEFAULT ''").use { it.step() }
    connection.prepare("ALTER TABLE albums ADD COLUMN content_rating TEXT NOT NULL DEFAULT ''").use { it.step() }
    connection.prepare("ALTER TABLE albums ADD COLUMN player_count TEXT NOT NULL DEFAULT ''").use { it.step() }
}
