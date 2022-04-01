package com.chenfu.avdioedit.model.data

import java.util.*

class CropModel {
    var containerId = -1
    var segmentId = -1
    var cursorOffset = 0L
    // 轨道总和Map
    var mediaTrackMap: TreeMap<Int, MediaTrack> ?= null

    fun getTrack(): MediaTrack? {
        return if (containerId != -1) {
            mediaTrackMap?.get(containerId)?.clone()
        } else {
            null
        }
    }

    fun getTrackSeg(): MediaTrack? {
        return if (containerId != -1 && segmentId != -1) {
            // 只要有一个为null就是null
            mediaTrackMap?.get(containerId)?.childMedias?.get(segmentId)?.clone()
        } else {
            null
        }
    }
}