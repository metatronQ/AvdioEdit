package com.chenfu.avdioedit.view.multitrack.model

import android.os.Parcel
import android.os.Parcelable

class MediaTrack() : Parcelable, Comparable<MediaTrack> {
    var id: Int = -1
    var type: Int = MediaType.TYPE_UNKNOWN
    var seqIn = 0L
    var seqOut = 0L
    var duration = 0L
    var path = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readInt()
        type = parcel.readInt()
        seqIn = parcel.readLong()
        seqOut = parcel.readLong()
        duration = parcel.readLong()
        path = parcel.readString().toString()
    }

    constructor(id: Int, type: Int) : this(Parcel.obtain()) {
        this.id = id
        this.type = type
    }

    override fun compareTo(other: MediaTrack): Int = this.id.compareTo(other.id)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(type)
        parcel.writeLong(seqIn)
        parcel.writeLong(seqOut)
        parcel.writeLong(duration)
        parcel.writeString(path)
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
}

object MediaType {
    const val TYPE_UNKNOWN = -1
    const val TYPE_VIDEO = 0
    const val TYPE_AUDIO = 1
    const val TYPE_VIDEO_REF = 2
    const val TYPE_AUDIO_REF = 3
}