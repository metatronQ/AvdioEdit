package com.chenfu.avdioedit.impl;

import android.media.MediaPlayer;
import android.os.Build;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import com.chenfu.avdioedit.enums.VideoStatus;
import com.chenfu.avdioedit.model.ProgressModel;
import com.chenfu.avdioedit.model.VideoModel;
import com.chenfu.avdioedit.util.RxUtils;
import com.chenfu.avdioedit.viewmodel.RouterViewModel;
import com.example.ndk_source.util.LogUtil;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class PlayerImpl implements SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener {

    private volatile static PlayerImpl player;
    private MediaPlayer mediaPlayer;
    private RouterViewModel routerViewModel;
    private ProgressModel progressModel = new ProgressModel();
    private VideoModel videoModel = new VideoModel();

    // 使用interval方式设置计时器，默认在计算调度线程Schedulers.computation()上运行
    // 也可以使用Handler的方式设置计时器，通过handler.removeMessages(UPDATE_TIME)停止事件循环
    private Disposable disposableTimer;
    private VideoStatus videoStatus;

    private PlayerImpl() {
        initPlayer();
    }

    public static PlayerImpl getInstance() {
        if (player == null) {
            synchronized (PlayerImpl.class) {
                if (player == null) {
                    player = new PlayerImpl();
                }
            }
        }
        return player;
    }

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

    /**
     * 需要在surfaceview创建之前调用
     */
    public void setViewModel(RouterViewModel routerViewModel) {
        this.routerViewModel = routerViewModel;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

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
                                        routerViewModel.showPosition.postValue(progressModel);
                                    }
                                }
                            });
        }
    }

    public void forward10() {
        if (mediaPlayer == null) {
            return;
        }
        int position = mediaPlayer.getCurrentPosition();
        seekTo(position + 10000);
    }

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
     * 同步准备
     */
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
    public void prepareAsync() {
        mediaPlayer.prepareAsync();
    }

    /**
     * play前必需的surface，有可能被外面调用，无surface只播放音频
     *
     * @param holder
     */
    public PlayerImpl setDisplay(SurfaceHolder holder) {
        // 保证mediaPlayer被释放后再次设置surface有效
        initPlayer();
        mediaPlayer.setDisplay(holder);
        return this;
    }

    /**
     * 播放本地视频必需的文件路径
     *
     * @param filePath
     */
    public PlayerImpl setPath(String filePath) {
        initPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
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
        progressModel.isFirst = true;

        videoModel.vWidth = mediaPlayer.getVideoWidth();
        videoModel.vHeight = mediaPlayer.getVideoHeight();

        routerViewModel.showPosition.setValue(progressModel);
        routerViewModel.recalculationScreen.setValue(videoModel);

        // 后面再回调就都不是首次了，因此这里直接设为false减少赋值代码
        progressModel.isFirst = false;

        videoStatus = VideoStatus.PREPARE_FINISH;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        LogUtil.INSTANCE.d("播放结束");
        videoStatus = VideoStatus.STOP;
        RxUtils.dispose(disposableTimer);
        routerViewModel.playOver.setValue(true);
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
        routerViewModel.showPosition.setValue(progressModel);
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        // log
        LogUtil.INSTANCE.v("视频宽：" + width + "，视频高：" + height);
    }
}
