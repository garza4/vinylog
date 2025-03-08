package com.example.vinylog.objects.database

import android.content.Context
import androidx.room.Room

object DatabaseSingleton {
    @Volatile
    private var INSTANCE: AppDb? = null

    fun getDatabase(context: Context): AppDb {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDb::class.java,
                "library"
            ).allowMainThreadQueries().build() // Remove allowMainThreadQueries() in production
            INSTANCE = instance
            instance
        }
    }
}