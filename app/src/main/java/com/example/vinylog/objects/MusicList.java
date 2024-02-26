package com.example.vinylog.objects;

import java.util.List;

public class MusicList {
    List<List<Album>> musicList;

    public MusicList(List<List<Album>> musicList) {
        this.musicList = musicList;
    }

    public List<List<Album>> getMusicList() {
        return musicList;
    }

    public void setMusicList(List<List<Album>> musicList) {
        this.musicList = musicList;
    }
}
