package com.chenfu.avdioedit.viewmodel;

import androidx.lifecycle.MutableLiveData;

import com.chenfu.avdioedit.base.BaseViewModel;
import com.chenfu.avdioedit.model.impl.LeftEditImpl;

public class LeftEditViewModel extends BaseViewModel<LeftEditImpl> {

    public MutableLiveData<Boolean> launchSaf = new MutableLiveData<>();

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
}
