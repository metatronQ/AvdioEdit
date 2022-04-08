package com.chenfu.avdioedit.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.chenfu.avdioedit.base.BaseViewModel;
import com.chenfu.avdioedit.model.data.CropModel;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.model.impl.MultiTrackImpl;
import com.chenfu.avdioedit.view.multitrack.SegmentContainer;

public class MultiTrackViewModel extends BaseViewModel<MultiTrackImpl> {

    public MutableLiveData<MediaTrackModel> updateTrack = new MutableLiveData<>();

    public final CropModel cropModel = new CropModel();

    public SegmentContainer.UpdateSelectedStatusListener updateSelectedStatusListener;

    @Override
    protected MultiTrackImpl bindImpl() {
        return new MultiTrackImpl(this);
    }

    @Override
    protected void subscribe() {

    }
}
