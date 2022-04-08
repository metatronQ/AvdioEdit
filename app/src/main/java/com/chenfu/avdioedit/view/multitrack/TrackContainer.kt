package com.chenfu.avdioedit.view.multitrack

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.HorizontalScrollView
import androidx.annotation.AttrRes
import androidx.core.view.GestureDetectorCompat
import com.chenfu.avdioedit.model.data.MediaTrackModel
import com.chenfu.avdioedit.view.multitrack.widget.ScaleRational
import com.chenfu.avdioedit.viewmodel.MultiTrackViewModel

/**
 * 参照简书
 * @url https://www.jianshu.com/p/1146f496398f
 */
class TrackContainer : HorizontalScrollView, BaseView {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private lateinit var mTrackView: TrackView
    private lateinit var mGestureDetector: GestureDetectorCompat
    private lateinit var mScaleDetector: ScaleGestureDetector
    private var mOnSeekBarChangeListener: OnSeekBarChangeListener? = null
    private var isDoublePointer = false
    private var cursorSize = 0f
    private var scale = 1f
    private var progress = 0f

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

    private fun applyDimension(unit: Int, value: Float): Float {
        val r = if (context == null) {
            Resources.getSystem()
        } else {
            context.resources
        }
        return TypedValue.applyDimension(unit, value, r.displayMetrics)
    }

    override fun onInitialize(context: Context) {
        cursorSize = applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f)
        paint.color = Color.BLUE
        mGestureDetector = GestureDetectorCompat(context, onGestureListener)
        mScaleDetector = ScaleGestureDetector(context, mScaleListener)
        mTrackView = TrackView(context)
        addView(mTrackView)
        post {
            if (childCount > 1) {
                throw RuntimeException("TrackContainer not support multi child view.")
            }
            if (childCount == 1 && getChildAt(0) !is TrackView) {
                throw RuntimeException("TrackContainer`s child view must be TrackView.")
            }
        }
    }

    override fun setViewModel(multiTrackViewModel: MultiTrackViewModel) {
        getChildView().setViewModel(multiTrackViewModel)
    }

    fun getChildView(): TrackView = mTrackView

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
//        LogUtil.packageName(context).v("$l $t $oldl $oldt")
        if (!isDoublePointer) {
            // scrollX / (mTrackView.measuredWidth - measuredWidth).toFloat()为滚动的距离占缩放后的originWidth的百分比
            // measuredWidth为两个padding长
            // 可以用来确定裁剪的位置
            mOnSeekBarChangeListener?.onProgressChanged(
                l / (mTrackView.measuredWidth - measuredWidth).toFloat(),
                true
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mOnSeekBarChangeListener?.onStartTrackingTouch()
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                isDoublePointer = true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                isDoublePointer = false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDoublePointer = false
                mOnSeekBarChangeListener?.onStopTrackingTouch()
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDoublePointer && mScaleDetector.onTouchEvent(event)) {
                    setProgress(progress)
                    return true
                }
            }
        }
        mGestureDetector.onTouchEvent(event)
        mScaleDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        mTrackView.setPadding(width / 2, 0, width / 2, 0)
    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        canvas?.drawRoundRect(
            scrollX + (measuredWidth - cursorSize) / 2,
            0f,
            scrollX + (measuredWidth + cursorSize) / 2,
            measuredHeight.toFloat(),
            cursorSize / 2f,
            cursorSize / 2f,
            paint
        )
    }

    fun setDuration(us: Long) {
        mTrackView.setDuration(us)
    }

    fun addTrack(trackModel: MediaTrackModel) {
        mTrackView.addTrack(trackModel)
    }

    fun updateTrack(trackModel: MediaTrackModel) {
        mTrackView.updateTrack(trackModel)
    }

    fun deleteTrackOrSeg(trackId: Int, segId: Int) {
        mTrackView.deleteTrackOrSeg(trackId, segId)
    }

    fun setProgress(progress: Float) {
        if (progress < 0 || progress > 1) {
            return
        }
        this.progress = progress
        scrollTo(((mTrackView.measuredWidth - measuredWidth) * this.progress).toInt(), 0)
    }

    fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener) {
        mOnSeekBarChangeListener = l
    }

    private val mScaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var previousScaleFactor = 1f
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            previousScaleFactor = 1f
            return super.onScaleBegin(detector)
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            previousScaleFactor = 1f
            super.onScaleEnd(detector)
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val anchor = PointF(
                detector.focusX * 2 / measuredWidth.toFloat() - 1f,
                -(detector.focusY * 2 / measuredHeight.toFloat() - 1f)
            )
            scale = scale * detector.scaleFactor / previousScaleFactor
            previousScaleFactor = detector.scaleFactor
            if (scale < 0.5f) {
                scale = 0.5f
            }
            if (scale > 3) {
                scale = 3f
            }
            getChildView().setScale(ScaleRational((scale * 10000).toInt(), 10000))
            return super.onScale(detector)
        }
    }

    private val onGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent?) {
            super.onLongPress(e)
        }
    }

    interface OnSeekBarChangeListener {
        fun onProgressChanged(
            progress: Float,
            fromUser: Boolean
        )

        fun onStartTrackingTouch()

        fun onStopTrackingTouch()
    }
}