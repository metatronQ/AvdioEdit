package com.chenfu.avdioedit.view.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.model.impl.PlayerImpl;
import com.chenfu.avdioedit.base.MyBaseActivity;
import com.chenfu.avdioedit.view.fragment.LeftEditFragment;
import com.chenfu.avdioedit.view.fragment.PlayerFragment;
import com.example.ndk_source.callback.Callback;

public class HomeActivity extends MyBaseActivity {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSON_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private Observer<Boolean> startSafWithRequestPermissions = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            // 请求权限
            if (ContextCompat.checkSelfPermission(getActivity(), PERMISSON_STORAGE[0]) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(getActivity(), PERMISSON_STORAGE[1]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), PERMISSON_STORAGE, REQUEST_EXTERNAL_STORAGE);
                return;
            }
            launch(new Callback() {
                @Override
                public void setFilePath(String path) {
                    setPath(path);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        observeSuccess();
        initViews();
    }

    private void initViews() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.preview_container, new PlayerFragment())
                .commitAllowingStateLoss();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.left_container, new LeftEditFragment())
                .commitAllowingStateLoss();
    }

    private void observeSuccess() {
        routerViewModel.startSafWithPermissions.observeForever(startSafWithRequestPermissions);
    }

    private void removeObservers() {
        routerViewModel.startSafWithPermissions.removeObserver(startSafWithRequestPermissions);
    }

    private void setPath(String filePath) {
        routerViewModel.deliverFilePath.setValue(filePath);
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