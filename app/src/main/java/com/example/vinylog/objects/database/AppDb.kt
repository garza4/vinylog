package com.example.vinylog.objects.database

import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.livedata.LiveDataDaoReturnTypeConverter
import com.example.vinylog.objects.Album
import com.example.vinylog.objects.dao.LibraryDao

@Database(entities = [Album::class], version = 2, exportSchema = false)
@DaoReturnTypeConverters(LiveDataDaoReturnTypeConverter::class)
abstract class AppDb : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
