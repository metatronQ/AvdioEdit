package com.chenfu.avdioedit.model.data

import java.util.*

class CropModel {
    var containerId = -1
    var segmentId = -1
    var cursorOffset = 0L
    // 轨道总和Map
    var mediaTrackModelMap: TreeMap<Int, MediaTrackModel> ?= null

    fun getTrack(): MediaTrackModel? {
        return if (containerId != -1) {
            mediaTrackModelMap?.get(containerId)?.clone()
        } else {
            null
        }
    }

    fun getTrackSeg(): MediaTrackModel? {
        return if (containerId != -1 && segmentId != -1) {
            // 只要有一个为null就是null
            mediaTrackModelMap?.get(containerId)?.childMedias?.get(segmentId)?.clone()
        } else {
            null
        }
    }
}