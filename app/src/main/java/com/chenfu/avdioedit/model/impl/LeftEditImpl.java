package com.chenfu.avdioedit.model.impl;

import androidx.lifecycle.ViewModel;

import com.chenfu.avdioedit.Interface.LeftEditInterface;
import com.chenfu.avdioedit.viewmodel.LeftEditViewModel;

public class LeftEditImpl implements LeftEditInterface {

    private LeftEditViewModel leftEditViewModel;

    public LeftEditImpl(LeftEditViewModel leftEditViewModel) {
        setViewModel(leftEditViewModel);
    }

    @Override
    public void launch() {
        // 由于SAF和动态权限都需要依赖fragment或activity，但是搭的框架impl中不需要持有有生命周期的对象
        // 因此这里直接赋值boolean的livedata通知在fragment或activity中saf启动
        // 即将获取path的方法交给了view层，一般来说是不应该的，只是这里获取数据的过程比较特殊
        leftEditViewModel.launchSaf.setValue(true);
    }

    @Override
    public void setViewModel(ViewModel viewModel) {
        this.leftEditViewModel = (LeftEditViewModel) viewModel;
    }
}
