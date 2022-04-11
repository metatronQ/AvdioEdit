package com.chenfu.avdioedit.model.impl;

import android.content.Context;

import androidx.lifecycle.ViewModel;

import com.chenfu.avdioedit.Interface.LeftEditInterface;
import com.chenfu.avdioedit.model.data.ClipModel;
import com.chenfu.avdioedit.model.data.FramesType;
import com.chenfu.avdioedit.util.FFmpegUtils;
import com.chenfu.avdioedit.model.data.MediaTrackModel;
import com.chenfu.avdioedit.util.IdUtils;
import com.chenfu.avdioedit.viewmodel.LeftEditViewModel;
import com.example.ndk_source.util.LogUtil;
import com.example.ndk_source.util.ToastUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.TreeMap;
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
    public void crop(Context context, ClipModel clipModel, TreeMap<Integer, MediaTrackModel> map) {
        LogUtil.INSTANCE
                .manualPkgName("com.chenfu.avdioedit")
                .d(clipModel.getContainerId() + " " + clipModel.getSegmentId() + " " + clipModel.getCursorOffset());
        MediaTrackModel track = clipModel.getTrack(map);
        MediaTrackModel trackSeg = clipModel.getTrackSeg(map);
        if (track == null || trackSeg == null) {
            return;
        }

        if (clipModel.getCursorOffset() <= trackSeg.getSeqIn() || clipModel.getCursorOffset() >= trackSeg.getSeqOut()) {
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

        if (clipModel.getCursorOffset() - trackSeg.getSeqIn() < frameLength
                || trackSeg.getSeqOut() - clipModel.getCursorOffset() < frameLength) {
            ToastUtil.INSTANCE.show(context, "裁剪后片段小于1帧");
            return;
        }

        long offsetReal = clipModel.getCursorOffset() - trackSeg.getSeqIn();

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

        MediaTrackModel mediaTrackModelOut1 = trackSeg.clone();
        // 裁剪的前一段id = 选中
        mediaTrackModelOut1.setId(trackSeg.getId());
        mediaTrackModelOut1.setPath(outPath1);
        mediaTrackModelOut1.setSeqIn(trackSeg.getSeqIn());
        mediaTrackModelOut1.setSeqOut(clipModel.getCursorOffset());
        mediaTrackModelOut1.setDuration(mediaTrackModelOut1.getSeqOut() - mediaTrackModelOut1.getSeqIn());
        mediaTrackModelOut1.getChildMedias().clear();

        MediaTrackModel mediaTrackModelOut2 = mediaTrackModelOut1.clone();
        // 裁剪的后一段id = 选中 + 1
        mediaTrackModelOut2.setId(IdUtils.INSTANCE.getNewestSegmentId());
        mediaTrackModelOut2.setPath(outPath2);
        mediaTrackModelOut2.setSeqIn(clipModel.getCursorOffset());
        mediaTrackModelOut2.setSeqOut(trackSeg.getSeqOut());
        mediaTrackModelOut2.setDuration(mediaTrackModelOut2.getSeqOut() - mediaTrackModelOut2.getSeqIn());

        track.getChildMedias().remove(trackSeg.getId());
        track.getChildMedias().put(mediaTrackModelOut1.getId(), mediaTrackModelOut1);
        track.getChildMedias().put(mediaTrackModelOut2.getId(), mediaTrackModelOut2);

        // 裁剪输出应该是两段文件
        String cmdString = spliceString(
                "-y",
                "-i", trackSeg.getPath(),
                "-ss" + "  " + format0 + "  " + "-t" + "  " + formatOffset + "  " + "-c  copy", outPath1,
                "-ss" + "  " + formatOffset + "  " + "-t" + "  " + formatOff2Dura + "  " + "-c  copy", outPath2
        );
        start(context, cmdString, new String[]{"裁剪成功", "裁剪超时"}, () -> leftEditViewModel.clipResultLiveData.setValue(track));
    }

    @Override
    public void merge(Context context, ClipModel firstClip, ClipModel secondClip, TreeMap<Integer, MediaTrackModel> map) {
        // 第二段加在第一段后面
        MediaTrackModel trackModel1 = firstClip.getTrack(map);
        MediaTrackModel segModel1 = firstClip.getTrackSeg(map);
        MediaTrackModel trackModel2 = secondClip.getTrack(map);
        MediaTrackModel segModel2 = secondClip.getTrackSeg(map);
        if (trackModel1 == null || segModel1 == null || trackModel2 == null || segModel2 == null) {
            return;
        }
        if (segModel1.getType() != segModel2.getType()) {
            ToastUtil.INSTANCE.show(context, "合并的文件格式需保持一致");
            return;
        }

        String[] src = segModel1.getPath().split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < src.length - 1; i++) {
            sb.append(src[i]).append("/");
        }
        String outDirPath = sb.toString();
        String[] srcNameStructure = src[src.length - 1].split("\\.");
        String outPath = outDirPath + srcNameStructure[0] + "-merge" + "." + srcNameStructure[1];

        String readFilePath = outDirPath + "file.txt";
        String writeContent = "file" + " " + "'" + segModel1.getPath() + "'" + "\n"
                + "file" + " " + "'" + segModel2.getPath() + "'";
        createAndWrite2File(readFilePath, writeContent);

        // 同一轨道只需发送一次更新信令
        if (trackModel1.getId() == trackModel2.getId()) {
            trackModel1.getChildMedias().remove(segModel2.getId());
        } else {
            // 清除seg2
            trackModel2.getChildMedias().remove(segModel2.getId());
            trackModel2.updateDuration();
        }

        // 更新seg1
        segModel1.setPath(outPath);
        long newSeqOut = segModel1.getSeqOut() + segModel2.getDuration();
        segModel1.setSeqOut(newSeqOut);
        segModel1.setDuration(segModel1.getSeqOut() - segModel1.getSeqIn());
        trackModel1.getChildMedias().put(segModel1.getId(), segModel1);
        trackModel1.keepNotOverlap();

        String cmdString = spliceString(
                "-y" + "  " + "-f" + "  " + "concat" + "  " + "-safe" + "  " + "0",
                "-i", readFilePath,
                "-c  copy", outPath
        );
        start(context, cmdString, new String[]{"拼合成功", "拼合超时"}, () -> {
            if (trackModel1.getId() != trackModel2.getId()) {
                leftEditViewModel.clipResultLiveData.setValue(trackModel2);
            }
            leftEditViewModel.clipResultLiveData.setValue(trackModel1);
        });
    }

    public void start(Context context, String cmd, String[] toastStrings, Callback callback) {
        AtomicBoolean isOut = new AtomicBoolean(false);
        FFmpegUtils.INSTANCE.generateCmd(cmd, () -> isOut.set(true)).start(context);
        if (timer != null && !timer.isDisposed()) timer.dispose();
        timer = Observable.interval(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    if (aLong > 10) {
                        ToastUtil.INSTANCE.show(context, toastStrings[1]);
                        FFmpegUtils.INSTANCE.killRunningProcesses();
                        timer.dispose();
                    }
                    if (isOut.get()) {
                        ToastUtil.INSTANCE.show(context, toastStrings[0]);
                        callback.success();
                        timer.dispose();
                    }
                });
    }

    private String spliceString(String ...values) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < values.length - 1; i++) {
            stringBuilder.append(values[i]).append("  ");
        }
        return stringBuilder.append(values[values.length - 1]).toString();
    }

    private void createAndWrite2File(String filePath, String content) {
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(content.getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setViewModel(ViewModel viewModel) {
        this.leftEditViewModel = (LeftEditViewModel) viewModel;
    }

    private interface Callback {
        void success();
    }
}
