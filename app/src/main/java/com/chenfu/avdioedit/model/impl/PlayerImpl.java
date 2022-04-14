package com.chenfu.avdioedit.model.impl;

import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import com.chenfu.avdioedit.Interface.PlayerInterface;
import com.chenfu.avdioedit.enums.VideoStatus;
import com.chenfu.avdioedit.model.data.MediaType;
import com.chenfu.avdioedit.model.data.ProgressModel;
import com.chenfu.avdioedit.model.data.VideoModel;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.viewmodel.PlayerViewModel;
import com.example.ndk_source.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 提供播放器数据的实现类
 *
 * 每个Track对应一个Surface，每个Surface对应一个Player
 *
 * @Model
 */
public class PlayerImpl implements PlayerInterface,
        SurfaceHolder.Callback,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener {

    private SurfaceHolder mHolder;

    private boolean isCreated = false;

    /**
     * TODO： 开始之前，即点击开始时判断一下，更新结束才真正开始
     * 更新position，若cur在某个seg中偏移，则将cur作为该seg的seqIn，更新当前player的seek
     * 不用删除前面的seg，因为不会到达前面seg的seqIn
     */
    public long currentPosition = 0L;

    private MediaTrackModel trackModel;

    private HashMap<Integer, MediaTrackModel> childsMap;

    /**
     * segId to mediaPlayer
     */
    private final HashMap<Integer, MediaPlayer> segId2PlayerMap = new HashMap<>();

    private MediaPlayer mediaPlayer;

    private final HashMap<Integer, String> segId2Url = new HashMap<>();

    private String lastUrl;

    private final HashMap<Long, Integer> seqIn2SegId = new HashMap<>();

    private final HashMap<Long, Integer> seqOut2SegId = new HashMap<>();

    private PlayerViewModel playerViewModel;
    private ProgressModel progressModel = new ProgressModel();
    private VideoModel videoModel = new VideoModel();
    private int nowSegId = -1;

    // region update map
    public void updateChildModelList(MediaTrackModel mediaTrackModel, HashMap<Integer, MediaTrackModel> map) {
        this.trackModel = mediaTrackModel;
        this.childsMap = map;
        // 用于非添加track时更新
        initAndDeleteOldPlayers();
    }

    private void initAndDeleteOldPlayers() {
        initPlayer();
        Iterator<Map.Entry<Integer, String>> it = segId2Url.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<Integer, String> entry = it.next();
            // 删除在childsMap中不存在的seg对应的url
            if(childsMap.get(entry.getKey()) == null) {
                it.remove();//使用迭代器的remove()方法删除元素
            }
        }
    }

    private void updateSeq2SegIdMap() {
        seqIn2SegId.clear();
        seqOut2SegId.clear();
        segId2Url.clear();
        for (Map.Entry<Integer, MediaTrackModel> entry : childsMap.entrySet()) {
            seqIn2SegId.put(entry.getValue().getSeqIn(), entry.getValue().getId());
            seqOut2SegId.put(entry.getValue().getSeqOut(), entry.getValue().getId());
            segId2Url.put(entry.getValue().getId(), entry.getValue().getPath());
        }
    }
    // endregion

    /**
     * 按下开始之前调用
     * 若cur在某个seg之间，则开始时从该seg开始
     */
    public void updateTimeFirst() {
        // 更新seqIn
        updateSeq2SegIdMap();
        if (currentPosition == 0) {
            return;
        }
        MediaTrackModel seg = null;
        List<MediaTrackModel> sortedChildList = trackModel.getSortedChildArray();
        for (MediaTrackModel childModel : sortedChildList) {
            // 删除处于cur前面的seqIn和seqOut
            if (childModel.getSeqIn() < currentPosition) {
                seqIn2SegId.remove(childModel.getSeqIn());
            }
            if (childModel.getSeqOut() < currentPosition) {
                seqOut2SegId.remove(childModel.getSeqOut());
            }

            if (childModel.getSeqIn() < currentPosition && currentPosition < childModel.getSeqOut()) {
                // 处于该seg范围内
                seg = childModel;
                break;
            }
        }
        if (seg == null) {
            // cur处于空白或终点处
            return;
        }
        seqIn2SegId.put(currentPosition, seg.getId());
    }

    public Integer getSegId(long position) {
        return seqIn2SegId.get(position);
    }

    public Integer getSegOut(long position) {
        return seqOut2SegId.get(position);
    }

    public VideoModel getWH(int segId) {
        MediaTrackModel child = childsMap.get(segId);
        VideoModel videoModel = new VideoModel();
        videoModel.vWidth = child == null ? FrameLayout.LayoutParams.MATCH_PARENT: child.getVWidth();
        videoModel.vHeight = child == null ? FrameLayout.LayoutParams.MATCH_PARENT: child.getVHeight();
        return videoModel;
    }

    private void initPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnInfoListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
        }
    }

    /**
     * 需要在surfaceview创建之前调用
     */
    @Override
    public void setViewModel(ViewModel viewModel) {
        playerViewModel = (PlayerViewModel) viewModel;
    }

    @Override
    public void play(long position) {
        Integer segId = seqIn2SegId.get(position);
        if (segId == null) {
            LogUtil.INSTANCE.manualPkgName("com.chenfu.avdioedit.model.impl.PlayerImpl").e("找不到seg");
            return;
        }
        if (mediaPlayer == null) {
            LogUtil.INSTANCE.manualPkgName("com.chenfu.avdioedit.model.impl").e("请先调用initPlayer初始化player");
            return;
        }

        MediaTrackModel child = childsMap.get(segId);
        if (child == null) {
            return;
        }
        mediaPlayer.reset();
        mediaPlayer.setOnPreparedListener(mp -> {
            // TODO: 需要保证mediaPlay从当前cur开始
            if (position == currentPosition) {
                int offset = (int) (currentPosition - child.getSeqIn());
                mp.seekTo(offset);
            }
            mp.start();
            lastUrl = segId2Url.get(segId);
        });
        if (child.getType() == MediaType.TYPE_VIDEO || child.getType() == MediaType.TYPE_VIDEO_AUDIO) {
            mediaPlayer.setDisplay(mHolder);
        }
        try {
            mediaPlayer.setDataSource(segId2Url.get(segId));
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        lastPlayer = mediaPlayer;
//        if (videoStatus == VideoStatus.PREPARE) {
//            return;
//        }
//        if (mediaPlayer.isPlaying()) {
//            videoStatus = VideoStatus.PAUSE;
//            mediaPlayer.pause();
//        } else {
//            initTimer();
//            // 此处应该只有三种可能的状态：PrepareFinish、Pause、Stop
//            if (videoStatus.getType() >= VideoStatus.PREPARE_FINISH.getType()) {
//                videoStatus = VideoStatus.START;
//            }
//            mediaPlayer.start();
//        }
    }

    @Override
    public void pause() {
        if (mediaPlayer == null) {
            LogUtil.INSTANCE.manualPkgName("com.chenfu.avdioedit.model.impl.PlayerImpl").e("无lastPlayer");
            return;
        }
        mediaPlayer.pause();
    }

    @Override
    public void seekTo(int position) {
        this.currentPosition = position;
    }

    /**
     * 第一次创建surface时调用
     *
     * @param holder
     */
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        // 设置输出surface
        // 需要保证surface在设置之前被创建
        this.mHolder = holder;
        initAndDeleteOldPlayers();
        isCreated = true;
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    // TODO: 申请文件跳到saf时会回调此方法
    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
//        releaseAll();
        isCreated = false;
    }

    /**
     * 释放资源的时机：view销毁时-》onDestroy()
     */
    public void releaseAll() {
        mediaPlayer.release();
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
}
