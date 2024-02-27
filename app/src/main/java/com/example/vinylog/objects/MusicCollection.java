package com.example.vinylog.objects;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.List;

public class MusicCollection implements Parcelable {
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {

    }
}
