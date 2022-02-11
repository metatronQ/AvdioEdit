package com.chenfu.avdioedit.Interface;

import android.view.SurfaceHolder;

import com.chenfu.avdioedit.base.Impl;

public interface PlayerInterface extends Impl {

    void setDisplay(SurfaceHolder holder);

    void setPath(String filePath);

    void prepare();

    void prepareAsync();

    boolean isPlaying();

    void play();

    void forward10();

    void backward10();

    void seekTo(int position);
}
