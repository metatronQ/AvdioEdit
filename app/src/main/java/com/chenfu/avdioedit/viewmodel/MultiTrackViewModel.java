package com.chenfu.avdioedit.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.chenfu.avdioedit.base.BaseViewModel;
import com.chenfu.avdioedit.model.data.ClipModel;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.model.impl.MultiTrackImpl;
import com.chenfu.avdioedit.view.multitrack.SegmentContainer;

import java.util.concurrent.ArrayBlockingQueue;

public class MultiTrackViewModel extends BaseViewModel<MultiTrackImpl> {

    public MutableLiveData<MediaTrackModel> updateTrack = new MutableLiveData<>();

    public final ClipModel clipModel = new ClipModel();

    public SegmentContainer.UpdateSelectedStatusListener updateSelectedStatusListener;

    public ArrayBlockingQueue<ClipModel> mergeQueue;

    public boolean isMerge = false;

    @Override
    protected MultiTrackImpl bindImpl() {
        return new MultiTrackImpl(this);
    }

    @Override
    protected void subscribe() {

    }

    public void clearMergeSelected() {
        while (mergeQueue.size() != 0) {
            ClipModel clipModel = mergeQueue.poll();
            if (clipModel != null) {
                updateSelectedStatusListener.update(clipModel.getContainerId(), clipModel.getSegmentId());
            }
        }
    }

    public void clearCropSelected() {
        updateSelectedStatusListener.update(clipModel.getContainerId(), clipModel.getSegmentId());
        clipModel.setContainerId(-1);
        clipModel.setSegmentId(-1);
    }
}
