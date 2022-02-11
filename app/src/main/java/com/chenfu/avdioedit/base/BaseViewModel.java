package com.chenfu.avdioedit.base;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * 业务类ViewModel的基类
 */
abstract public class BaseViewModel<T extends Impl> extends ViewModel {
    protected CompositeDisposable compositeDisposable = new CompositeDisposable();

    protected T impl = bindImpl();

    // 创建impl并绑定ViewModel，且只能父子类间使用
    abstract protected T bindImpl();

    // 必要时提供给外部获取impl
    public T getImpl() {
        return impl;
    }

    abstract protected void subscribe();

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.dispose();
    }

    abstract class DisposingObserver<V> implements Observer<V> {
        @Override
        public void onSubscribe(@NonNull Disposable d) {
            compositeDisposable.add(d);
        }

        @Override
        public void onError(@NonNull Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    }
}
