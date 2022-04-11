package com.chenfu.avdioedit.base;

import android.util.Pair;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.chenfu.avdioedit.model.data.ClipModel;
import com.chenfu.avdioedit.model.data.MediaTrackModel;

import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 用于Fragment与fragment、activity之间通信，不包含业务逻辑
 * <p>
 * livedata跟生命周期有关，经由viewmodel创建的只要在一个生命周期内，那么对应注册的监听都能触发
 * routerViewModel = new ViewModelProvider(绑定的对应生命周期的类(this)).get(RouterViewModel.class);
 * 比如在base类绑定同一个activity对象，那么子类fragment之间和fragment与该activity之间就可以通过此viewmodel中的livedata进行通信，
 * 因为都在同一个生命周期
 * 那么这个ViewModelProvider内部肯定有生命周期相关的方法
 * livedata的作用范围限定在this的生命周期内
 * <p>
 * （对，但不是livedata能够类间通信的原因）由于继承的特性，子类会继承父类的非私有属性，
 * 但是这个非私有属性是属于不同子类对象伴生的不同父类对象，不属于同一块内存
 */
public class RouterViewModel extends ViewModel {
    // openSaf
    public MutableLiveData<Boolean> startSafWithPermissions = new MutableLiveData<>();

    public MutableLiveData<String> deliverFilePath = new MutableLiveData<>();

    public MutableLiveData<MediaTrackModel> deliverMediaTrack = new MutableLiveData<>();

    public MutableLiveData<ClipModel> cropData = new MutableLiveData<>();

    // <containerId, segId>
    public MutableLiveData<Pair<Integer, Integer>> deleteTrackOrSegment = new MutableLiveData<>();

    public MutableLiveData<Boolean> isMergeStatus = new MutableLiveData<>();

    public ArrayBlockingQueue<ClipModel> mergeTwoModelQueue = new ArrayBlockingQueue<>(2);

    // 轨道总和map
    public TreeMap<Integer, MediaTrackModel> mediaTrackModelMap;

    public int trackCount = 0;

    public boolean isTrackFull() {
        return trackCount >= 5;
    }
}
