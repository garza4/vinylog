package com.example.vinylog.objects.dao

import androidx.lifecycle.LiveData
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.livedata.LiveDataDaoReturnTypeConverter
import com.example.vinylog.objects.Album

@Dao
@DaoReturnTypeConverters(LiveDataDaoReturnTypeConverter::class)
interface LibraryDao {
    @Query("SELECT * FROM albums")
    fun getAll(): LiveData<List<Album>>

    @Query("SELECT * FROM albums WHERE artist IN (:artist)")
    fun loadAllByArtist(artist: String): LiveData<List<Album>>

    @Query("SELECT media_type FROM albums")
    fun getByMedia(): LiveData<List<String>>

    @Query("SELECT * FROM albums WHERE media_type = :mediaType ORDER BY albumName COLLATE NOCASE")
    fun getAllByMediaType(mediaType: String): LiveData<List<Album>>

    @Insert
    suspend fun insertAll(vararg albums: Album)

    @Insert
    suspend fun insert(album: Album): Long

    @Delete
    suspend fun delete(album: Album)
}
