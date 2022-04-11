package com.chenfu.avdioedit.viewmodel;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.chenfu.avdioedit.base.BaseViewModel;
import com.chenfu.avdioedit.model.data.ClipModel;
import com.chenfu.avdioedit.model.impl.LeftEditImpl;
import com.chenfu.avdioedit.model.data.MediaTrackModel;

import java.util.TreeMap;

public class LeftEditViewModel extends BaseViewModel<LeftEditImpl> {

    public MutableLiveData<Boolean> launchSaf = new MutableLiveData<>();
    public MutableLiveData<MediaTrackModel> clipResultLiveData = new MutableLiveData<>();

    @Override
    protected LeftEditImpl bindImpl() {
        return new LeftEditImpl(this);
    }

    @Override
    protected void subscribe() {

    }

    // 打开saf
    public void launch() {
        impl.launch();
    }

    public void crop(Context context, ClipModel clipModel, TreeMap<Integer, MediaTrackModel> map) {
        impl.crop(context, clipModel, map);
    }

    public void merge(Context context, ClipModel firstClip, ClipModel secondClip, TreeMap<Integer, MediaTrackModel> map) {
        impl.merge(context, firstClip, secondClip, map);
    }

    public void separate(Context context, ClipModel clipModel, TreeMap<Integer, MediaTrackModel> map) {
        impl.separate(context, clipModel, map);
    }
}
