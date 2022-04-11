package com.chenfu.avdioedit.view.fragment;

import android.util.Pair;
import android.view.View;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.base.BaseFragment;
import com.chenfu.avdioedit.util.IdUtils;
import com.chenfu.avdioedit.view.multitrack.TrackContainer;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.viewmodel.MultiTrackViewModel;
import com.example.ndk_source.util.ToastUtil;

public class MultiTrackFragment extends BaseFragment {

    private TrackContainer mTrackContainer;
    private float mProgress = 0;

    private MultiTrackViewModel multiTrackViewModel;

    private final Observer<MediaTrackModel> updateTrackObserver = new Observer<MediaTrackModel>() {
        @Override
        public void onChanged(MediaTrackModel mediaTrackModel) {
            if (mediaTrackModel.getId() == -1) {
                if (mTrackContainer.getChildView().getTrackMap().size() == 0 &&
                        mediaTrackModel.getDuration() <= 0) {
                    // 无现存轨道则不能新增空轨道
                    ToastUtil.INSTANCE.show(requireContext(), "请先添加有效轨道");
                    return;
                }
                mediaTrackModel.setId(IdUtils.INSTANCE.getNewestTrackId());
                mTrackContainer.setDuration(mediaTrackModel.getDuration());
                mTrackContainer.addTrack(mediaTrackModel);

                routerViewModel.trackCount++;
            } else {
                mTrackContainer.updateTrack(mediaTrackModel);
            }
            // 每次新增或更新轨道时，都可能导致时间轴的变化，时间轴变化会导致偏移量的变化，因此需要更新偏移量
            multiTrackViewModel.clipModel.setCursorOffset(Math.round(mTrackContainer.getChildView().getTimeDuration() * mProgress));
        }
    };

    private final Observer<Pair<Integer, Integer>> deleteTrackOrSegObserver = pair -> {
        mTrackContainer.deleteTrackOrSeg(pair.first, pair.second);
        if (pair.second == -1) {
            routerViewModel.trackCount--;
        }
    };

    private final Observer<Boolean> mergeStatusObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            multiTrackViewModel.isMerge = aBoolean;
            if (aBoolean) {
                multiTrackViewModel.clearCropSelected();
            } else {
                // 转为剪切状态
                multiTrackViewModel.clearMergeSelected();
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
                multiTrackViewModel.clipModel.setCursorOffset(Math.round(mTrackContainer.getChildView().getTimeDuration() * mProgress));
            }

            @Override
            public void onStartTrackingTouch() {

            }

            @Override
            public void onStopTrackingTouch() {

            }
        });
        multiTrackViewModel = new ViewModelProvider(this).get(MultiTrackViewModel.class);
        routerViewModel.cropData.setValue(multiTrackViewModel.clipModel);
        multiTrackViewModel.mergeQueue = routerViewModel.mergeTwoModelQueue;
        mTrackContainer.setViewModel(multiTrackViewModel);
        routerViewModel.mediaTrackModelMap = mTrackContainer.getChildView().getTrackMap();
    }

    @Override
    protected void observeActions() {
        multiTrackViewModel.updateTrack.observeForever(updateTrackObserver);

        routerViewModel.deliverMediaTrack.observeForever(updateTrackObserver);
        routerViewModel.deleteTrackOrSegment.observeForever(deleteTrackOrSegObserver);
        routerViewModel.isMergeStatus.observeForever(mergeStatusObserver);
    }

    @Override
    protected void removeObserversAndDispose() {
        super.removeObserversAndDispose();
        multiTrackViewModel.updateTrack.removeObserver(updateTrackObserver);

        routerViewModel.deliverMediaTrack.removeObserver(updateTrackObserver);
        routerViewModel.deleteTrackOrSegment.removeObserver(deleteTrackOrSegObserver);
        routerViewModel.isMergeStatus.removeObserver(mergeStatusObserver);
    }
}
