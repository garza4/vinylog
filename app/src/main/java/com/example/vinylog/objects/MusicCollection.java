package com.example.vinylog.objects;

import android.os.Parcel;

import java.io.Serializable;
import java.util.List;

public class MusicCollection implements Serializable {
//    {
//        albums:[collections: {name:"",albums }]
//    }
    List<List<MCollection>> collect;

    public MusicCollection(List<List<MCollection>> collect) {
        this.collect = collect;
    }

    protected MusicCollection(Parcel in) {
    }

    public List<List<MCollection>> getCollect() {
        return collect;
    }

    public void setCollect(List<List<MCollection>> collect) {
        this.collect = collect;
    }
}
