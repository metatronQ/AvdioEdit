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
import com.chenfu.avdioedit.model.data.MediaTrackModel
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
    private val tMap = TreeMap<Int, MediaTrackModel>()
    private val vMap = TreeMap<Int, SegmentContainer>()
    private var originWidth = 0
    private var mVideoColor = Color.DKGRAY
    private var mAudioColor = Color.BLACK
    private lateinit var multiViewModel: MultiTrackViewModel

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
        multiViewModel.updateSelectedStatusListener = object : SegmentContainer.UpdateSelectedStatusListener {
            override fun update(lastContainerId: Int, lastSegId: Int) {
                vMap[lastContainerId]?.clearLastSelectedStatus(lastSegId)
            }
        }
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

    fun setDuration(us: Long) {
        if (tMap.size == 0) {
            mTimeView.setDuration(us)
            return
        }
        if (us > mTimeView.getDuration()) {
            mTimeView.setDuration(us)
            tMap.forEach {
                it.value.duration = mTimeView.getDuration()
            }
        }
    }

    fun getTrackMap() = tMap

    fun getTimeDuration() = mTimeView.getDuration()

    fun addTrack(trackModel: MediaTrackModel) {
        if (tMap.containsKey(trackModel.id)) {
            return
        }
        // add ????????????????????????
        trackModel.duration = mTimeView.getDuration()
        tMap[trackModel.id] = trackModel
        vMap[trackModel.id] = SegmentContainer(context, trackModel)
        vMap[trackModel.id]!!.setViewModel(multiViewModel)
//        val padding = applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f).toInt()
//        vMap[track.id]?.setPadding(padding, padding, padding, padding)
        addView(vMap[trackModel.id], makeLayoutParams())
        requestLayout()
//        updateAudioTrack(track)
    }

    fun updateTrack(trackModel: MediaTrackModel) {
        if (!tMap.containsKey(trackModel.id)) {
            return
        }
        // ????????????maxDuration??????????????????duration????????????
        setDuration(trackModel.duration)
        trackModel.duration = mTimeView.getDuration()
        tMap[trackModel.id] = trackModel
        vMap[trackModel.id]?.updateAllSegment(trackModel)
        requestLayout()
//        updateAudioTrack(track)
    }

    fun deleteTrackOrSeg(containerId: Int, segId: Int) {
        if (segId == -1) {
            tMap.remove(containerId)
            removeView(vMap[containerId])
            vMap.remove(containerId)
        } else {
            tMap[containerId]?.let {
                it.childMedias.remove(segId)
                vMap[containerId]?.updateAllSegment(it)
            }
        }
        var maxSeqOut = 0L
        vMap.forEach {
            maxSeqOut = if (it.value.getMaxSeqOut() >= maxSeqOut) {
                it.value.getMaxSeqOut()
            } else {
                maxSeqOut
            }
        }
        // ???????????????????????????
        if (maxSeqOut < mTimeView.getDuration()) {
            mTimeView.setDuration(maxSeqOut)
            tMap.forEach {
                // ???????????????????????????????????????????????????????????????track???
                it.value.duration = mTimeView.getDuration()
            }
        }
        requestLayout()
        // ?????????????????????
        multiViewModel.run {
            clipModel.segmentId = -1
            clipModel.containerId = -1
        }
    }

    fun getViewId(trackId: Int): Int {
        // surface??????5???????????????????????????????????????0???????????????View
        // 5????????????view
        var realViewId = 5
        // 0 ???????????????childCount??????6????????????1???
        for (viewId in 1..childCount) {
            val segmentContainer = getChildAt(viewId) as SegmentContainer
            if (segmentContainer.getContainerId() == trackId) {
                return 5 - (viewId - 1)
            }
        }
        return -1
    }

    fun setScale(scale: ScaleRational) {
        this.scale.num = scale.num
        this.scale.den = scale.den
        requestLayout()
    }

    private fun updateAudioTrack(trackModel: MediaTrackModel) {
        if (MediaType.TYPE_AUDIO == trackModel.type) {
            GlobalScope.launch {
                val src = File(trackModel.path)
                if (!src.exists()) {
                    return@launch
                }
                val file = File("${context.externalCacheDir!!.path}/${File(trackModel.path).name}.bmp")
//                AlFFUtils.exec("ffmpeg -i ${src.absolutePath} -lavfi showwavespic=s=720x60:colors=orange:scale=sqrt -f image2 ${file.absolutePath}")
                if (!file.exists()) {
                    return@launch
                }
                post {
                    vMap[trackModel.id]?.background =
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

        // TODO: ?????????key?????????????????????
        vMap.forEach {
            val track = tMap[it.key]
            val view = it.value

            w = measuredWidth - paddingLeft - paddingRight
            h = DisplayUtils.dip2px(context, 50f)
            var offset = 0
            if (track != null && mTimeView.getDuration() > 0 && track.duration > 0) {
                offset = (track.seqIn * w / mTimeView.getDuration()).toInt()
                w = (track.duration * w / mTimeView.getDuration()).toInt()
            }
            view.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), h)
            view.layout(paddingLeft + l + offset, height, paddingLeft + l + w, height + h)
            height += h
        }
    }

    companion object {
        const val TAG = "TrackView"
    }
}