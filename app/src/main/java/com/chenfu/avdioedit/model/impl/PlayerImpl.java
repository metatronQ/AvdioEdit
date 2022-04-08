package com.chenfu.avdioedit.model.impl;

import android.media.MediaPlayer;
import android.os.Build;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.chenfu.avdioedit.Interface.PlayerInterface;
import com.chenfu.avdioedit.enums.VideoStatus;
import com.chenfu.avdioedit.model.data.ProgressModel;
import com.chenfu.avdioedit.model.data.VideoModel;
import com.chenfu.avdioedit.util.IdUtils;
import com.chenfu.avdioedit.util.RxUtils;
import com.chenfu.avdioedit.model.data.FramesType;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.model.data.MediaType;
import com.chenfu.avdioedit.viewmodel.PlayerViewModel;
import com.example.ndk_source.util.LogUtil;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * 提供播放器数据的实现类，只能被VM持有
 *
 * @Model
 */
public class PlayerImpl implements PlayerInterface,
        SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener {

    private MediaPlayer mediaPlayer;
    private PlayerViewModel playerViewModel;
    private ProgressModel progressModel = new ProgressModel();
    private VideoModel videoModel = new VideoModel();
    private String url = "";

    // 使用interval方式设置计时器，默认在计算调度线程Schedulers.computation()上运行
    // 也可以使用Handler的方式设置计时器，通过handler.removeMessages(UPDATE_TIME)停止事件循环
    private Disposable disposableTimer;
    private VideoStatus videoStatus;

    public PlayerImpl(PlayerViewModel playerViewModel) {
        initPlayer();
        setViewModel(playerViewModel);
    }

    /**
     * 不做成单例是因为以后可能支持拖动多个视频，以多个播放器叠加的形式播放预览
     */
//    private volatile static PlayerImpl player;
//    public static PlayerImpl getInstance() {
//        if (player == null) {
//            synchronized (PlayerImpl.class) {
//                if (player == null) {
//                    player = new PlayerImpl();
//                }
//            }
//        }
//        return player;
//    }

    private void initPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnInfoListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
        }
    }

    private void initTimer() {
        if (disposableTimer == null || disposableTimer.isDisposed()) {
            disposableTimer =
                    Observable.interval(0, 1, TimeUnit.SECONDS)
//                            .takeWhile(new Predicate<Long>() {
//                                @Override
//                                public boolean test(@NonNull Long aLong) {
//                                    return !isPauseOrStop;
//                                }
//                            })
                            .subscribe(new Consumer<Long>() {
                                @Override
                                public void accept(Long aLong) {
                                    if (videoStatus == VideoStatus.START) {
                                        progressModel.position = mediaPlayer.getCurrentPosition();
                                        // setValue 必须在主线程调用，postValue可以在子线程调用
                                        // 都是发送给主线程
                                        playerViewModel.showPosition.postValue(progressModel);
                                    }
                                }
                            });
        }
    }

    /**
     * 需要在surfaceview创建之前调用
     */
    @Override
    public void setViewModel(ViewModel viewModel) {
        playerViewModel = (PlayerViewModel) viewModel;
    }

    /**
     * play前必需的surface，有可能被外面调用，无surface只播放音频
     *
     * @param holder
     */
    @Override
    public void setDisplay(SurfaceHolder holder) {
        // 保证mediaPlayer被释放后再次设置surface有效
        initPlayer();
        mediaPlayer.setDisplay(holder);
    }

    /**
     * 播放本地视频必需的文件路径
     *
     * @param filePath
     */
    @Override
    public void setPath(String filePath) {
        initPlayer();
        try {
            url = filePath;
            mediaPlayer.setDataSource(url);
        } catch (IOException e) {
            e.printStackTrace();
            url = "";
        }
    }

    /**
     * 同步准备
     */
    @Override
    public void prepare() {
        try {
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 异步准备
     */
    @Override
    public void prepareAsync() {
        mediaPlayer.prepareAsync();
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    @Override
    public void play() {
        if (mediaPlayer == null || videoStatus == VideoStatus.PREPARE) {
            return;
        }
        if (mediaPlayer.isPlaying()) {
            videoStatus = VideoStatus.PAUSE;
            mediaPlayer.pause();
        } else {
            initTimer();
            // 此处应该只有三种可能的状态：PrepareFinish、Pause、Stop
            if (videoStatus.getType() >= VideoStatus.PREPARE_FINISH.getType()) {
                videoStatus = VideoStatus.START;
            }
            mediaPlayer.start();
        }
    }

    @Override
    public void forward10() {
        if (mediaPlayer == null) {
            return;
        }
        int position = mediaPlayer.getCurrentPosition();
        seekTo(position + 10000);
    }

    @Override
    public void backward10() {
        if (mediaPlayer == null) {
            return;
        }
        int position = mediaPlayer.getCurrentPosition();
        if (position > 10000) {
            position -= 10000;
        } else {
            position = 0;
        }
        seekTo(position);
    }

    @Override
    public void seekTo(int position) {
        if (mediaPlayer == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mediaPlayer.seekTo(position, MediaPlayer.SEEK_CLOSEST);
        } else {
            mediaPlayer.seekTo(position);
        }
    }

    /**
     * 第一次创建surface时调用
     *
     * @param holder
     */
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        // 设置输出surface
        setDisplay(holder);
        videoStatus = VideoStatus.PREPARE;
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mediaPlayer.release();
        mediaPlayer = null;
        videoStatus = VideoStatus.STOP;
        RxUtils.dispose(disposableTimer);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        progressModel.duration = mediaPlayer.getDuration();
        progressModel.position = mediaPlayer.getCurrentPosition();

        videoModel.vWidth = mediaPlayer.getVideoWidth();
        videoModel.vHeight = mediaPlayer.getVideoHeight();

        MediaTrackModel mediaTrackModel = new MediaTrackModel();
        mediaTrackModel.setId(-1);
        mediaTrackModel.setType(MediaType.TYPE_UNKNOWN);
        mediaTrackModel.setDuration(mediaPlayer.getDuration());
        mediaTrackModel.setSeqIn(0);
        mediaTrackModel.setSeqOut(mediaTrackModel.getDuration());
        mediaTrackModel.setPath(url);
        // FIXME 此处应该通过底层获取视频帧数
        // TEST
        mediaTrackModel.setFrames(FramesType.FRAMES_60);

        MediaTrackModel child = mediaTrackModel.clone();
        child.setId(IdUtils.INSTANCE.getNewestSegmentId());
        // 新建轨道中包含本身
        mediaTrackModel.getChildMedias().put(child.getId(), child);

        // 准备中取值为null
        // mediaTrack.setFrames((Integer) mediaPlayer.getMetrics().get(MediaPlayer.MetricsConstants.FRAMES));

        playerViewModel.showPosition.setValue(progressModel);
        playerViewModel.recalculationScreen.setValue(videoModel);
        playerViewModel.notifyMultiTrack.setValue(mediaTrackModel);

        videoStatus = VideoStatus.PREPARE_FINISH;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        LogUtil.INSTANCE.d("播放结束");
        videoStatus = VideoStatus.STOP;
        RxUtils.dispose(disposableTimer);
        playerViewModel.playOver.setValue(true);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        // seek完成
        LogUtil.INSTANCE.v("seek完成");
        progressModel.position = mediaPlayer.getCurrentPosition();
        playerViewModel.showPosition.setValue(progressModel);
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        // log
        LogUtil.INSTANCE.v("视频宽：" + width + "，视频高：" + height);
    }
}
