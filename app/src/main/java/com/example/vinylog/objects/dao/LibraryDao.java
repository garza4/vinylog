package com.example.vinylog.objects.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.vinylog.objects.Album;

import java.util.List;

@Dao
public interface LibraryDao {
    @Query("SELECT * FROM albums")
    LiveData<List<Album>> getAll();

    @Query("SELECT * FROM albums WHERE artist IN (:artist)")
    LiveData<List<Album>> loadAllByArtist(String artist);

    @Query("SELECT media_type FROM albums")
    LiveData<List<String>> getByMedia();

    @Query("SELECT * FROM albums WHERE media_type IN (:mediaType)")
    LiveData<List<Album>> getAllByMediaType(String mediaType);

    @Insert
    void insertAll(Album... albums);

    @Insert
    void insertByMediaType(Album album);

    @Delete
    void delete(Album album);
}
