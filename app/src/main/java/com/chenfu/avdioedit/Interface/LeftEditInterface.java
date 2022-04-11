package com.chenfu.avdioedit.Interface;

import android.content.Context;

import com.chenfu.avdioedit.base.Impl;
import com.chenfu.avdioedit.model.data.ClipModel;
import com.chenfu.avdioedit.model.data.MediaTrackModel;

import java.util.TreeMap;

public interface LeftEditInterface extends Impl {
    void launch();

    void crop(Context context, ClipModel clipModel, TreeMap<Integer, MediaTrackModel> map);

    void merge(Context context, ClipModel firstClip, ClipModel secondClip, TreeMap<Integer, MediaTrackModel> map);

    void separate(Context context, ClipModel clipModel, TreeMap<Integer, MediaTrackModel> map);
}
