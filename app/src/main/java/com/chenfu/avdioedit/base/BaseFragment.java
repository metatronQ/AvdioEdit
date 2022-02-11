package com.chenfu.avdioedit.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import io.reactivex.disposables.CompositeDisposable;

abstract public class BaseFragment extends Fragment {

    protected CompositeDisposable compositeDisposable = new CompositeDisposable();

    // 父类层级的viewmodel，用于与fragment以及activity通信
    protected RouterViewModel routerViewModel;

    abstract protected int getLayoutId();

    protected void observeActions() {

    }

    protected void removeObserversAndDispose() {
        compositeDisposable.dispose();
    }

    protected void init(View view) {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutId(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            // HomeActivity
            routerViewModel = new ViewModelProvider(getActivity()).get(RouterViewModel.class);
        }
        // onCreateView返回的view
        init(view);
        observeActions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
    }

    @Override
    public void onDestroy() {
        removeObserversAndDispose();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
