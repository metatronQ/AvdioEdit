package com.chenfu.avdioedit.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.chenfu.avdioedit.base.BaseViewModel;
import com.chenfu.avdioedit.model.impl.PlayerImpl;
import com.chenfu.avdioedit.model.data.ProgressModel;
import com.chenfu.avdioedit.model.data.VideoModel;
import com.chenfu.avdioedit.model.data.MediaTrackModel;

public class PlayerViewModel extends BaseViewModel<PlayerImpl> {

    // 播放进度回调
    public MutableLiveData<ProgressModel> showPosition = new MutableLiveData<>();

    // 准备完成并且重新计算屏幕
    public MutableLiveData<VideoModel> recalculationScreen = new MutableLiveData<>();

    // 通知多轨更新轨道信息
    public MutableLiveData<MediaTrackModel> notifyMultiTrack = new MutableLiveData<>();

    // 视频播放结束
    public MutableLiveData<Boolean> playOver = new MutableLiveData<>();

    @Override
    protected PlayerImpl bindImpl() {
        return new PlayerImpl(this);
    }

    @Override
    protected void subscribe() {

    }

    public void setPath(String filePath) {
        impl.setPath(filePath);
    }

    public void prepare() {
        impl.prepare();
    }

    public void prepareAsync() {
        impl.prepareAsync();
    }

    public boolean isPlaying() {
        return impl.isPlaying();
    }

    public void play() {
        impl.play();
    }

    public void forward10() {
        impl.forward10();
    }

    public void backward10() {
        impl.backward10();
    }

    public void seekTo(int position) {
        impl.seekTo(position);
    }
}
