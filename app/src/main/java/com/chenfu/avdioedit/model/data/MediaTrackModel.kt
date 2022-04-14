package com.chenfu.avdioedit.model.data

import android.os.Parcel
import android.os.Parcelable

class MediaTrackModel() : Parcelable, Comparable<MediaTrackModel> {
    // -1代表初始或新增轨道
    var id: Int = -1
    var childMedias: HashMap<Int, MediaTrackModel> = HashMap()
    var type: Int = MediaType.TYPE_UNKNOWN
    var seqIn = 0L
    var seqOut = 0L
    // duration 应该等于seqOut-seqIn，无关是否是片段
    var duration = 0L
    var path = ""
    var frames = FramesType.FRAMES_UNKNOWN

    var vWidth = 0
    var vHeight = 0

    constructor(parcel: Parcel) : this() {
        id = parcel.readInt()
        type = parcel.readInt()
        seqIn = parcel.readLong()
        seqOut = parcel.readLong()
        duration = parcel.readLong()
        path = parcel.readString().toString()
        frames = parcel.readInt()
        vWidth = parcel.readInt()
        vHeight = parcel.readInt()
        childMedias = parcel.readHashMap(MediaTrackModel::class.java.classLoader) as HashMap<Int, MediaTrackModel>
    }

//    constructor(id: Int, type: Int) : this(Parcel.obtain()) {
//        this.id = id
//        this.type = type
//    }

    override fun compareTo(other: MediaTrackModel): Int = this.id.compareTo(other.id)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(type)
        parcel.writeLong(seqIn)
        parcel.writeLong(seqOut)
        parcel.writeLong(duration)
        parcel.writeString(path)
        parcel.writeInt(frames)
        parcel.writeInt(vWidth)
        parcel.writeInt(vHeight)
        parcel.writeMap(childMedias)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaTrackModel> {
        override fun createFromParcel(parcel: Parcel): MediaTrackModel {
            return MediaTrackModel(parcel)
        }

        override fun newArray(size: Int): Array<MediaTrackModel?> {
            return arrayOfNulls(size)
        }
    }

    fun clone() : MediaTrackModel {
        val clone = MediaTrackModel()
        clone.id = this.id
        clone.type = this.type
        clone.seqIn = this.seqIn
        clone.seqOut = this.seqOut
        clone.duration = this.duration
        clone.path = this.path
        clone.frames = this.frames
        // 对象不能直接赋值
        clone.childMedias = HashMap()
        this.childMedias.forEach {
            clone.childMedias[it.key] = it.value.clone()
        }
        clone.vWidth = this.vWidth
        clone.vHeight = this.vHeight
        return clone
    }

    fun getSortedChildArray(): List<MediaTrackModel> {
        val array = ArrayList(this.childMedias.values)
        val sortedArray = array.sortedBy {
            it.seqIn
        }
        return sortedArray
    }

    fun updateDuration() {
        if (this.childMedias.size == 0) {
            this.duration = 0
            return
        }
        val sortedArray = getSortedChildArray()
        this.duration = sortedArray[sortedArray.size - 1].seqOut
    }

    /**
     * 对segment根据seqIn从小到大排序
     * 然后以冒泡的方式更新重叠的seg
     */
    fun keepNotOverlap() {
        val sortedArray = getSortedChildArray()
        for (i in 0..sortedArray.size - 2) {
            compareNowAndNextTrack(sortedArray[i], sortedArray[i+1])
            this.childMedias[sortedArray[i].id] = sortedArray[i]
        }
        // 更新末尾
        this.childMedias[sortedArray[sortedArray.size - 1].id] = sortedArray[sortedArray.size - 1]
        this.duration = sortedArray[sortedArray.size - 1].seqOut
    }

    private fun compareNowAndNextTrack(nowTrackModel: MediaTrackModel, nextTrackModel: MediaTrackModel) {
        if (nowTrackModel.seqOut <= nextTrackModel.seqIn) {
            return
        }
        updateSegModelSeq(nextTrackModel, nowTrackModel.seqOut)
    }

    private fun updateSegModelSeq(segmentModel: MediaTrackModel, seqIn: Long) {
        segmentModel.seqIn = seqIn
        segmentModel.seqOut = segmentModel.seqIn + segmentModel.duration
    }
}

object MediaType {
    const val TYPE_UNKNOWN = -1
    const val TYPE_VIDEO = 0
    const val TYPE_AUDIO = 1
    const val TYPE_VIDEO_AUDIO = 2
//    const val TYPE_VIDEO_REF = 2
//    const val TYPE_AUDIO_REF = 3
}

object FramesType {
    const val FRAMES_UNKNOWN = -1
    const val FRAMES_10 = 10
    const val FRAMES_24 = 24
    const val FRAMES_30 = 30
    const val FRAMES_60 = 60
}