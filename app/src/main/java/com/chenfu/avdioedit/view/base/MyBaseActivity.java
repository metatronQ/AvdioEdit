package com.chenfu.avdioedit.view.base;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.chenfu.avdioedit.MyApplication;
import com.chenfu.avdioedit.viewmodel.RouterViewModel;
import com.example.ndk_source.base.BaseActivity;

abstract public class MyBaseActivity extends BaseActivity {

    protected RouterViewModel routerViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication app = (MyApplication) getApplication();
        app.push(this);
        initViewModel();
    }

    public void initViewModel() {
        routerViewModel = new ViewModelProvider(this).get(RouterViewModel.class);
    }
}
