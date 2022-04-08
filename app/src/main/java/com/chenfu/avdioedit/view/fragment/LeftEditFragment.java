package com.chenfu.avdioedit.view.fragment;

import android.util.Pair;
import android.view.View;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.base.BaseFragment;
import com.chenfu.avdioedit.model.data.FramesType;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.model.data.MediaType;
import com.chenfu.avdioedit.viewmodel.LeftEditViewModel;
import com.example.ndk_source.util.ToastUtil;

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
        view.findViewById(R.id.get_avideo_tv).setOnClickListener(v -> {
            if (routerViewModel.isTrackFull()) {
                ToastUtil.INSTANCE.show(requireContext(), "轨道最多5条");
                return;
            }
            leftEditViewModel.launch();
        });
        view.findViewById(R.id.crop_avideo_tv).setOnClickListener(v -> leftEditViewModel.crop(getContext(), routerViewModel.cropData.getValue()));
        view.findViewById(R.id.merge_avideo_tv).setOnClickListener(v -> {

        });
        view.findViewById(R.id.separate_avideo_tv).setOnClickListener(v -> {
            if (routerViewModel.isTrackFull()) {
                ToastUtil.INSTANCE.show(requireContext(), "轨道最多5条");
                return;
            }
        });
        view.findViewById(R.id.delete_segment_tv).setOnClickListener(v -> {
            if (routerViewModel.cropData.getValue() != null
                    && routerViewModel.cropData.getValue().getSegmentId() != -1) {
                // 需要seg被选中
                routerViewModel.deleteTrackOrSegment.setValue(new Pair<>(
                        routerViewModel.cropData.getValue().getContainerId(),
                        routerViewModel.cropData.getValue().getSegmentId())
                );
            }
        });
        view.findViewById(R.id.add_empty_track_tv).setOnClickListener(v -> {
            if (routerViewModel.isTrackFull()) {
                ToastUtil.INSTANCE.show(requireContext(), "轨道最多5条");
                return;
            }
            MediaTrackModel mediaTrackModel = new MediaTrackModel();
            mediaTrackModel.setId(-1);
            mediaTrackModel.setType(MediaType.TYPE_UNKNOWN);
            mediaTrackModel.setDuration(0);
            mediaTrackModel.setSeqIn(0);
            mediaTrackModel.setSeqOut(mediaTrackModel.getDuration());
            mediaTrackModel.setPath("");
            // FIXME 此处应该通过底层获取视频帧数
            mediaTrackModel.setFrames(FramesType.FRAMES_UNKNOWN);
            routerViewModel.deliverMediaTrack.setValue(mediaTrackModel);
        });
        view.findViewById(R.id.delete_track_tv).setOnClickListener(v -> {
            if (routerViewModel.cropData.getValue() != null
                && routerViewModel.cropData.getValue().getContainerId() != -1) {
                // 需要container或seg被选中
                routerViewModel.deleteTrackOrSegment.setValue(new Pair<>(
                        routerViewModel.cropData.getValue().getContainerId(),
                        -1)
                );
            }
        });
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
