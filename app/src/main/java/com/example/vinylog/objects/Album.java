package com.example.vinylog.objects;


import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "albums")
public class Album implements Serializable {
    @PrimaryKey
    @NonNull
    String albumName;
    @ColumnInfo(name="artist")
    String artistName;
    @ColumnInfo(name="songs")
    String songsOnRecord;
    @ColumnInfo(name="year")
    String yearPublished;
    @ColumnInfo(name="genre")
    String genre;

    @ColumnInfo(name="media_type")
    String mediaType;

    public Album(@NonNull String albumName, String artistName, String songsOnRecord, String yearPublished, String genre, String mediaType) {
        this.albumName = albumName;
        this.artistName = artistName;
        this.songsOnRecord = songsOnRecord;
        this.yearPublished = yearPublished;
        this.genre = genre;
        this.mediaType = mediaType;
    }

    public Album(){
        this.albumName = "";
        this.artistName = "";
        this.songsOnRecord = "";
        this.yearPublished = "";
        this.genre = "";
        this.mediaType = "";
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getYearPublished() {
        return yearPublished;
    }

    public void setYearPublished(String yearPublished) {
        this.yearPublished = yearPublished;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getSongsOnRecord() {
        return songsOnRecord;
    }

    public void setSongsOnRecord(String songsOnRecord) {
        this.songsOnRecord = songsOnRecord;
    }
    @Override
    public String toString(){
        return "album: " + albumName + ", artist:" + artistName + ", genre: " + genre;
    }
}
