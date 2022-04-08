package com.chenfu.avdioedit.util

object IdUtils {
    private var trackId = -1
    private var segmentId = -1

    fun getNewestTrackId(): Int {
        trackId += 1
        return trackId
    }

    fun getNewestSegmentId(): Int {
        segmentId += 1
        return segmentId
    }
}