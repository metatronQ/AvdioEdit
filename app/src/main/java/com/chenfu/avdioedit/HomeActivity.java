package com.chenfu.avdioedit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chenfu.avdioedit.base.MyBaseActivity;
import com.chenfu.avdioedit.impl.PlayerImpl;
import com.example.ndk_source.callback.Callback;

public class HomeActivity extends MyBaseActivity {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSON_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private SurfaceView surfaceView;
    private ImageButton back10Seconds;
    private ImageButton playOrPause;
    private ImageButton forward10Seconds;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        hideStateBar();
        initViews();
        initSurfaceView();
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        // 页面加载完毕之后请求saf
//        launch(this::setPath);
//    }

    protected void initViews() {
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
        button = findViewById(R.id.get_video_bt);
        button.setOnClickListener(v -> {
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

    protected void initSurfaceView() {
        surfaceView = findViewById(R.id.surface_view);
//        surfaceView.setZOrderOnTop(false);
//        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.getHolder().addCallback(PlayerImpl.getInstance());
    }

    protected void setPath(String filePath) {
        PlayerImpl.getInstance().setPath(filePath);
        PlayerImpl.getInstance().prepareAsync();
    }

    /**
     * 隐藏状态栏，不同SDK版本不同方法
     */
    protected void hideStateBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars());
            }
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }
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
}