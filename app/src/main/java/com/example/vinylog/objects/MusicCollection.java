package com.example.vinylog.objects;

import java.util.List;

public class MusicCollection {
//    {
//        albums:[collections: {name:"",albums }]
//    }
    List<List<MCollection>> collect;

    public MusicCollection(List<List<MCollection>> collect) {
        this.collect = collect;
    }

    public List<List<MCollection>> getCollect() {
        return collect;
    }

    public void setCollect(List<List<MCollection>> collect) {
        this.collect = collect;
    }
}
