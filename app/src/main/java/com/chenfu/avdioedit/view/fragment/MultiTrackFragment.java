package com.chenfu.avdioedit.view.fragment;

import android.util.Pair;
import android.view.View;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.base.BaseFragment;
import com.chenfu.avdioedit.model.data.FramesType;
import com.chenfu.avdioedit.model.data.MediaType;
import com.chenfu.avdioedit.model.data.ProgressModel;
import com.chenfu.avdioedit.model.data.VideoModel;
import com.chenfu.avdioedit.util.IdUtils;
import com.chenfu.avdioedit.util.RxUtils;
import com.chenfu.avdioedit.util.SupportUtils;
import com.chenfu.avdioedit.view.multitrack.TrackContainer;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.viewmodel.MultiTrackViewModel;
import com.example.ndk_source.util.ToastUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

public class MultiTrackFragment extends BaseFragment {

    private TrackContainer mTrackContainer;
    private float mProgress = 0;

    // 使用interval方式设置计时器，默认在计算调度线程Schedulers.computation()上运行
    // 也可以使用Handler的方式设置计时器，通过handler.removeMessages(UPDATE_TIME)停止事件循环
    private Disposable disposableTimer;

    // 时间轴duration和游标position
    private final ProgressModel progressModel = new ProgressModel();

    private MultiTrackViewModel multiTrackViewModel;

    private final Observer<String> setFilePathThenPrepare = new Observer<String>() {
        @Override
        public void onChanged(String url) {
            int mediaType;
            // 本地环境初始化实例一次
            SupportUtils.INSTANCE.setNewPath(url);
            switch (SupportUtils.INSTANCE.supportFormat(url.split("\\.")[1])) {
                case 1:
                    int trackCount = SupportUtils.INSTANCE.getTrackCount();
                    if (trackCount > 1) {
                        mediaType = MediaType.TYPE_VIDEO_AUDIO;
                    } else if (trackCount == 1){
                        mediaType = MediaType.TYPE_VIDEO;
                    } else {
                        ToastUtil.INSTANCE.show(requireContext(), "无视频数据或视频数据不正确");
                        return;
                    }
                    break;
                case 0:
                    mediaType = MediaType.TYPE_AUDIO;
                    break;
                case -1:
                default:
                    ToastUtil.INSTANCE.show(requireContext(), "不支持的格式");
                    return;
            }

            long duration = SupportUtils.INSTANCE.getDuration();
            if (duration <= 0) {
                ToastUtil.INSTANCE.show(requireContext(), "时长为0");
                return;
            }

            Object frameRate = SupportUtils.INSTANCE.getFrameRate();
            int frameRateReal;
            if (frameRate == null)  {
                frameRateReal = FramesType.FRAMES_60;
            } else {
                frameRateReal = (int) frameRate;
                if (frameRateReal > FramesType.FRAMES_30) {
                    frameRateReal = FramesType.FRAMES_60;
                } else if (frameRateReal > FramesType.FRAMES_24) {
                    frameRateReal = FramesType.FRAMES_30;
                } else if (frameRateReal > FramesType.FRAMES_10){
                    frameRateReal = FramesType.FRAMES_24;
                } else {
                    frameRateReal = FramesType.FRAMES_10;
                }
            }

            VideoModel videoModel = SupportUtils.INSTANCE.getVH();

            MediaTrackModel mediaTrackModel = new MediaTrackModel();
            mediaTrackModel.setId(-1);
            mediaTrackModel.setType(mediaType);
            mediaTrackModel.setDuration(duration);
            mediaTrackModel.setSeqIn(0);
            mediaTrackModel.setSeqOut(mediaTrackModel.getDuration());
            mediaTrackModel.setPath(url);
            mediaTrackModel.setVWidth(videoModel.vWidth);
            mediaTrackModel.setVHeight(videoModel.vHeight);
            mediaTrackModel.setFrames(frameRateReal);

            MediaTrackModel child = mediaTrackModel.clone();
            child.setId(IdUtils.INSTANCE.getNewestSegmentId());
            // 新建轨道中包含本身
            mediaTrackModel.getChildMedias().put(child.getId(), child);

            progressModel.duration = child.getDuration();
            progressModel.position = child.getSeqIn();
            routerViewModel.deliverProgress.setValue(progressModel);

            multiTrackViewModel.updateTrack.setValue(mediaTrackModel);
        }
    };

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
            // 更新duration
            progressModel.duration = mTrackContainer.getChildView().getTimeDuration();
            progressModel.position = multiTrackViewModel.clipModel.getCursorOffset();
            routerViewModel.deliverProgress.setValue(progressModel);

            int viewId = mTrackContainer.getChildView().getViewId(mediaTrackModel.getId());
            if (viewId == -1) {
                ToastUtil.INSTANCE.show(requireContext(), "找不到对应ViewId");
                return;
            }
            routerViewModel.deliverPairSurface2TrackPlayers.setValue(new Pair<>(viewId, mediaTrackModel));
        }
    };

    private final Observer<Pair<Integer, Integer>> deleteTrackOrSegObserver = pair -> {
        int viewId = mTrackContainer.getChildView().getViewId(pair.first);
        mTrackContainer.deleteTrackOrSeg(pair.first, pair.second);
        if (pair.second == -1) {
            routerViewModel.trackCount--;
            routerViewModel.deletePlayer.setValue(viewId);
        } else {
            // 删除片段不删轨道，不删player
            routerViewModel.deliverPairSurface2TrackPlayers.setValue(new Pair<>(viewId, mTrackContainer.getChildView().getTrackMap().get(pair.first)));
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

    private final Observer<Long> playingScrollObserver = new Observer<Long>() {
        @Override
        public void onChanged(Long aLong) {
            // 将会回调onScrollChanged
            int realTrackWidthPx =  mTrackContainer.getChildView().getMeasuredWidth() - mTrackContainer.getMeasuredWidth();
            int offset = (int) (realTrackWidthPx * aLong / progressModel.duration);
            // 采用scrollTo，因为每毫秒移动的像素值不好同步
            mTrackContainer.scrollTo(offset, 0);
        }
    };

    private final Observer<Boolean> playOrPauseObserver = aBoolean -> {
        if (aBoolean) {
            initTimer();
        } else {
            // 由于暂停，需要更新player的position
            routerViewModel.deliverProgress.setValue(progressModel);
        }
    };

    /**
     * TODO: 播放时不能进行除了暂停之外的操作
     */
    private void initTimer() {
        if (disposableTimer == null || disposableTimer.isDisposed()) {
            // position在此时已经固定, 即已经与progressModel无关了
            AtomicLong position = new AtomicLong(progressModel.position);
            long duration = progressModel.duration;
            disposableTimer =
                    Observable.interval(0, 1, TimeUnit.MILLISECONDS)
                            .subscribe(aLong -> {
                                if (!routerViewModel.isPlaying) {
                                    RxUtils.dispose(disposableTimer);
                                    // 发送暂停
                                    routerViewModel.getTimeObserver().onNext(-1L);
                                    return;
                                }
                                if (position.get() == duration) {
                                    // 停止
                                    RxUtils.dispose(disposableTimer);
                                    // 发送停止信令
                                    routerViewModel.playOver.postValue(true);
                                } else {
                                    routerViewModel.getTimeObserver().onNext(position.get());
                                }
                                multiTrackViewModel.playingScroll.postValue(position.get());
                                position.getAndIncrement();
                            });
        }
    }

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
                // 这里获取offset有问题，由于HorizontalScrollView计算了速度，最终scroll得到的位置可能不准确
                // -> 更新放在onScrollChanged，而不是TouchEvent的move分支中
                mProgress = progress;
                long realPosition = Math.round(mTrackContainer.getChildView().getTimeDuration() * mProgress);
                // 限制范围
                realPosition = Math.max(realPosition, 0);
                realPosition = Math.min(realPosition, mTrackContainer.getChildView().getTimeDuration());

                if (routerViewModel.trackCount == 0) {
                    multiTrackViewModel.clipModel.setCursorOffset(0);
                    return;
                }
                multiTrackViewModel.clipModel.setCursorOffset(realPosition);
                // 更新position
                progressModel.duration = mTrackContainer.getChildView().getTimeDuration();
                progressModel.position = realPosition;
                routerViewModel.deliverProgress.setValue(progressModel);
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
        multiTrackViewModel.playingScroll.observeForever(playingScrollObserver);

        routerViewModel.deliverFilePath.observeForever(setFilePathThenPrepare);
        routerViewModel.deliverMediaTrack.observeForever(updateTrackObserver);
        routerViewModel.deleteTrackOrSegment.observeForever(deleteTrackOrSegObserver);
        routerViewModel.isMergeStatus.observeForever(mergeStatusObserver);
        routerViewModel.playOrPause.observeForever(playOrPauseObserver);
    }

    @Override
    protected void removeObserversAndDispose() {
        super.removeObserversAndDispose();
        multiTrackViewModel.updateTrack.removeObserver(updateTrackObserver);
        multiTrackViewModel.playingScroll.removeObserver(playingScrollObserver);

        routerViewModel.deliverFilePath.observeForever(setFilePathThenPrepare);
        routerViewModel.deliverMediaTrack.removeObserver(updateTrackObserver);
        routerViewModel.deleteTrackOrSegment.removeObserver(deleteTrackOrSegObserver);
        routerViewModel.isMergeStatus.removeObserver(mergeStatusObserver);
        routerViewModel.playOrPause.removeObserver(playOrPauseObserver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SupportUtils.INSTANCE.release();
    }
}
