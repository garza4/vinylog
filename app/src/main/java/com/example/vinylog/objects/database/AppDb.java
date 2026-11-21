package com.example.vinylog.objects.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.vinylog.objects.Album;
import com.example.vinylog.objects.dao.LibraryDao;

@Database(entities = {Album.class}, version = 2, exportSchema = false)
public abstract class AppDb extends RoomDatabase {
    public abstract LibraryDao libraryDao();
}
