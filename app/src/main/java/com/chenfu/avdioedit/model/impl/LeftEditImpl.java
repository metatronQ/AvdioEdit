package com.chenfu.avdioedit.model.impl;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import com.chenfu.avdioedit.Interface.LeftEditInterface;
import com.chenfu.avdioedit.model.data.CropModel;
import com.chenfu.avdioedit.model.data.FramesType;
import com.chenfu.avdioedit.util.FFmpegUtils;
import com.chenfu.avdioedit.model.data.MediaTrack;
import com.chenfu.avdioedit.viewmodel.LeftEditViewModel;
import com.example.ndk_source.util.LogUtil;
import com.example.ndk_source.util.ToastUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class LeftEditImpl implements LeftEditInterface {
    Disposable timer;

    private LeftEditViewModel leftEditViewModel;

    public LeftEditImpl(LeftEditViewModel leftEditViewModel) {
        setViewModel(leftEditViewModel);
    }

    @Override
    public void launch() {
        // 由于SAF和动态权限都需要依赖fragment或activity，但是搭的框架impl中不需要持有有生命周期的对象
        // 因此这里直接赋值boolean的livedata通知在fragment或activity中saf启动
        // 即将获取path的方法交给了view层，一般来说是不应该的，只是这里获取数据的过程比较特殊
        leftEditViewModel.launchSaf.setValue(true);
    }

    @Override
    public void crop(Context context, CropModel cropModel) {
        LogUtil.INSTANCE
                .manualPkgName("com.chenfu.avdioedit")
                .d(cropModel.getContainerId() + " " + cropModel.getSegmentId() + " " + cropModel.getCursorOffset());
        MediaTrack track = cropModel.getTrack();
        MediaTrack trackSeg = cropModel.getTrackSeg();
        if (track == null || trackSeg == null) {
            return;
        }

        if (cropModel.getCursorOffset() <= trackSeg.getSeqIn() || cropModel.getCursorOffset() >= trackSeg.getSeqOut()) {
            ToastUtil.INSTANCE.show(context, "未在选中范围内");
            return;
        }

        if (trackSeg.getFrames() == FramesType.FRAMES_UNKNOWN) {
            ToastUtil.INSTANCE.show(context, "无效帧数");
            return;
        }
        // 1000 / 24 = 41.66666...
        // 30、60同理
        long frameLength = (long) Math.ceil(1000.0 / trackSeg.getFrames());

        if (cropModel.getCursorOffset() - trackSeg.getSeqIn() < frameLength
                || trackSeg.getSeqOut() - cropModel.getCursorOffset() < frameLength) {
            ToastUtil.INSTANCE.show(context, "裁剪后片段小于1帧");
            return;
        }

        long offsetReal = cropModel.getCursorOffset() - trackSeg.getSeqIn();

        // 一次裁剪应该裁剪两个片段出来，且不覆盖源文件
        // 非/下文件
        String[] src = trackSeg.getPath().split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < src.length - 1; i++) {
            sb.append(src[i]).append("/");
        }
        String outDirPath = sb.toString();
        String[] srcNameStructure = src[src.length - 1].split("\\.");
        String outPath1 = outDirPath + srcNameStructure[0] + "-1" + "." + srcNameStructure[1];
        String outPath2 = outDirPath + srcNameStructure[0] + "-2" + "." + srcNameStructure[1];


        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");
        // 设置时区为从0开始，注意HH:mm 是24小时制，hh:mm是12小时制
        // 注意毫秒前是.
        fmt.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        String format0 = fmt.format(new Date(0));
        String formatOffset = fmt.format(new Date(offsetReal));
        String formatDuration = fmt.format(new Date(trackSeg.getDuration()));
        String formatOff2Dura = fmt.format(new Date(trackSeg.getDuration() - offsetReal));

        MediaTrack mediaTrackOut1 = trackSeg.clone();
        // 裁剪的前一段id = 选中
        mediaTrackOut1.setId(trackSeg.getId());
        mediaTrackOut1.setPath(outPath1);
        mediaTrackOut1.setSeqIn(trackSeg.getSeqIn());
        mediaTrackOut1.setSeqOut(cropModel.getCursorOffset());
        mediaTrackOut1.setDuration(mediaTrackOut1.getSeqOut() - mediaTrackOut1.getSeqIn());
        mediaTrackOut1.getChildMedias().clear();

        MediaTrack mediaTrackOut2 = mediaTrackOut1.clone();
        // 裁剪的后一段id = 选中 + 1
        mediaTrackOut2.setId(trackSeg.getId() + 1);
        mediaTrackOut2.setPath(outPath2);
        mediaTrackOut2.setSeqIn(cropModel.getCursorOffset());
        mediaTrackOut2.setSeqOut(trackSeg.getSeqOut());
        mediaTrackOut2.setDuration(mediaTrackOut2.getSeqOut() - mediaTrackOut2.getSeqIn());

        track.getChildMedias().remove(trackSeg.getId());
        track.getChildMedias().put(mediaTrackOut1.getId(), mediaTrackOut1);
        track.getChildMedias().put(mediaTrackOut2.getId(), mediaTrackOut2);

        AtomicBoolean isOut = new AtomicBoolean(false);
        FFmpegUtils.INSTANCE.CropVideoByCopy(
                context,
                trackSeg.getPath(),
                outPath1,
                "-ss" + " "
                        + format0 + " "
                        + "-t" + " "
                        + formatOffset,
                outPath2,
                "-ss" + " "
                        + formatOffset + " "
                        + "-t" + " "
                        + formatOff2Dura,
                () -> isOut.set(true));

        if (timer != null && !timer.isDisposed()) timer.dispose();
        timer = Observable.interval(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    if (aLong > 10) {
                        ToastUtil.INSTANCE.show(context, "裁剪超时");
                        FFmpegUtils.INSTANCE.killRunningProcesses();
                        timer.dispose();
                    }

                    if (isOut.get()) {
                        ToastUtil.INSTANCE.show(context, "裁剪成功");
                        leftEditViewModel.cropResultLiveData.setValue(track);
                        timer.dispose();
                    }
                });
    }

    @Override
    public void setViewModel(ViewModel viewModel) {
        this.leftEditViewModel = (LeftEditViewModel) viewModel;
    }
}
