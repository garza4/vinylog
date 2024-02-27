package com.example.vinylog.objects;

import java.util.List;

public class MCollection {
    String collectionName;
    List<Album> albums;

    public MCollection(String collectionName, List<Album> albums) {
        this.collectionName = collectionName;
        this.albums = albums;
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
