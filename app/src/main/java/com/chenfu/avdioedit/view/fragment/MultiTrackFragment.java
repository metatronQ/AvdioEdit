package com.chenfu.avdioedit.view.fragment;

import android.view.View;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.base.BaseFragment;
import com.chenfu.avdioedit.view.multitrack.TrackContainer;
import com.chenfu.avdioedit.model.data.MediaTrack;
import com.chenfu.avdioedit.viewmodel.MultiTrackViewModel;
import com.example.ndk_source.util.LogUtil;

public class MultiTrackFragment extends BaseFragment {

    private TrackContainer mTrackContainer;
    private float mProgress = 0;

    private MultiTrackViewModel multiTrackViewModel;

    private Observer<MediaTrack> updateTrackObserver = new Observer<MediaTrack>() {
        @Override
        public void onChanged(MediaTrack mediaTrack) {
            //                MediaTrack childTrack = new MediaTrack();
//                childTrack.setId(0);
//                childTrack.setSeqIn(0);
//                childTrack.setSeqOut(mediaTrack.getDuration());
//                childTrack.setDuration(childTrack.getSeqOut() - childTrack.getSeqIn());
//                childTrack.setType(mediaTrack.getType());
//                childTrack.setFrames(mediaTrack.getFrames());

//                MediaTrack childTrack1 = new MediaTrack();
//                childTrack1.setId(1);
//                childTrack1.setDuration(mediaTrack.getDuration());
//                childTrack1.setSeqIn(mediaTrack.getDuration() / 2);
//                childTrack1.setSeqOut(mediaTrack.getDuration());
//                childTrack1.setDuration(childTrack.getSeqOut() - childTrack.getSeqIn());
//                childTrack1.setType(mediaTrack.getType());
//                childTrack1.setFrames(mediaTrack.getFrames());

//                mediaTrack.getChildMedias().put(childTrack.getId(), childTrack);
//                mediaTrack.getChildMedias().put(childTrack1.getId(), childTrack1);
//                mTrackContainer.setProgress(0.5f);
            if (mediaTrack.getId() == -1) {
                mediaTrack.setId(mTrackContainer.getChildView().getTrackMap().size());
                mTrackContainer.setDuration(mediaTrack.getDuration(), mediaTrack.getFrames());
                mTrackContainer.addTrack(mediaTrack);
            } else {
                mTrackContainer.updateTrack(mediaTrack);
            }
            if (routerViewModel.cropData.getValue() != null) {
                // 更新公用的TrackTreeMap
                routerViewModel.cropData.getValue().setMediaTrackMap(mTrackContainer.getChildView().getTrackMap());
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
                multiTrackViewModel.cropModel.setCursorOffset(Math.round(mTrackContainer.getChildView().getMaxDuration() * mProgress));
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
        routerViewModel.deliverMediaTrack.observeForever(updateTrackObserver);
    }

    @Override
    protected void removeObserversAndDispose() {
        super.removeObserversAndDispose();
        routerViewModel.deliverMediaTrack.removeObserver(updateTrackObserver);
    }
}
