package com.chenfu.avdioedit.view.fragment;

import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.base.BaseFragment;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.viewmodel.LeftEditViewModel;

public class LeftEditFragment extends BaseFragment {
    private LeftEditViewModel leftEditViewModel;

    private Observer<Boolean> openSaf;
    private Observer<MediaTrackModel> cropResult;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_left_edit;
    }

    @Override
    protected void init(View view) {
        view.findViewById(R.id.get_video_tv).setOnClickListener(v -> leftEditViewModel.launch());
        view.findViewById(R.id.crop_video_tv).setOnClickListener(v -> leftEditViewModel.crop(getContext(), routerViewModel.cropData.getValue()));
        leftEditViewModel = new ViewModelProvider(this).get(LeftEditViewModel.class);
    }

    @Override
    protected void observeActions() {
        openSaf = aBoolean -> routerViewModel.startSafWithPermissions.setValue(true);
        cropResult = mediaTrack -> routerViewModel.deliverMediaTrack.setValue(mediaTrack);
        leftEditViewModel.launchSaf.observeForever(openSaf);
        leftEditViewModel.cropResultLiveData.observeForever(cropResult);
    }

    @Override
    protected void removeObserversAndDispose() {
        super.removeObserversAndDispose();
        leftEditViewModel.launchSaf.removeObserver(openSaf);
        leftEditViewModel.cropResultLiveData.removeObserver(cropResult);
    }
}
