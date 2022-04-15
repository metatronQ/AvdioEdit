package com.chenfu.avdioedit.Interface;

import android.view.SurfaceHolder;

import com.chenfu.avdioedit.base.Impl;

public interface PlayerInterface extends Impl {

//    void setDisplay(SurfaceHolder holder);
//
//    void setPath(String filePath);
//
//    void prepare();
//
//    void prepareAsync();

//    boolean isPlaying();

    void play(long position);

    void pause();

    void seekTo(long position);
}
