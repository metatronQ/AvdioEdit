package com.chenfu.avdioedit.viewmodel;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.chenfu.avdioedit.base.BaseViewModel;
import com.chenfu.avdioedit.model.data.CropModel;
import com.chenfu.avdioedit.model.impl.LeftEditImpl;
import com.chenfu.avdioedit.model.data.MediaTrackModel;

public class LeftEditViewModel extends BaseViewModel<LeftEditImpl> {

    public MutableLiveData<Boolean> launchSaf = new MutableLiveData<>();
    public MutableLiveData<MediaTrackModel> cropResultLiveData = new MutableLiveData<>();

    @Override
    protected LeftEditImpl bindImpl() {
        return new LeftEditImpl(this);
    }

    @Override
    protected void subscribe() {

    }

    // 打开saf
    public void launch() {
        impl.launch();
    }

    public void crop(Context context, CropModel cropModel) {
        impl.crop(context, cropModel);
    }
}
