package com.example.vinylog.objects.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.vinylog.objects.Album;

import java.util.List;

@Dao
public interface LibraryDao {
    @Query("SELECT * FROM albums")
    List<Album> getAll();

    @Query("SELECT * FROM albums WHERE artist IN (:artist)")
    List<Album> loadAllByArtist(String artist);

    @Query("SELECT media_type FROM albums")
    List<String> getByMedia();

    @Insert
    void insertAll(Album... albums);

    @Delete
    void delete(Album album);
}
