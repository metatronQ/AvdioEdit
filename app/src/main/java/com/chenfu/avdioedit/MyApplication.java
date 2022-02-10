package com.chenfu.avdioedit;

import com.chenfu.avdioedit.view.base.MyBaseActivity;
import com.example.ndk_source.base.BaseApplication;

import java.util.ArrayList;
import java.util.List;

public class MyApplication extends BaseApplication {

    /**
     * Activity栈.
     */
    private List<MyBaseActivity> mActivityList = null;

    @Override
    protected void onCreateWithMainProcess() {
        initActivityList();
    }

    @Override
    protected void onCreateWithOtherProcess() {
        // do nothing
    }

    /**
     * 初始化Activity栈.
     */
    private void initActivityList() {
        if (mActivityList == null) {
            mActivityList = new ArrayList<>();
        }
    }

    /**
     * 压栈.
     *
     * @param activity .
     */
    public void push(MyBaseActivity activity) {
        if (mActivityList == null) return;
        mActivityList.add(activity);
    }

    /**
     * 关闭所有Activity.
     */
    public void finishAll() {
        if (mActivityList == null) return;
        for (MyBaseActivity activity : mActivityList) {
            activity.finish();
        }
    }
}
