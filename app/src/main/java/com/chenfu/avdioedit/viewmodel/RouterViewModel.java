package com.chenfu.avdioedit.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.chenfu.avdioedit.model.ProgressModel;
import com.chenfu.avdioedit.model.VideoModel;

public class RouterViewModel extends ViewModel {

    // 播放进度回调
    public MutableLiveData<ProgressModel> showPosition = new MutableLiveData<>();

    // 准备完成并且重新计算屏幕
    public MutableLiveData<VideoModel> recalculationScreen = new MutableLiveData<>();

    // 视频播放结束
    public MutableLiveData<Boolean> playOver = new MutableLiveData<>();
}
