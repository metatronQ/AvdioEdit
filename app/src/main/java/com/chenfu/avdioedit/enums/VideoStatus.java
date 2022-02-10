package com.chenfu.avdioedit.enums;

public enum VideoStatus {
    /**
     * 准备阶段
     */
    PREPARE(0),
    /**
     * 准备完成
     */
    PREPARE_FINISH(1),
    /**
     * 开始播放视频
     */
    START(2),
//    /**
//     * 正在播放视频
//     */
//    PLAYING(3),
    /**
     * 暂停视频
     */
    PAUSE(4),
    /**
     * 视频播放结束（停止视频）
     */
    STOP(5);

    private int type = 0;

    VideoStatus(int type) {
        this.type = type;
    }

    public static VideoStatus from(int type) {
        switch (type) {
            case 0:
                return PREPARE;
            case 1:
                return PREPARE_FINISH;
            case 2:
                return START;
//            case 3:
//                return PLAYING;
            case 4:
                return PAUSE;
            case 5:
                return STOP;
            default:
                return PREPARE;
        }
    }

    public int getType() {
        return type;
    }
}
