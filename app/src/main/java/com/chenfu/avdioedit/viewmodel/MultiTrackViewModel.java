package com.chenfu.avdioedit.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.chenfu.avdioedit.base.BaseViewModel;
import com.chenfu.avdioedit.model.data.CropModel;
import com.chenfu.avdioedit.model.impl.MultiTrackImpl;

public class MultiTrackViewModel extends BaseViewModel<MultiTrackImpl> {

    public final CropModel cropModel = new CropModel();

    @Override
    protected MultiTrackImpl bindImpl() {
        return new MultiTrackImpl(this);
    }

    @Override
    protected void subscribe() {

    }
}
