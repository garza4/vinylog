package com.example.vinylog.objects;

import java.io.Serializable;
import java.util.List;

public class MCollection implements Serializable {
    String collectionName;
    List<Album> albums;

    public MCollection(String collectionName, List<Album> albums) {
        this.collectionName = collectionName;
        this.albums = albums;
    }

    public MCollection(){

    }

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
}
