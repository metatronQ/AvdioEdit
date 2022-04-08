package com.chenfu.avdioedit.view.fragment;

import android.view.View;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.base.BaseFragment;
import com.chenfu.avdioedit.util.IdUtils;
import com.chenfu.avdioedit.view.multitrack.TrackContainer;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.viewmodel.MultiTrackViewModel;

public class MultiTrackFragment extends BaseFragment {

    private TrackContainer mTrackContainer;
    private float mProgress = 0;

    private MultiTrackViewModel multiTrackViewModel;

    private Observer<MediaTrackModel> updateTrackObserver = new Observer<MediaTrackModel>() {
        @Override
        public void onChanged(MediaTrackModel mediaTrackModel) {
            if (mediaTrackModel.getId() == -1) {
                mediaTrackModel.setId(IdUtils.INSTANCE.getNewestTrackId());
                mTrackContainer.setDuration(mediaTrackModel.getDuration(), mediaTrackModel.getFrames());
                mTrackContainer.addTrack(mediaTrackModel);
            } else {
                mTrackContainer.updateTrack(mediaTrackModel);
            }
            if (routerViewModel.cropData.getValue() != null) {
                // 更新公用的TrackTreeMap
                routerViewModel.cropData.getValue().setMediaTrackModelMap(mTrackContainer.getChildView().getTrackMap());
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
        mTrackContainer.setOnSeekBarChangeListener(new TrackContainer.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(float progress, boolean fromUser) {
//                LogUtil.INSTANCE.packageName(getContext()).v("" + progress);
                mProgress = progress;
                // 这里获取offset有问题，由于HorizontalScrollView计算了速度，最终scroll得到的位置可能不准确
                // -> 更新放在onScrollChanged，而不是TouchEvent的move分支中
                multiTrackViewModel.cropModel.setCursorOffset(Math.round(mTrackContainer.getChildView().getTimeDuration() * mProgress));
            }

            @Override
            public void onStartTrackingTouch() {

            }

            @Override
            public void onStopTrackingTouch() {

            }
        });
        multiTrackViewModel = new ViewModelProvider(this).get(MultiTrackViewModel.class);
        routerViewModel.cropData.setValue(multiTrackViewModel.cropModel);
        mTrackContainer.setViewModel(multiTrackViewModel);
    }

    @Override
    protected void observeActions() {
        multiTrackViewModel.updateTrack.observeForever(updateTrackObserver);

        routerViewModel.deliverMediaTrack.observeForever(updateTrackObserver);
    }

    @Override
    protected void removeObserversAndDispose() {
        super.removeObserversAndDispose();
        multiTrackViewModel.updateTrack.removeObserver(updateTrackObserver);

        routerViewModel.deliverMediaTrack.removeObserver(updateTrackObserver);
    }
}
