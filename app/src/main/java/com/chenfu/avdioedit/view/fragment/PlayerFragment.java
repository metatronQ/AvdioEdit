package com.chenfu.avdioedit.view.fragment;

import android.util.Pair;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.base.BaseFragment;
import com.chenfu.avdioedit.model.data.ProgressModel;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.model.impl.PlayerImpl;
import com.chenfu.avdioedit.viewmodel.PlayerViewModel;
import com.example.ndk_source.util.ToastUtil;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PlayerFragment extends BaseFragment {

    private View rootView;
    private FrameLayout textureContainer;
    private ImageButton playOrPause;
    private TextView curPositionTv;
    private TextView durationPositionTv;

    private PlayerViewModel playerViewModel;

    // 存储所有track的监听事件
    private CompositeDisposable compositeDisposable;

    // 对应surface
    private Disposable d1, d2, d3, d4, d5;

    private final HashMap<Integer, PlayerImpl> viewId2Player = new HashMap<>();

    // 0 - 4 -> 1 - 5
    // private boolean[] playHierarchy = {false, false, false, false, false};

    private int mDuration = 0;
    private int containerW = 0;
    private int containerH = 0;

    private final Observer<ProgressModel> progressModelObserver = new Observer<ProgressModel>() {
        @Override
        public void onChanged(ProgressModel progressModel) {
            playerViewModel.progressModel.duration = progressModel.duration;
            playerViewModel.progressModel.position = progressModel.position;

            mDuration = (int) progressModel.duration;
            durationPositionTv.setText(String.valueOf(progressModel.duration));
            curPositionTv.setText(String.valueOf(progressModel.position));
        }
    };

    // 播放结束回调
    private final Observer<Boolean> playOverObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean aBoolean) {
            playerViewModel.progressModel.duration = mDuration;
            playerViewModel.progressModel.position = mDuration;

            routerViewModel.isPlaying = false;
            curPositionTv.setText(String.valueOf(mDuration));
            playOrPause.setImageResource(R.drawable.round_play_arrow_black_20);
        }
    };

//    private final Observer<VideoModel> videoModelObserver = videoModel -> {
//            recalculationScreen(videoModel.vWidth, videoModel.vHeight);
//    };

    private final Observer<Pair<Integer, MediaTrackModel>> updateTrackPlayers = integerMediaTrackModelPair -> {
        int viewId = integerMediaTrackModelPair.first;
        MediaTrackModel trackModel = integerMediaTrackModelPair.second;
        initTextureViewsAndPlayers(viewId, trackModel);
    };

    /**
     * 删除轨道监听
     */
    private final Observer<Integer> deletePlayerObserver = viewId -> {
        PlayerImpl player = viewId2Player.get(viewId);
        if (player == null) return;
        if (viewId == getBottomViewIdBySize()) {
            // 删除的是底层track
            player.dispose();
            player.release();
            viewId2Player.remove(viewId);

            if (routerViewModel.trackCount == 0) {
                playerViewModel.progressModel.duration = 0;
                playerViewModel.progressModel.position = 0;
                mDuration = 0;
                durationPositionTv.setText("0000");
                curPositionTv.setText("0000");
            }
            return;
        }
        // 删除轨道释放dispose
        player.dispose();
        player.release();
        for (int i = viewId; i > getBottomViewIdBySize(); i--) {
            viewId2Player.put(i, viewId2Player.get(i - 1));
        }
        // 删除映射，而不release
        viewId2Player.remove(getBottomViewIdBySize());
    };

    private int getBottomViewIdBySize() {
        switch (viewId2Player.size()) {
            case 5:
                return 1;
            case 4:
                return 2;
            case 3:
                return 3;
            case 2:
                return 4;
            case 1:
                return 5;
            default:
                return -1;
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_player;
    }

    @Override
    protected void init(View view) {
        this.rootView = view;
        playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
        initViews();
//        initSurfaceViewsAndPlayers();
    }

    private void initViews() {
        textureContainer = rootView.findViewById(R.id.surface_container);
        textureContainer.post(() -> {
            containerW = textureContainer.getWidth();
            containerH = textureContainer.getHeight();
        });
        curPositionTv = rootView.findViewById(R.id.current_position);
        durationPositionTv = rootView.findViewById(R.id.duration_position);
        playOrPause = rootView.findViewById(R.id.play_or_pause);
        playOrPause.setOnClickListener(v -> {
            if (routerViewModel.trackCount == 0) {
                ToastUtil.INSTANCE.show(requireContext(), "无轨道，请添加轨道");
                return;
            }
            if (playerViewModel.progressModel.position == playerViewModel.progressModel.duration) {
                // 游标在末尾
                ToastUtil.INSTANCE.show(requireContext(), "游标在末尾，请移动游标");
                return;
            }
            if (routerViewModel.isPlaying) {
                routerViewModel.isPlaying = false;
                playOrPause.setImageResource(R.drawable.round_play_arrow_black_20);
                routerViewModel.playOrPause.setValue(false);
            } else {
                routerViewModel.isPlaying = true;
                playOrPause.setImageResource(R.drawable.round_pause_black_20);
                // 准备
//                for (Map.Entry<Integer, PlayerImpl> entry : viewId2Player.entrySet()) {
//                    entry.getValue().updateTimeBeforePlay();
//                }
                // 开始
                routerViewModel.playOrPause.setValue(true);
            }
        });
        playOrPause.setImageResource(R.drawable.round_play_arrow_black_20);
    }

    private void initTextureViewsAndPlayers(int viewId, MediaTrackModel trackModel) {
        PlayerImpl player = viewId2Player.get(viewId);
        TextureView textureView = (TextureView) textureContainer.getChildAt(viewId);
        if (player == null) {
            player = new PlayerImpl();
//            textureView.getHolder().addCallback(player);
            textureView.setSurfaceTextureListener(player);
            viewId2Player.put(viewId, player);
            player.containerW = containerW;
            player.containerH = containerH;
        }
        player.updateChildModelList(textureView, trackModel, trackModel.getChildMedias());
    }

    private void initDisposables() {
        compositeDisposable = new CompositeDisposable();
        d5 = routerViewModel.getTimeObserver()
                .filter(aLong -> filterContainer(5, aLong))
                .observeOn(Schedulers.computation())
                .subscribe(aLong -> {
                    viewId2Player.get(5).getSubject().onNext(aLong);
                });
        d4 = routerViewModel.getTimeObserver()
                .filter(aLong -> filterContainer(4, aLong))
                .observeOn(Schedulers.computation())
                .subscribe(aLong -> {
                    viewId2Player.get(4).getSubject().onNext(aLong);
                });
        d3 = routerViewModel.getTimeObserver()
                .filter(aLong -> filterContainer(3, aLong))
                .observeOn(Schedulers.computation())
                .subscribe(aLong -> {
                    viewId2Player.get(3).getSubject().onNext(aLong);
                });
        d2 = routerViewModel.getTimeObserver()
                .filter(aLong -> filterContainer(2, aLong))
                .observeOn(Schedulers.computation())
                .subscribe(aLong -> {
                    viewId2Player.get(2).getSubject().onNext(aLong);
                });
        d1 = routerViewModel.getTimeObserver()
                .filter(aLong -> filterContainer(1, aLong))
                .observeOn(Schedulers.computation())
                .subscribe(aLong -> {
                    viewId2Player.get(1).getSubject().onNext(aLong);
                });
        compositeDisposable.addAll(d5, d4, d3, d2, d1);
    }

    public boolean filterContainer(int viewId, long position) {
        PlayerImpl temp = viewId2Player.get(viewId);
        if (temp != null) {
            if (temp.getChildSegSize() == 0) {
                // size为0则不发送信令
                return false;
            }
            if (position == -1) {
                // -1为暂停
                temp.pause();
                return false;
            }
            temp.isGesture = !routerViewModel.isPlaying;
            return true;
        }
        return false;
    }

    @Override
    protected void observeActions() {
        initDisposables();
        routerViewModel.deliverProgress.observeForever(progressModelObserver);
        routerViewModel.deliverPairSurface2TrackPlayers.observeForever(updateTrackPlayers);
        routerViewModel.playOver.observeForever(playOverObserver);
        routerViewModel.deletePlayer.observeForever(deletePlayerObserver);

//        playerViewModel.showPosition.observeForever(progressModelObserver);
//        playerViewModel.recalculationScreen.observeForever(videoModelObserver);
    }

    @Override
    protected void removeObserversAndDispose() {
        super.removeObserversAndDispose();
        routerViewModel.deliverProgress.removeObserver(progressModelObserver);
        routerViewModel.deliverPairSurface2TrackPlayers.removeObserver(updateTrackPlayers);
        routerViewModel.playOver.removeObserver(playOverObserver);
        routerViewModel.deletePlayer.removeObserver(deletePlayerObserver);

//        playerViewModel.showPosition.removeObserver(progressModelObserver);
//        playerViewModel.recalculationScreen.removeObserver(videoModelObserver);
        compositeDisposable.dispose();
        compositeDisposable = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Map.Entry<Integer, PlayerImpl> entry : viewId2Player.entrySet()) {
            entry.getValue().dispose();
            entry.getValue().release();
        }
        viewId2Player.clear();
    }
}
