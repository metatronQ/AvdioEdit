package com.chenfu.avdioedit;

import android.content.Context;

/**
 * 参考：https://blog.csdn.net/lmj623565791/article/details/40481055
 * 注意，由于ApplicationContext职责有限，只能在非UI且获取不到上下文的情况下使用此单例
 */
public class GlobalAppContext {
    private static GlobalAppContext sInstance;
    public Context appContext;

    private GlobalAppContext(Context context) {
        this.appContext = context;
    }

    public static synchronized GlobalAppContext getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GlobalAppContext(context.getApplicationContext());
        }
        return sInstance;
    }

    public static Context getContext() {
        return sInstance.appContext;
    }
}