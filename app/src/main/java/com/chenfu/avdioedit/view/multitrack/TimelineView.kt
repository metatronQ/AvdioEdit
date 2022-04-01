package com.chenfu.avdioedit.view.multitrack

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import com.chenfu.avdioedit.model.data.FramesType
import com.chenfu.avdioedit.viewmodel.MultiTrackViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class TimelineView : View, BaseView {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fmt = SimpleDateFormat("mm:ss:SSS")
    private var durationInUS = 0L
    private val textSize = Point()
    private val cursorRect = Rect()
    private var cursorSize = 3f
    private var spaceSize = 0f
    private val textVec = ArrayList<String>()
    private var mLastVisibleWidth = 0
    private var mFrames = FramesType.FRAMES_UNKNOWN

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
        paint.strokeWidth = 3f
        paint.color = Color.GRAY
        paint.textSize = applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f)
        val bounds = Rect()
        paint.getTextBounds("00:00:000", 0, 9, bounds)
        textSize.x = bounds.width()
        textSize.y = bounds.height()
        spaceSize = textSize.x.toFloat()

        cursorSize = applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f)
        cursorRect.set(0, 0, textSize.x / 2, cursorSize.toInt())
    }

    override fun setViewModel(multiTrackViewModel: MultiTrackViewModel) {
        TODO("Not yet implemented")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(
                width,
                textSize.y + paddingTop + paddingBottom
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

    fun setDuration(us: Long, frames: Int) {
        if (us > 0 && frames != FramesType.FRAMES_UNKNOWN) {
            this.durationInUS = us
            this.mFrames = frames
            post {
                textVec.clear()
                measureText()
                invalidate()
            }
        }
    }

    fun getDuration(): Long = durationInUS

    private fun keepZoomLevel(visibleWidth: Int): Int {
        if (abs(mLastVisibleWidth - visibleWidth) < 5) {
            return textVec.size
        }
        mLastVisibleWidth = visibleWidth
        val tmp = (visibleWidth - textSize.x * textVec.size) / (textVec.size - 1).toFloat()
        if (tmp < textSize.x + cursorRect.width() * 2 && tmp > cursorRect.width()) {
            spaceSize = tmp
            return textVec.size
        }
        return Int.MIN_VALUE
    }

    private fun measureText(): Int {
        if (durationInUS <= 0) {
            textVec.clear()
            return 0
        }
        // 包括起始的一个text长度的时间轴总长
        val visibleWidth = measuredWidth + textSize.x - paddingLeft - paddingRight
        var count = visibleWidth / (textSize.x + cursorRect.width())
        if (textVec.isNotEmpty()) {
            // keepZoomLevel重新测量间距
            if (Int.MIN_VALUE != keepZoomLevel(visibleWidth)) {
                return textVec.size
            }
            count = if (count < textVec.size) {
                textVec.size / 2
            } else {
                textVec.size * 2
            }
        }

        textVec.clear()
        if (count > 1) {
            spaceSize = (visibleWidth - textSize.x * count) / (count - 1).toFloat()
            for (i in 0 until count) {
                // durationInUS已经是毫秒级的参数，省略小数点后的部分，不需要再除1000
                val value = i * durationInUS / (count - 1)
                val scaleValue = fmt.format(Date(value))
                textVec.add(scaleValue)
            }
        } else {
            spaceSize = (visibleWidth - textSize.x).toFloat()
            textVec.add(fmt.format(Date(0)))
        }
        return count
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val count = measureText()
        for (i in 0 until count) {
            val text = textVec[i]
            val x = paddingLeft - textSize.x / 2f + (textSize.x + spaceSize) * i
            canvas?.drawText(text, x, (measuredHeight + textSize.y) / 2f, paint)
            if (i < count - 1) {
                canvas?.drawCircle(
                        x + textSize.x + spaceSize / 2f,
                        measuredHeight / 2f,
                        cursorSize / 2f, paint
                )
            }
        }
    }
}