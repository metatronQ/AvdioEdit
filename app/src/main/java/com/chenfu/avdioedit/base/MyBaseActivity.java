package com.chenfu.avdioedit.base;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.chenfu.avdioedit.MyApplication;
import com.example.ndk_source.base.BaseActivity;

abstract public class MyBaseActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyApplication app = (MyApplication) getApplication();
        app.push(this);
    }
}
