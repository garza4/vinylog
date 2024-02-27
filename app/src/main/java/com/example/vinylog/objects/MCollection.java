package com.example.vinylog.objects;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.List;

public class MCollection implements Parcelable {
    String collectionName;
    List<Album> albums;

    public MCollection(String collectionName, List<Album> albums) {
        this.collectionName = collectionName;
        this.albums = albums;
    }

    protected MCollection(Parcel in) {
        collectionName = in.readString();
        albums = in.createTypedArrayList(Album.CREATOR);
    }

    public static final Creator<MCollection> CREATOR = new Creator<MCollection>() {
        @Override
        public MCollection createFromParcel(Parcel in) {
            return new MCollection(in);
        }

        @Override
        public MCollection[] newArray(int size) {
            return new MCollection[size];
        }
    };

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public List<Album> getAlbums() {
        return albums;
    }

    public void setAlbums(List<Album> albums) {
        this.albums = albums;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(collectionName);
        parcel.writeTypedList(albums);
    }
}
