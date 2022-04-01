package com.chenfu.avdioedit.model.data

import android.os.Parcel
import android.os.Parcelable

class MediaTrack() : Parcelable, Comparable<MediaTrack> {
    // -1代表初始或新增轨道
    var id: Int = -1
    var childMedias: HashMap<Int, MediaTrack> = HashMap()
    var type: Int = MediaType.TYPE_UNKNOWN
    var seqIn = 0L
    var seqOut = 0L
    // duration 应该等于seqOut-seqIn，无关是否是片段
    var duration = 0L
    var path = ""
    var frames = FramesType.FRAMES_UNKNOWN

    constructor(parcel: Parcel) : this() {
        id = parcel.readInt()
        childMedias = parcel.readHashMap(MediaTrack::class.java.classLoader) as HashMap<Int, MediaTrack>
        type = parcel.readInt()
        seqIn = parcel.readLong()
        seqOut = parcel.readLong()
        duration = parcel.readLong()
        path = parcel.readString().toString()
        frames = parcel.readInt()
    }

    constructor(id: Int, type: Int) : this(Parcel.obtain()) {
        this.id = id
        this.type = type
    }

    override fun compareTo(other: MediaTrack): Int = this.id.compareTo(other.id)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeMap(childMedias)
        parcel.writeInt(type)
        parcel.writeLong(seqIn)
        parcel.writeLong(seqOut)
        parcel.writeLong(duration)
        parcel.writeString(path)
        parcel.writeInt(frames)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MediaTrack> {
        override fun createFromParcel(parcel: Parcel): MediaTrack {
            return MediaTrack(parcel)
        }

        override fun newArray(size: Int): Array<MediaTrack?> {
            return arrayOfNulls(size)
        }
    }

    fun clone() : MediaTrack {
        val clone = MediaTrack()
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
            clone.childMedias[it.key] = it.value
        }
        return clone
    }
}

object MediaType {
    const val TYPE_UNKNOWN = -1
    const val TYPE_VIDEO = 0
    const val TYPE_AUDIO = 1
    const val TYPE_VIDEO_REF = 2
    const val TYPE_AUDIO_REF = 3
}

object FramesType {
    const val FRAMES_UNKNOWN = -1
    const val FRAMES_24 = 24
    const val FRAMES_30 = 30
    const val FRAMES_60 = 60
}