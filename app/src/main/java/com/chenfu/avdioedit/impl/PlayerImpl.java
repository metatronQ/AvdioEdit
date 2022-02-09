package com.chenfu.avdioedit.impl;

import android.media.MediaPlayer;
import android.os.Build;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import java.io.IOException;

public class PlayerImpl implements SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener {

    private volatile static PlayerImpl player;
    private MediaPlayer mediaPlayer;

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
//            try {
//                mediaPlayer.setDataSource("/storage/emulated/0/Pictures/Screenshots/SVID_20220131_185101_1.mp4");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void play() {
        if (mediaPlayer == null) {
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
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
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        mediaPlayer.release();
        mediaPlayer = null;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {

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
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {

    }
}
