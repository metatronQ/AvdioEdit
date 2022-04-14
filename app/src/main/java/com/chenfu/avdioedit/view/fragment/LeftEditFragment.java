package com.chenfu.avdioedit.view.fragment;

import android.util.Pair;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.base.BaseFragment;
import com.chenfu.avdioedit.model.data.ClipModel;
import com.chenfu.avdioedit.model.data.FramesType;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.model.data.MediaType;
import com.chenfu.avdioedit.viewmodel.LeftEditViewModel;
import com.example.ndk_source.util.ToastUtil;

public class LeftEditFragment extends BaseFragment {
    private LeftEditViewModel leftEditViewModel;

    private Observer<Boolean> openSaf;
    private Observer<MediaTrackModel> clipResult;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_left_edit;
    }

    @Override
    protected void init(View view) {
        TextView mergeTv = view.findViewById(R.id.merge_avideo_tv);
        TextView cropTv = view.findViewById(R.id.crop_avideo_tv);
        view.findViewById(R.id.get_avideo_tv).setOnClickListener(v -> {
            if (routerViewModel.isTrackFull()) {
                ToastUtil.INSTANCE.show(requireContext(), "轨道最多5条");
                return;
            }
            leftEditViewModel.launch();
        });
        cropTv.setOnClickListener(v -> leftEditViewModel.crop(getContext(), routerViewModel.cropData.getValue(), routerViewModel.mediaTrackModelMap));
        mergeTv.setOnClickListener(v -> {
            if (routerViewModel.mergeTwoModelQueue.size() != 2) {
                ToastUtil.INSTANCE.show(requireContext(), "需选中2个片段");
                return;
            }
            ClipModel[] clipModels = routerViewModel.mergeTwoModelQueue.toArray(new ClipModel[0]);
            leftEditViewModel.merge(getContext(), clipModels[0], clipModels[1], routerViewModel.mediaTrackModelMap);
        });
        view.findViewById(R.id.separate_avideo_tv).setOnClickListener(v -> {
            if (routerViewModel.isTrackFull()) {
                ToastUtil.INSTANCE.show(requireContext(), "轨道最多5条");
                return;
            }
            leftEditViewModel.separate(getContext(), routerViewModel.cropData.getValue(), routerViewModel.mediaTrackModelMap);
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
        view.findViewById(R.id.change_crop_or_merge_tv).setOnClickListener(v -> {
            if (mergeTv.isEnabled()) {
                mergeTv.setEnabled(false);
                cropTv.setEnabled(true);
                ((TextView) v).setText("剪切状态");
                routerViewModel.isMergeStatus.setValue(false);
            } else {
                mergeTv.setEnabled(true);
                cropTv.setEnabled(false);
                ((TextView) v).setText("拼合状态");
                routerViewModel.isMergeStatus.setValue(true);
            }

        });
        mergeTv.setEnabled(false);
        leftEditViewModel = new ViewModelProvider(this).get(LeftEditViewModel.class);
    }

    @Override
    protected void observeActions() {
        openSaf = aBoolean -> routerViewModel.startSafWithPermissions.setValue(true);
        clipResult = mediaTrack -> routerViewModel.deliverMediaTrack.setValue(mediaTrack);
        leftEditViewModel.launchSaf.observeForever(openSaf);
        leftEditViewModel.clipResultLiveData.observeForever(clipResult);
    }

    @Override
    protected void removeObserversAndDispose() {
        super.removeObserversAndDispose();
        leftEditViewModel.launchSaf.removeObserver(openSaf);
        leftEditViewModel.clipResultLiveData.removeObserver(clipResult);


    }
}
