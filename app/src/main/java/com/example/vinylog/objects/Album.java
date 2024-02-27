package com.example.vinylog.objects;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Album implements Parcelable {
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {

    }
}
