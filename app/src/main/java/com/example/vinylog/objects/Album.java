package com.example.vinylog.objects;


import android.os.Parcel;

import java.io.Serializable;

public class Album implements Serializable {
    String collectionName;
    String albumName;
    String artistName;
    String songsOnRecord;
    String yearPublished;
    String genre;

    public Album(String albumName, String artistName, String songsOnRecord, String yearPublished, String genre) {
        this.albumName = albumName;
        this.artistName = artistName;
        this.songsOnRecord = songsOnRecord;
        this.yearPublished = yearPublished;
        this.genre = genre;
    }

    protected Album(Parcel in) {
        collectionName = in.readString();
        albumName = in.readString();
        artistName = in.readString();
        songsOnRecord = in.readString();
        yearPublished = in.readString();
        genre = in.readString();
    }

    public String getYearPublished() {
        return yearPublished;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
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
        return albumName + ", " + artistName + ", ";
    }
}
