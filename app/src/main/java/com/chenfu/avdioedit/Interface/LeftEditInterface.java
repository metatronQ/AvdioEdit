package com.chenfu.avdioedit.Interface;

import android.content.Context;

import com.chenfu.avdioedit.base.Impl;
import com.chenfu.avdioedit.model.data.CropModel;

public interface LeftEditInterface extends Impl {
    void launch();

    void crop(Context context, CropModel cropModel);


}
