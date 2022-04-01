package com.chenfu.avdioedit.model.impl;

import androidx.lifecycle.ViewModel;

import com.chenfu.avdioedit.Interface.MultiTrackInterface;
import com.chenfu.avdioedit.viewmodel.MultiTrackViewModel;

public class MultiTrackImpl implements MultiTrackInterface {
    private MultiTrackViewModel multiTrackViewModel;

    public MultiTrackImpl(MultiTrackViewModel multiTrackViewModel) {
        setViewModel(multiTrackViewModel);
    }

    @Override
    public void setViewModel(ViewModel viewModel) {
        this.multiTrackViewModel = (MultiTrackViewModel) viewModel;
    }
}
