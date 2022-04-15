package com.chenfu.avdioedit.model.impl;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.chenfu.avdioedit.Interface.PlayerInterface;
import com.chenfu.avdioedit.model.data.MediaType;
import com.chenfu.avdioedit.model.data.ProgressModel;
import com.chenfu.avdioedit.model.data.VideoModel;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.util.RxUtils;
import com.chenfu.avdioedit.viewmodel.PlayerViewModel;
import com.example.ndk_source.util.LogUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * 提供播放器数据的实现类
 *
 * 每个Track对应一个Surface，每个Surface对应一个Player
 *
 * @Model
 */
public class PlayerImpl implements PlayerInterface,
        TextureView.SurfaceTextureListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnPreparedListener {

    /**
     * 每个seg观察者
     */
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    /**
     * 本track的被观察者，即给seg发送信令的对象
     */
    private final PublishSubject<Long> childSegSubject = PublishSubject.create();

    private Surface mSurface;

    public int containerW = 0;
    public int containerH = 0;

    public long currentPosition = 0L;

    private MediaTrackModel trackModel;

    private HashMap<Integer, MediaTrackModel> childsMap;

    private MediaPlayer mediaPlayer;

    private int nowSegId = -1;

    private boolean isPrepared = false;

    public boolean isGesture = true;

    public void updateChildModelList(TextureView textureView, MediaTrackModel mediaTrackModel, HashMap<Integer, MediaTrackModel> map) {
        this.trackModel = mediaTrackModel;
        this.childsMap = map;
        initPlayer();
        if (childsMap.size() == 0) {
            dispose();
            release();
            recalculationScreen(
                    textureView,
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
                    0);
            return;
        }
        // 新增轨道、添加轨道、删除seg、剪切、拼合
        initObservers(textureView);
    }

    private void initObservers(TextureView textureView) {
        if (textureView == null) {
            return;
        }
        dispose();
        compositeDisposable = null;
        compositeDisposable = new CompositeDisposable();
        for (Map.Entry<Integer, MediaTrackModel> entry : childsMap.entrySet()) {
            Disposable disposable = childSegSubject
                    .filter(position -> {
                        MediaTrackModel child = entry.getValue();
                        if (isGesture) {
                            // 滑动
                            currentPosition = position;
                            if (child.getSeqIn() < position && position < child.getSeqOut()) {
                                nowSegId = child.getId();
                                // 在seg范围内
                                if (!isPrepared) {
                                    mediaPlayer.reset();
                                    if (child.getType() == MediaType.TYPE_VIDEO || child.getType() == MediaType.TYPE_VIDEO_AUDIO) {
                                        mediaPlayer.setSurface(mSurface);
                                    }
                                    try {
                                        mediaPlayer.setDataSource(child.getPath());
                                        mediaPlayer.prepare();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    recalculationScreen(textureView, child.getVWidth(), child.getVHeight(), 1);
                                }
                                mediaPlayer.seekTo((int) (position - child.getSeqIn()));
                                return false;
                            }
                            if (nowSegId == -1 || nowSegId != child.getId()) {
                                // 不是当前seg
                                return false;
                            }
                            // 是当前seg且不在当前seg范围内
                            mediaPlayer.reset();
                            isPrepared = false;
                            nowSegId = -1;
                            recalculationScreen(
                                    textureView,
                                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
                                    0);
                            return false;
                        } else {
                            // 播放：优先级seqIn > cur > seqOut
                            if (child.getSeqIn() == position) {
                                nowSegId = child.getId();
                                // 即使cur == seqIn
                                mediaPlayer.reset();
                                if (child.getType() == MediaType.TYPE_VIDEO || child.getType() == MediaType.TYPE_VIDEO_AUDIO) {
                                    mediaPlayer.setSurface(mSurface);
                                }
                                try {
                                    mediaPlayer.setDataSource(child.getPath());
                                    mediaPlayer.prepare();
                                    mediaPlayer.seekTo((int) (position - child.getSeqIn()));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                recalculationScreen(textureView, child.getVWidth(), child.getVHeight(), 1);
                                return true;
                            }
                            if (nowSegId == -1 || nowSegId != child.getId()) {
                                return false;
                            }
                            if (currentPosition == position) {
                                // 滑动已准备
                                return true;
                            }
                            // 按播放的顺序，应该是先隐藏再显示
                            if (child.getSeqOut() == position) {
                                recalculationScreen(
                                        textureView,
                                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
                                        0);
                                return false;
                            }
                            return false;
                        }
                    })
                    .observeOn(Schedulers.computation())
                    .subscribe(position -> {
                        mediaPlayer.start();
                    });
            compositeDisposable.add(disposable);
        }
    }

    public PublishSubject<Long> getSubject() {
        return childSegSubject;
    }

    /**
     * 重新计算texture的长宽
     *
     * @param vWidth
     * @param vHeight
     */
    private void recalculationScreen(TextureView surfaceView, int vWidth, int vHeight, int alpha) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) surfaceView.getLayoutParams();
        if (vWidth > containerW || vHeight > containerH) {
            // 如果video的宽或者高超出了当前容器的大小，则要进行缩放
//            float wRatio = (float) vWidth / (float) lw;
            float hRatio = (float) vHeight / (float) containerH;

//            float ratio = Math.max(wRatio, hRatio);
            // 固定高，缩放宽
            vHeight = containerH;
            vWidth = (int) Math.ceil((float) vWidth / hRatio);

            // 设置surfaceView的布局参数
            lp.width = vWidth;
            lp.height = vHeight;
//            surfaceView.setLayoutParams(lp);
        } else {
            lp.width = vWidth;
            lp.height = vHeight;
        }
        surfaceView.post(() -> {
            surfaceView.setLayoutParams(lp);
            surfaceView.setAlpha(alpha);
        });
    }

    public int getChildSegSize() {
        return childsMap.size();
    }

    private void initPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnInfoListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.setOnPreparedListener(this);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
    }

    @Override
    public void setViewModel(ViewModel viewModel) {
    }

    /**
     * @param position updateTimeWhenPlayFirst
     */
    @Override
    public void play(long position) {
    }

    /**
     * 由于每次开始前会重新排列并发送信令，因此暂停状态不可控，但是seek会更新mediaplay状态
     */
    @Override
    public void pause() {
        if (mediaPlayer == null) {
            LogUtil.INSTANCE.e("无lastPlayer");
            return;
        }
        mediaPlayer.pause();
        // 暂停即停止，再次start需reset
        isPrepared = false;
    }

    // 非播放状态下
    @Override
    public void seekTo(long position) {
    }

    /**
     * 释放资源的时机：
     * view销毁时-》onDestroy()
     * track删除时
     */
    public void release() {
        // 本track销毁则发送完成信令
        childSegSubject.onComplete();
        mediaPlayer.release();
        if (mSurface != null) mSurface.release();
    }

    public void dispose() {
        RxUtils.dispose(compositeDisposable);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        LogUtil.INSTANCE.d("播放结束");
//        playerViewModel.playOver.setValue(true);
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
//        progressModel.position = mp.getCurrentPosition();
//        playerViewModel.showPosition.setValue(progressModel);
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        // log
        LogUtil.INSTANCE.v("视频宽：" + width + "，视频高：" + height);
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        // 此方法不回调，怀疑是实现回调的对象不是activity
//        this.mSurface = new Surface(surface);
//        initAndDeleteOldPlayers();
//        isCreated = true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        mSurface = null;
        mediaPlayer.stop();
        mSurface.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        if (mSurface == null || !mSurface.isValid()) {
            LogUtil.INSTANCE.d("》》》");
            this.mSurface = new Surface(surface);
        }
    }
}
