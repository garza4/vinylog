package com.example.vinylog.objects.database

import android.content.Context
import androidx.room3.Room
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
                .fallbackToDestructiveMigration()
                .setDriver(AndroidSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
            INSTANCE = instance
            instance
        }
    }
}
