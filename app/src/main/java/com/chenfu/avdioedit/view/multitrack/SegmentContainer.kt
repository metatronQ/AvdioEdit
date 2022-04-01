package com.chenfu.avdioedit.view.multitrack

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AttrRes
import com.chenfu.avdioedit.util.DisplayUtils
import com.chenfu.avdioedit.model.data.MediaTrack
import com.chenfu.avdioedit.model.data.MediaType
import com.chenfu.avdioedit.viewmodel.MultiTrackViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class SegmentContainer : ViewGroup, BaseView {
    // 当前轨道
    private lateinit var mMediaTrack: MediaTrack
    private val tvMap = TreeMap<Int, TextView>()
    private val mVideoColor = Color.DKGRAY
    private val mAudioColor = Color.BLACK
    // -1表示未选中
    private var selectedId = -1
    private var multiViewModel: MultiTrackViewModel ?= null

    constructor(context: Context, mediaTrack: MediaTrack): super(context) {
        this.mMediaTrack = mediaTrack
        onResolveAttribute(context, null, 0, 0)
        onInitialize(context)
    }

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
    ) {}

    override fun onInitialize(context: Context) {
        resetSelected()
        mMediaTrack.childMedias.forEach {
            tvMap[it.key] = generateTv(it.value)
            addView(tvMap[it.key])
        }
    }

    override fun setViewModel(multiTrackViewModel: MultiTrackViewModel) {
        this.multiViewModel = multiTrackViewModel
        multiViewModel?.run {
            cropModel.containerId = mMediaTrack.id
        }
    }

    private fun generateTv(childTrack: MediaTrack) : TextView {
        val tv = TextView(context)
        tv.setOnLongClickListener {
            multiViewModel?.let {
                when (selectedId) {
                    -1 -> {
                        tv.isSelected = true
                        selectedId = childTrack.id
                        it.cropModel.segmentId = childTrack.id
                    }
                    childTrack.id -> {
                        tv.isSelected = false
                        selectedId = -1
                        it.cropModel.segmentId = -1
                    }
                    else -> {
                        tvMap[selectedId]?.isSelected = false
                        tv.isSelected = true
                        selectedId = childTrack.id
                        it.cropModel.segmentId = childTrack.id
                    }
                }
            }
            true
        }
        tv.textSize = 14f
        tv.setTextColor(Color.WHITE)
        tv.text = when (childTrack.type) {
            MediaType.TYPE_VIDEO -> "Video ${childTrack.id}"
            MediaType.TYPE_AUDIO -> "Audio ${childTrack.id}"
            else -> "Unknown"
        }
        tv.background = generateBg(childTrack)
        return tv
    }

    private fun generateBg(childTrack: MediaTrack) : Drawable {
        val drawableSelected = GradientDrawable()
        drawableSelected.run {
            setColor(when (childTrack.type) {
                MediaType.TYPE_VIDEO -> mVideoColor
                MediaType.TYPE_AUDIO -> mAudioColor
                else -> Color.RED
            })
            shape = GradientDrawable.RECTANGLE
            setStroke(DisplayUtils.dip2px(context, 2f), Color.YELLOW)
        }
        val drawableDefault = GradientDrawable()
        drawableDefault.run {
            setColor(when (childTrack.type) {
                MediaType.TYPE_VIDEO -> mVideoColor
                MediaType.TYPE_AUDIO -> mAudioColor
                else -> Color.RED
            })
            shape = GradientDrawable.RECTANGLE
            setStroke(DisplayUtils.dip2px(context, 1f), Color.GRAY)
        }
        val stateListDrawable = StateListDrawable()
        val selected = android.R.attr.state_selected
        stateListDrawable.addState(intArrayOf(selected), drawableSelected)
        stateListDrawable.addState(intArrayOf(-selected), drawableDefault)
        return stateListDrawable
    }

    fun updateAllSegment(track: MediaTrack) {
        this.mMediaTrack = track
        resetSelected()
        mMediaTrack.childMedias.forEach {
            tvMap[it.key] = generateTv(it.value)
            addView(tvMap[it.key])
        }
        requestLayout()
//        updateAudioTrack(track)
    }

    private fun resetSelected() {
        selectedId = -1
        multiViewModel?.run {
            cropModel.segmentId = -1
        }
        removeAllViews()
    }

    fun updateChildView(childTrack: MediaTrack) {

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
                    tvMap[track.id]?.background =
                        BitmapDrawable(resources, BitmapFactory.decodeFile(file.absolutePath))
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(
            width + paddingLeft + paddingRight,
            height
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        tvMap.forEach {
            val childTrack = mMediaTrack.childMedias[it.key]!!
            val childTv = it.value
            var w = measuredWidth - paddingLeft - paddingRight
            val h = measuredHeight
            var offsetLeft = 0
            var offsetRight = 0
            if (mMediaTrack.duration > 0) {
                offsetLeft = (childTrack.seqIn * w / mMediaTrack.duration).toInt()
                offsetRight = (childTrack.seqOut * w / mMediaTrack.duration).toInt()
                w = (childTrack.duration * w / mMediaTrack.duration).toInt()
            }
            childTv.layout(paddingLeft + offsetLeft, 0, paddingLeft + offsetRight, h)
            childTv.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), h)
        }
    }
}