package com.chenfu.avdioedit.util

import android.media.MediaExtractor
import android.media.MediaFormat
import com.chenfu.avdioedit.model.data.VideoModel
import java.io.IOException


object SupportUtils {
    // 获取媒体信息的类，包括时长、宽高、帧率等
    // 新url需要新的对象
    private var mediaExtractor: MediaExtractor = MediaExtractor()
    private val supportVideoFormat = arrayOf("mp4")
    private val supportAudioFormat = arrayOf("mp3", "m4a")

    fun supportFormat(format: String): Int {
        if (supportVideoFormat.contains(format)) {
            return 1
        }
        if (supportAudioFormat.contains(format)) {
            return 0
        }
        return -1
    }

    // TODO：需要先调用
    fun setNewPath(url: String) {
        mediaExtractor.release()
        mediaExtractor = MediaExtractor();
        try {
            mediaExtractor.setDataSource(url)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getTrackCount() = mediaExtractor.trackCount

    /**
     * 一般都是一视多音吧，而且一般也是视频轨在0
     */
    fun getVideoTrackFormat(): MediaFormat? {
        for (i in 0 until getTrackCount()) {
            val mediaFormat: MediaFormat = mediaExtractor.getTrackFormat(i)
            val currentTrack = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (currentTrack?.startsWith("video/") == true) {
                return mediaFormat
            }
        }
        return null
    }

    fun getDuration(): Long {
        var duration = 0L
        var count = getTrackCount()
        for (i in 0 until getTrackCount()) {
            val mediaFormat: MediaFormat = mediaExtractor.getTrackFormat(i)
            val trackDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION)
            if (trackDuration > duration) {
                duration = trackDuration
            }
        }
        // 微秒转毫秒，舍去小数点后
        return duration / 1000
    }

    fun getFrameRate() = getVideoTrackFormat()?.getInteger(MediaFormat.KEY_FRAME_RATE)

    fun getVH(): VideoModel {
        val videoModel = VideoModel()
        val mediaFormat = getVideoTrackFormat()
        videoModel.vWidth = mediaFormat?.getInteger(MediaFormat.KEY_WIDTH) ?: 0
        videoModel.vHeight = mediaFormat?.getInteger(MediaFormat.KEY_HEIGHT) ?: 0
        return videoModel
    }

    fun release() {
        mediaExtractor.release()
    }
}