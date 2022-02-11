package com.chenfu.avdioedit.base;

import androidx.lifecycle.ViewModel;

/**
 * Model实现类的基类，只能被VM持有
 */
public interface Impl {
    void setViewModel(ViewModel viewModel);
}
