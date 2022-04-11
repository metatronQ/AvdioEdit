package com.chenfu.avdioedit.model.data

import java.util.*

class ClipModel(var containerId: Int = -1, var segmentId: Int = -1) {
    var cursorOffset = 0L

    fun getTrack(mediaTrackModelMap: TreeMap<Int, MediaTrackModel>?): MediaTrackModel? {
        return if (containerId != -1) {
            mediaTrackModelMap?.get(containerId)?.clone()
        } else {
            null
        }
    }

    fun getTrackSeg(mediaTrackModelMap: TreeMap<Int, MediaTrackModel>?): MediaTrackModel? {
        return if (containerId != -1 && segmentId != -1) {
            // 只要有一个为null就是null
            mediaTrackModelMap?.get(containerId)?.childMedias?.get(segmentId)?.clone()
        } else {
            null
        }
    }
}