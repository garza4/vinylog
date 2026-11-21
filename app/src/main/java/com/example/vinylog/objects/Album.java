package com.example.vinylog.objects;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "albums")
public class Album implements Serializable {
    @PrimaryKey(autoGenerate = true)
    long id;

    String albumName;

    @ColumnInfo(name = "artist")
    String artistName;

    @ColumnInfo(name = "songs")
    String songsOnRecord;

    @ColumnInfo(name = "year")
    String yearPublished;

    @ColumnInfo(name = "genre")
    String genre;

    @ColumnInfo(name = "media_type")
    String mediaType;

    @ColumnInfo(name = "barcode")
    String barcode;

    public Album(String albumName, String artistName, String songsOnRecord, String yearPublished, String genre, String mediaType, String barcode) {
        this.albumName = albumName == null ? "" : albumName;
        this.artistName = artistName == null ? "" : artistName;
        this.songsOnRecord = songsOnRecord == null ? "" : songsOnRecord;
        this.yearPublished = yearPublished == null ? "" : yearPublished;
        this.genre = genre == null ? "" : genre;
        this.mediaType = mediaType == null ? "" : mediaType;
        this.barcode = barcode == null ? "" : barcode;
    }

    @Ignore
    public Album(String albumName, String artistName, String songsOnRecord, String yearPublished, String genre, String mediaType) {
        this(albumName, artistName, songsOnRecord, yearPublished, genre, mediaType, "");
    }

    @Ignore
    public Album() {
        this("", "", "", "", "", "", "");
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode == null ? "" : barcode;
    }

    @Override
    public String toString() {
        String name = albumName == null ? "" : albumName;
        String artist = artistName == null ? "" : artistName;
        String year = yearPublished == null || yearPublished.isEmpty() ? "" : " (" + yearPublished + ")";
        if (artist.isEmpty()) {
            return name + year;
        }
        return name + year + " — " + artist;
    }
}
