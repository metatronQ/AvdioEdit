package com.chenfu.avdioedit.view.fragment;

import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.base.BaseFragment;
import com.chenfu.avdioedit.model.data.ProgressModel;
import com.chenfu.avdioedit.model.data.VideoModel;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.viewmodel.PlayerViewModel;

public class PlayerFragment extends BaseFragment {

    private View rootView;
    private SurfaceView surfaceView;
    private ImageButton back10Seconds;
    private ImageButton playOrPause;
    private ImageButton forward10Seconds;
    private TextView curPositionTv;
    private TextView durationPositionTv;

    private PlayerViewModel playerViewModel;

    private Observer<ProgressModel> progressModelObserver = new Observer<ProgressModel>() {
        @Override
        public void onChanged(ProgressModel progressModel) {
            durationPositionTv.setText(String.valueOf(progressModel.duration));
            curPositionTv.setText(String.valueOf(progressModel.position));
        }
    };

    private Observer<VideoModel> videoModelObserver = new Observer<VideoModel>() {
        @Override
        public void onChanged(VideoModel videoModel) {
            recalculationScreen(videoModel.vWidth, videoModel.vHeight);
        }
    };

    private Observer<Boolean> playOverObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            curPositionTv.setText(durationPositionTv.getText());
            playOrPause.setImageResource(R.drawable.round_play_arrow_black_20);
        }
    };

    private Observer<String> setFilePathThenPrepare = new Observer<String>() {
        @Override
        public void onChanged(String s) {
            playerViewModel.setPath(s);
            playerViewModel.prepareAsync();
        }
    };

    private Observer<MediaTrackModel> mediaTrackObserver = new Observer<MediaTrackModel>() {
        @Override
        public void onChanged(MediaTrackModel mediaTrackModel) {
            routerViewModel.deliverMediaTrack.setValue(mediaTrackModel);
        }
    };

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_player;
    }

    @Override
    protected void init(View view) {
        this.rootView = view;
        playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
        initViews();
        initSurfaceView();
    }

    private void initViews() {
        curPositionTv = rootView.findViewById(R.id.current_position);
        durationPositionTv = rootView.findViewById(R.id.duration_position);
        back10Seconds = rootView.findViewById(R.id.back_10_seconds);
        back10Seconds.setOnClickListener(v -> {
            playerViewModel.backward10();
        });
        forward10Seconds = rootView.findViewById(R.id.forward_10_seconds);
        forward10Seconds.setOnClickListener(v -> {
            playerViewModel.forward10();
        });
        playOrPause = rootView.findViewById(R.id.play_or_pause);
        playOrPause.setOnClickListener(v -> {
            playerViewModel.play();
            if (playerViewModel.isPlaying()) {
                playOrPause.setImageResource(R.drawable.round_pause_black_20);
            } else {
                playOrPause.setImageResource(R.drawable.round_play_arrow_black_20);
            }
        });
        playOrPause.setImageResource(R.drawable.round_play_arrow_black_20);
    }

    private void initSurfaceView() {
        surfaceView = rootView.findViewById(R.id.surface_view);
//        surfaceView.setZOrderOnTop(false);
//        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.getHolder().addCallback(playerViewModel.getImpl());
    }

    /**
     * 重新计算surfaceview的长宽
     *
     * @param vWidth
     * @param vHeight
     */
    private void recalculationScreen(int vWidth, int vHeight) {
        RelativeLayout surfaceContainer = rootView.findViewById(R.id.surface_container);
        int lw = surfaceContainer.getWidth();
        int lh = surfaceContainer.getHeight();

        if (vWidth > lw || vHeight > lh) {
            // 如果video的宽或者高超出了当前容器的大小，则要进行缩放
            float wRatio = (float) vWidth / (float) lw;
            float hRatio = (float) vHeight / (float) lh;

            // 选择大的一个进行缩放
            float ratio = Math.max(wRatio, hRatio);
            vWidth = (int) Math.ceil((float) vWidth / ratio);
            vHeight = (int) Math.ceil((float) vHeight / ratio);

            // 设置surfaceView的布局参数
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(vWidth, vHeight);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            surfaceView.setLayoutParams(lp);
        }
    }

    @Override
    protected void observeActions() {
        routerViewModel.deliverFilePath.observeForever(setFilePathThenPrepare);

        playerViewModel.showPosition.observeForever(progressModelObserver);
        playerViewModel.recalculationScreen.observeForever(videoModelObserver);
        playerViewModel.playOver.observeForever(playOverObserver);
        playerViewModel.notifyMultiTrack.observeForever(mediaTrackObserver);
    }

    @Override
    protected void removeObserversAndDispose() {
        super.removeObserversAndDispose();
        routerViewModel.deliverFilePath.removeObserver(setFilePathThenPrepare);

        playerViewModel.showPosition.removeObserver(progressModelObserver);
        playerViewModel.recalculationScreen.removeObserver(videoModelObserver);
        playerViewModel.playOver.removeObserver(playOverObserver);
        playerViewModel.notifyMultiTrack.removeObserver(mediaTrackObserver);
    }
}
