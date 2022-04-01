package com.chenfu.avdioedit.view.multitrack

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import androidx.annotation.AttrRes
import com.chenfu.avdioedit.util.DisplayUtils
import com.chenfu.avdioedit.model.data.MediaTrack
import com.chenfu.avdioedit.model.data.MediaType
import com.chenfu.avdioedit.view.multitrack.widget.ScaleRational
import com.chenfu.avdioedit.viewmodel.MultiTrackViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class TrackView : ViewGroup, BaseView {
    private lateinit var mTimeView: TimelineView
    private val scale = ScaleRational(1, 1)
    private val tMap = TreeMap<Int, MediaTrack>()
    private val vMap = TreeMap<Int, SegmentContainer>()
    private var originWidth = 0
    private var mVideoColor = Color.DKGRAY
    private var mAudioColor = Color.BLACK
    private lateinit var multiViewModel: MultiTrackViewModel

    // 视频轨最大长度
    public var maxDuration = 0L

    constructor(context: Context) : super(context) {
        onResolveAttribute(context, null, 0, 0)
        onInitialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?)
            : super(context, attrs) {
        onResolveAttribute(context, attrs, 0, 0)
        onInitialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        onResolveAttribute(context, attrs, defStyleAttr, 0)
        onInitialize(context)
    }

    override fun onResolveAttribute(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {

    }

    override fun onInitialize(context: Context) {
        clipToPadding = false
        mTimeView = TimelineView(context)
        mTimeView.setPadding(
            0, applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f).toInt(),
            0, applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f).toInt()
        )
        addView(mTimeView, makeLayoutParams())
    }

    override fun setViewModel(multiTrackViewModel: MultiTrackViewModel) {
        this.multiViewModel = multiTrackViewModel
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        mTimeView.setPadding(left, mTimeView.paddingTop, right, mTimeView.paddingBottom)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        if (originWidth <= 0) {
            originWidth = width
        }
        setMeasuredDimension(
            originWidth * scale.num / scale.den + paddingLeft + paddingRight,
            height
        )
    }

    fun setDuration(us: Long, frames: Int) {
        mTimeView.setDuration(us, frames)
    }

    fun getTrackMap() = tMap

    fun addTrack(track: MediaTrack) {
        if (tMap.containsKey(track.id)) {
            return
        }
        // FIXME 需要重新计算maxDuration
        maxDuration = track.duration
        mTimeView.setDuration(maxDuration, track.frames)

        tMap[track.id] = track
        vMap[track.id] = SegmentContainer(context, track)
        vMap[track.id]!!.setViewModel(multiViewModel)
//        val padding = applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f).toInt()
//        vMap[track.id]?.setPadding(padding, padding, padding, padding)
        addView(vMap[track.id], makeLayoutParams())
        requestLayout()
//        updateAudioTrack(track)
    }

    fun updateTrack(track: MediaTrack) {
        if (!tMap.containsKey(track.id)) {
            return
        }
        // FIXME 需要重新计算maxDuration
        maxDuration = track.duration
        mTimeView.setDuration(maxDuration, track.frames)

        tMap[track.id] = track
        vMap[track.id]?.updateAllSegment(track)
        requestLayout()
//        updateAudioTrack(track)
    }

    fun setScale(scale: ScaleRational) {
        this.scale.num = scale.num
        this.scale.den = scale.den
        requestLayout()
    }

    private fun updateAudioTrack(track: MediaTrack) {
        if (MediaType.TYPE_AUDIO == track.type) {
            GlobalScope.launch {
                val src = File(track.path)
                if (!src.exists()) {
                    return@launch
                }
                val file = File("${context.externalCacheDir!!.path}/${File(track.path).name}.bmp")
//                AlFFUtils.exec("ffmpeg -i ${src.absolutePath} -lavfi showwavespic=s=720x60:colors=orange:scale=sqrt -f image2 ${file.absolutePath}")
                if (!file.exists()) {
                    return@launch
                }
                post {
                    vMap[track.id]?.background =
                        BitmapDrawable(resources, BitmapFactory.decodeFile(file.absolutePath))
                }
            }
        }
    }

    private fun makeLayoutParams(): LayoutParams {
        return LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
    }

    private fun applyDimension(unit: Int, value: Float): Float {
        val r = if (context == null) {
            Resources.getSystem()
        } else {
            context.resources
        }
        return TypedValue.applyDimension(unit, value, r.displayMetrics)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var height = 0

        var w = measuredWidth
        var h = mTimeView.measuredHeight
        mTimeView.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), h)
        mTimeView.layout(l, height, l + w, height + h)
        height += h

        vMap.forEach {
            val track = tMap[it.key]
            val view = it.value

            w = measuredWidth - paddingLeft - paddingRight
            h = DisplayUtils.dip2px(context, 50f)
//            var offset = 0
//            if (null != track && mTimeView.getDuration() > 0 && track.duration > 0) {
//                offset = (track.seqIn * w / mTimeView.getDuration()).toInt()
//                w = (track.duration * w / mTimeView.getDuration()).toInt()
//            }
//            view.layout(paddingLeft + l, height, paddingLeft + l + w, height + h)
//            view.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), h)

            view.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), h)
            view.layout(paddingLeft + l, height, paddingLeft + l + w, height + h)
            height += h
        }
    }

    companion object {
        const val TAG = "TrackView"
    }
}