package com.chenfu.avdioedit.view.fragment;

import android.graphics.PixelFormat;
import android.util.Pair;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chenfu.avdioedit.R;
import com.chenfu.avdioedit.base.BaseFragment;
import com.chenfu.avdioedit.model.data.ProgressModel;
import com.chenfu.avdioedit.model.data.VideoModel;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.model.impl.PlayerImpl;
import com.chenfu.avdioedit.util.RxUtils;
import com.chenfu.avdioedit.viewmodel.PlayerViewModel;
import com.example.ndk_source.util.ToastUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class PlayerFragment extends BaseFragment {

    private View rootView;
    private FrameLayout surfaceContainer;
    private ImageButton playOrPause;
    private TextView curPositionTv;
    private TextView durationPositionTv;

    private PlayerViewModel playerViewModel;

    // 存储所有track的监听事件
    private CompositeDisposable compositeDisposable;

    // 对应surface
    private Disposable d1, d2, d3, d4, d5;

    private PlayerImpl player1, player2, player3, player4, player5;

    private final HashMap<Integer, PlayerImpl> viewId2Player = new HashMap<>();

    // 五层，只需要看前四层的层级
    private boolean[] playHierarchy = {false, false, false, false};

    private int mDuration = 0;

    private final Observer<ProgressModel> progressModelObserver = new Observer<ProgressModel>() {
        @Override
        public void onChanged(ProgressModel progressModel) {
            playerViewModel.progressModel.duration = progressModel.duration;
            playerViewModel.progressModel.position = progressModel.position;

            mDuration = (int) progressModel.duration;
            durationPositionTv.setText(String.valueOf(progressModel.duration));
            curPositionTv.setText(String.valueOf(progressModel.position));
            if (!routerViewModel.isPlaying) {
                // 播放时不更新player
                for (Map.Entry<Integer, PlayerImpl> entry : viewId2Player.entrySet()) {
                    entry.getValue().seekTo((int) progressModel.position);
                }
            }
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
            for (Map.Entry<Integer, PlayerImpl> entry : viewId2Player.entrySet()) {
                entry.getValue().seekTo(mDuration);
            }
        }
    };

//    private final Observer<VideoModel> videoModelObserver = videoModel -> {
//            recalculationScreen(videoModel.vWidth, videoModel.vHeight);
//    };

    private final Observer<Pair<Integer, MediaTrackModel>> updateTrackPlayers = integerMediaTrackModelPair -> {
        int viewId = integerMediaTrackModelPair.first;
        MediaTrackModel trackModel = integerMediaTrackModelPair.second;
        initSurfaceViewsAndPlayers(viewId, trackModel);
    };

    /**
     * 删除轨道监听
     */
    private final Observer<Integer> deletePlayerObserver = viewId -> {
        PlayerImpl player = viewId2Player.get(viewId);
        if (player == null) return;
        if (viewId == getBottomViewIdBySize()) {
            // 删除的是底层track
            player.releaseAll();
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
        player.releaseAll();
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
        surfaceContainer = rootView.findViewById(R.id.surface_container);
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
                for (Map.Entry<Integer, PlayerImpl> entry : viewId2Player.entrySet()) {
                    entry.getValue().updateTimeFirst();
                }
                // 开始
                routerViewModel.playOrPause.setValue(true);
            }
        });
        playOrPause.setImageResource(R.drawable.round_play_arrow_black_20);
    }

    private void initSurfaceViewsAndPlayers(int viewId, MediaTrackModel trackModel) {
        PlayerImpl player = viewId2Player.get(viewId);
        if (player == null) {
            player = new PlayerImpl();
            SurfaceView surfaceView = (SurfaceView) surfaceContainer.getChildAt(viewId);
            surfaceView.getHolder().addCallback(player);
            viewId2Player.put(viewId, player);
        }
        player.updateChildModelList(trackModel, trackModel.getChildMedias());
    }

    private void initSurfaceViewsAndPlayers() {
//        surfaceView.setZOrderOnTop(false);
//        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        SurfaceView surfaceView5 = (SurfaceView) surfaceContainer.getChildAt(5);
        player5 = new PlayerImpl();
        surfaceView5.getHolder().addCallback(player5);
        viewId2Player.put(5, player5);

        SurfaceView surfaceView4 = (SurfaceView) surfaceContainer.getChildAt(4);
        player4 = new PlayerImpl();
        surfaceView4.getHolder().addCallback(player4);
        viewId2Player.put(4, player4);

        SurfaceView surfaceView3 = (SurfaceView) surfaceContainer.getChildAt(3);
        player3 = new PlayerImpl();
        surfaceView3.getHolder().addCallback(player3);
        viewId2Player.put(3, player3);

        SurfaceView surfaceView2 = (SurfaceView) surfaceContainer.getChildAt(2);
        player2 = new PlayerImpl();
        surfaceView2.getHolder().addCallback(player2);
        viewId2Player.put(2, player2);

        SurfaceView surfaceView1 = (SurfaceView) surfaceContainer.getChildAt(1);
        player1 = new PlayerImpl();
        surfaceView1.getHolder().addCallback(player1);
        viewId2Player.put(1, player1);
    }

    /**
     * 重新计算surfaceview的长宽
     *
     * @param vWidth
     * @param vHeight
     */
    private void recalculationScreen(SurfaceView surfaceView, int vWidth, int vHeight) {
        int lw = surfaceContainer.getWidth();
        int lh = surfaceContainer.getHeight();

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) surfaceView.getLayoutParams();
        if (vWidth > lw || vHeight > lh) {
            // 如果video的宽或者高超出了当前容器的大小，则要进行缩放
            float wRatio = (float) vWidth / (float) lw;
            float hRatio = (float) vHeight / (float) lh;

            // 选择大的一个进行缩放
            float ratio = Math.max(wRatio, hRatio);
            vWidth = (int) Math.ceil((float) vWidth / ratio);
            vHeight = (int) Math.ceil((float) vHeight / ratio);

            // 设置surfaceView的布局参数
            lp.width = vWidth;
            lp.height = vHeight;
//            surfaceView.setLayoutParams(lp);
        } else {
            lp.width = vWidth;
            lp.height = vHeight;
        }
        surfaceView.post(() -> surfaceView.setLayoutParams(lp));
    }

    private void initDisposables() {
        compositeDisposable = new CompositeDisposable();
        d5 = routerViewModel.getTimeObserver()
                // 判断是否到达seqIn或seqOut位置
                .filter(aLong -> filterPositionAndUpdateOverlay(5, aLong))
                .observeOn(Schedulers.computation())
                .subscribe(aLong -> {
                    consumeEvent(viewId2Player.get(5), aLong);
                });
        d4 = routerViewModel.getTimeObserver()
                .filter(aLong -> filterPositionAndUpdateOverlay(4, aLong))
                .observeOn(Schedulers.computation())
                .subscribe(aLong -> {
                    consumeEvent(viewId2Player.get(4), aLong);
                });
        d3 = routerViewModel.getTimeObserver()
                .filter(aLong -> filterPositionAndUpdateOverlay(3, aLong))
                .observeOn(Schedulers.computation())
                .subscribe(aLong -> {
                    consumeEvent(viewId2Player.get(3), aLong);
                });
        d2 = routerViewModel.getTimeObserver()
                .filter(aLong -> filterPositionAndUpdateOverlay(2, aLong))
                .observeOn(Schedulers.computation())
                .subscribe(aLong -> {
                    consumeEvent(viewId2Player.get(2), aLong);
                });
        d1 = routerViewModel.getTimeObserver()
                .filter(aLong -> filterPositionAndUpdateOverlay(1, aLong))
                .observeOn(Schedulers.computation())
                .subscribe(aLong -> {
                    consumeEvent(viewId2Player.get(1), aLong);
                });
        compositeDisposable.addAll(d5, d4, d3, d2, d1);
    }

    private boolean filterPositionAndUpdateOverlay(int viewId, long position) {
        PlayerImpl temp = viewId2Player.get(viewId);
        if (temp != null) {
            if (position == -1) {
                // -1为暂停
                temp.pause();
                return false;
            }
            SurfaceView surfaceView = (SurfaceView) surfaceContainer.getChildAt(viewId);
            if (temp.getSegId(position) != null) {
//                VideoModel videoModel = temp.getWH(temp.getSegId(position));
//                recalculationScreen(surfaceView, videoModel.vWidth, videoModel.vHeight);
                surfaceView.setZOrderMediaOverlay(true);
                surfaceView.getHolder().setFormat(PixelFormat.OPAQUE);
                return true;
            }
            if (temp.getSegOut(position) != null) {
//                recalculationScreen(surfaceView, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
                surfaceView.setZOrderMediaOverlay(false);
                surfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
                return false;
            }
        }
        return false;
    }

    private void consumeEvent(PlayerImpl player, long position) {
        if (player == null) return;
        player.play(position);
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
            entry.getValue().releaseAll();
        }
        viewId2Player.clear();
    }
}
