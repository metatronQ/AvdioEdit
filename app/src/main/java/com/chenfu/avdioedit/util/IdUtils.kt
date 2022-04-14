package com.chenfu.avdioedit.util

object IdUtils {
    // track是独一无二的
    private var trackId = -1
    // seg是独一无二的
    private var segmentId = -1
    private val segId2SurfaceMap = HashMap<Int, Int>()

    fun getNewestTrackId(): Int {
        trackId += 1
        return trackId
    }

    /**
     * 只有全局新增的seg才会获取新id
     */
    fun getNewestSegmentId(): Int {
        segmentId += 1
        return segmentId
    }

    /**
     * 使用segId建立与surface的对应关系，同理也使用segId对应相应的mediaplayer
     */
    fun setSegId2Surface(segId: Int, surfaceId: Int) {
        segId2SurfaceMap[segId] = surfaceId
    }

    fun getSurfaceIdFromSeg(segId: Int): Int? {
        return segId2SurfaceMap[segId]
    }
}