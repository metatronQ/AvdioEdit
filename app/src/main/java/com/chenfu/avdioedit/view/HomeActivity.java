package com.chenfu.avdioedit.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.impl.PlayerImpl;
import com.chenfu.avdioedit.model.ProgressModel;
import com.chenfu.avdioedit.model.VideoModel;
import com.chenfu.avdioedit.view.base.MyBaseActivity;
import com.example.ndk_source.callback.Callback;

public class HomeActivity extends MyBaseActivity {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSON_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private SurfaceView surfaceView;
    private ImageButton back10Seconds;
    private ImageButton playOrPause;
    private ImageButton forward10Seconds;
    private TextView safTv;
    private TextView curPositionTv;
    private TextView durationPositionTv;

    private Observer<ProgressModel> progressModelObserver = new Observer<ProgressModel>() {
        @Override
        public void onChanged(ProgressModel progressModel) {
            if (progressModel.isFirst) {
                durationPositionTv.setText(String.valueOf(progressModel.duration));
            }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        observeSuccess();
        initViews();
        initSurfaceView();
    }

    private void initViews() {
        PlayerImpl.getInstance().setViewModel(routerViewModel);
        curPositionTv = findViewById(R.id.current_position);
        durationPositionTv = findViewById(R.id.duration_position);
        back10Seconds = findViewById(R.id.back_10_seconds);
        back10Seconds.setOnClickListener(v -> {
            PlayerImpl.getInstance().backward10();
        });
        forward10Seconds = findViewById(R.id.forward_10_seconds);
        forward10Seconds.setOnClickListener(v -> {
            PlayerImpl.getInstance().forward10();
        });
        playOrPause = findViewById(R.id.play_or_pause);
        playOrPause.setOnClickListener(v -> {
            PlayerImpl.getInstance().play();
            if (PlayerImpl.getInstance().isPlaying()) {
                playOrPause.setImageResource(R.drawable.round_pause_black_20);
            } else {
                playOrPause.setImageResource(R.drawable.round_play_arrow_black_20);
            }
        });
        playOrPause.setImageResource(R.drawable.round_play_arrow_black_20);
        safTv = findViewById(R.id.get_video_tv);
        safTv.setOnClickListener(v -> {
            // 请求权限
            if (ContextCompat.checkSelfPermission(this, PERMISSON_STORAGE[0]) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, PERMISSON_STORAGE[1]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSON_STORAGE, REQUEST_EXTERNAL_STORAGE);
                return;
            }
            launch(new Callback() {
                @Override
                public void setFilePath(String path) {
                    setPath(path);
                }
            });
        });
    }

    private void initSurfaceView() {
        surfaceView = findViewById(R.id.surface_view);
//        surfaceView.setZOrderOnTop(false);
//        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.getHolder().addCallback(PlayerImpl.getInstance());
    }

    /**
     * 重新计算surfaceview的长宽
     * @param vWidth
     * @param vHeight
     */
    private void recalculationScreen(int vWidth, int vHeight) {
        RelativeLayout surfaceContainer = findViewById(R.id.surface_container);
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
            RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(vWidth, vHeight);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            surfaceView.setLayoutParams(lp);
        }
    }

    private void observeSuccess() {
        routerViewModel.showPosition.observeForever(progressModelObserver);
        routerViewModel.recalculationScreen.observeForever(videoModelObserver);
        routerViewModel.playOver.observeForever(playOverObserver);
    }

    private void removeObservers() {
        routerViewModel.showPosition.removeObserver(progressModelObserver);
        routerViewModel.recalculationScreen.removeObserver(videoModelObserver);
        routerViewModel.playOver.removeObserver(playOverObserver);
    }


    private void setPath(String filePath) {
        PlayerImpl.getInstance().setPath(filePath);
        PlayerImpl.getInstance().prepareAsync();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0) {
                    for (int grantResult : grantResults) {
                        Log.d("2mp3_PermissionResults", "" + grantResult);
                    }
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        removeObservers();
        super.onDestroy();
    }
}