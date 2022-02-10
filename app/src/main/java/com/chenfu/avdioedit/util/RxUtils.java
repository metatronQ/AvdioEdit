package com.chenfu.avdioedit.util;

import io.reactivex.disposables.Disposable;

public class RxUtils {

    /**
     * dispose
     *
     * @param disposable
     */
    public static void dispose(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }
}
