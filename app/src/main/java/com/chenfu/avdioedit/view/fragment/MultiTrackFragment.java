package com.chenfu.avdioedit.view.fragment;

import android.view.View;

import androidx.lifecycle.Observer;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.base.BaseFragment;
import com.chenfu.avdioedit.view.multitrack.TrackContainer;
import com.chenfu.avdioedit.view.multitrack.model.MediaTrack;

import io.reactivex.disposables.Disposable;

public class MultiTrackFragment extends BaseFragment {

    private TrackContainer mTrackContainer;

    private Observer<MediaTrack> updateTrackObserver = new Observer<MediaTrack>() {
        @Override
        public void onChanged(MediaTrack mediaTrack) {
            if (mTrackContainer != null) {
                mTrackContainer.setDuration(mediaTrack.getDuration());
                mTrackContainer.addTrack(mediaTrack);
            }
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_multi_track;
    }

    @Override
    protected void init(View view) {
        mTrackContainer = view.findViewById(R.id.track_container);
    }

    @Override
    protected void observeActions() {
        routerViewModel.deliverMediaTrack.observeForever(updateTrackObserver);
    }

    @Override
    protected void removeObserversAndDispose() {
        super.removeObserversAndDispose();
        routerViewModel.deliverMediaTrack.removeObserver(updateTrackObserver);
    }
}
