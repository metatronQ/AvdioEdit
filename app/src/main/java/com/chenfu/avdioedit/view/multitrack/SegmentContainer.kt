package com.chenfu.avdioedit.view.multitrack

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.*
import android.util.AttributeSet
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.core.view.size
import com.chenfu.avdioedit.util.DisplayUtils
import com.chenfu.avdioedit.model.data.MediaTrackModel
import com.chenfu.avdioedit.model.data.MediaType
import com.chenfu.avdioedit.util.IdUtils
import com.chenfu.avdioedit.viewmodel.MultiTrackViewModel
import com.example.ndk_source.util.LogUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.round

class SegmentContainer : ViewGroup, BaseView {
    // 当前轨道
    private lateinit var mMediaTrackModel: MediaTrackModel
    private val tvMap = TreeMap<Int, TextView>()
    private val mVideoColor = Color.DKGRAY
    private val mAudioColor = Color.BLACK
    private var multiViewModel: MultiTrackViewModel ?= null
    private var startPoint = Point()
    private var point = Point()
    private var startDragX = 0f
    private var startDragY = 0f
    private var initialObj: Any?= null

    constructor(context: Context, mediaTrackModel: MediaTrackModel): super(context) {
        this.mMediaTrackModel = mediaTrackModel
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
        background = GradientDrawable().run {
            shape = GradientDrawable.RECTANGLE
            setStroke(DisplayUtils.dip2px(context, 1f), Color.BLACK)
            this
        }
        resetSelected()
        mMediaTrackModel.childMedias.forEach {
            tvMap[it.key] = generateTv(it.value)
            addView(tvMap[it.key])
        }

        // 一旦有一个拖曳，那所有setOnDragListener都可能会被回调
        // 在DragEvent.ACTION_DROP 时才能获取到 ClipData 的数据
        // DragShadowBuilder(tv)即是根据tv复制的一个拖曳view，默认拖曳点在正中心，可继承后自定义拖曳点
        // container设置为拖放区，即v只可能为SegmentContainer
        setOnDragListener { v, event ->
            when (event!!.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    LogUtil.packageName(context).d("DragEvent.ACTION_DRAG_STARTED")
                    LogUtil.packageName(context).d("DragEvent_Started:" + event.x.toString())
                    val tv = (event.localState as DragLocalData).textView
                    tv.visibility = INVISIBLE

                    // 起始xy
                    startDragX = event.x
                    startDragY = event.y

                    // 记录最初的container
                    initialObj = v
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    LogUtil.packageName(context).d("DragEvent.ACTION_DRAG_ENTERED")
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    // ACTION_DRAG_ENTERED后调用，不断回调，返回当前手指touch点距离当前容器边界的距离
                    // TODO: 当减至0/加至容器边界时则会触发ACTION_DRAG_EXITED，此时当前容器的Location会结束，
                    //  若手指进入到新容器边界内，则新容器的ACTION_DRAG_ENTERED被调用，之后x、y便是距离新容器的距离
                    LogUtil.packageName(context).d("DragEvent_Location: x:" + event.x.toString() + " y:" + event.y.toString())
                    point.x = event.x.toInt()
                    point.y = event.y.toInt()
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    LogUtil.packageName(context).d("DragEvent.ACTION_DRAG_EXITED")
                }
                // 系统允许用户将拖动阴影释放到拖动事件监听器不接收拖动事件的 View 上，而且允许用户将拖动阴影释放到应用界面的空白区域或应用以外的区域。
                // 在所有这些情况下，系统均不会发送操作类型为 ACTION_DRAG_ENDED 的事件，不过它会发送 ACTION_DROP 事件
                // 此时的x、y有可能是新容器的距离
                // 这个步骤只会在用户放下拖放阴影的View对象（这个对象被注册用于接受这个拖拽事件）中发生，
                // 如果用户在其他的任何不接收这个拖拽事件的地方释放了拖拽阴影，就不会有ACTION_DROP拖拽事件发出，并且会在end返回false
                // TODO event.xy为距离容器的距离，而startpoint是触点距离tv边界的距离，只能作为一开始拖曳触点使用
                DragEvent.ACTION_DROP -> {
                    LogUtil.packageName(context).d("DragEvent.ACTION_DROP")
                    LogUtil.packageName(context).d("DragEvent_Drop:" + event.x.toString())

                    val dragLocalData = event.localState as DragLocalData
                    // containerTrackModel才是原来的track，而nowTrackModel根据不同放置区而不同
                    // TODO：这里传过来的是引用，因此直接修改会造成对应的segment的属性，因此这里需要clone
                    val containerTrackModel = dragLocalData.containerTrackModel.clone()
                    val childTrackModel = dragLocalData.childTrackModel.clone()
                    val nowTrackModel = mMediaTrackModel.clone()

                    val dropDuration = round(event.x * 1f * nowTrackModel.duration / v.width).toLong()
                    val offsetXPer = (event.x - startDragX) * 1f / v.width
                    val offsetYPer = (event.y - startDragY) * 1f / v.width
                    val offsetX = round(nowTrackModel.duration * offsetXPer).toLong()

                    if (childTrackModel.seqIn + offsetX < 0) {
                        childTrackModel.seqIn = 0
                        childTrackModel.seqOut = childTrackModel.duration
                    } else {
                        childTrackModel.seqIn += offsetX
                        childTrackModel.seqOut += offsetX
                    }

                    mMediaTrackModel.childMedias.forEach {
                        // 若放置seg遮盖了其他seg，则直接清除所有被遮盖的seg
                        if (it.key != childTrackModel.id &&
                            it.value.seqIn >= childTrackModel.seqIn && it.value.seqOut <= childTrackModel.seqOut) {
                            nowTrackModel.childMedias.remove(it.key)
                        }
                    }

                    nowTrackModel.duration = if (childTrackModel.seqOut > nowTrackModel.seqOut) {
                        childTrackModel.seqOut
                    } else {
                        nowTrackModel.duration
                    }
                    nowTrackModel.seqOut = nowTrackModel.duration

                    if (containerTrackModel.id != nowTrackModel.id) {
                        // 放置在新的container，原来的container也要更新
                        containerTrackModel.childMedias.remove(childTrackModel.id)
                        multiViewModel?.updateTrack?.value = containerTrackModel

                        childTrackModel.id = IdUtils.getNewestSegmentId()
                    }

                    nowTrackModel.childMedias[childTrackModel.id] = childTrackModel
                    if (nowTrackModel.childMedias.size > 1) {
                        Map2SortListThenUpdateMap(nowTrackModel)
                    }

                    multiViewModel?.updateTrack?.value = nowTrackModel
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    LogUtil.packageName(context).d("DragEvent.ACTION_DRAG_ENDED")
                    (event.localState as DragLocalData).textView.visibility = VISIBLE
                }
            }
            true
        }
    }

    /**
     * 对segment根据seqIn从小到大排序
     * 然后以冒泡的方式更新重叠的seg
     */
    private fun Map2SortListThenUpdateMap(nowTrackModel: MediaTrackModel) {
        val array = ArrayList(nowTrackModel.childMedias.values)
        val sortedArray = array.sortedBy {
            it.seqIn
        }
        for (i in 0..sortedArray.size - 2) {
            compareNowAndNextTrack(sortedArray[i], sortedArray[i+1])
            nowTrackModel.childMedias[sortedArray[i].id] = sortedArray[i]
        }
        // 更新末尾
        nowTrackModel.childMedias[sortedArray[sortedArray.size - 1].id] = sortedArray[sortedArray.size - 1]
        nowTrackModel.duration = sortedArray[sortedArray.size - 1].seqOut
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

    private fun keepNotOverlay(
        reference: Long, childTrackModel: MediaTrackModel,
        seg0Center: Long, seg0: MediaTrackModel,
        nowTrackModel: MediaTrackModel
    ) {
        val seg0OldOut = seg0.seqOut
        if (reference >= seg0Center) {
            childTrackModel.seqIn = seg0.seqOut
            childTrackModel.seqOut = childTrackModel.seqIn + childTrackModel.duration
        } else {
            childTrackModel.seqIn = seg0.seqIn
            childTrackModel.seqOut = childTrackModel.seqIn + childTrackModel.duration
            seg0.seqIn = childTrackModel.seqOut
            seg0.seqOut = seg0.seqIn + seg0.duration
        }
        // 更新timeDuration
        var furthest = nowTrackModel.duration
        nowTrackModel.childMedias.forEach {
            if (it.key != childTrackModel.id && it.key != seg0.id
                && it.value.seqIn >= seg0OldOut) {
                it.value.seqIn += childTrackModel.duration
                it.value.seqOut += childTrackModel.duration
            }
            furthest = if (furthest < it.value.seqOut) {
                it.value.seqOut
            } else {
                furthest
            }
        }
        nowTrackModel.duration = furthest
    }

    override fun setViewModel(multiTrackViewModel: MultiTrackViewModel) {
        this.multiViewModel = multiTrackViewModel
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun generateTv(childTrackModel: MediaTrackModel) : TextView {
        val tv = TextView(context)
        tv.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // 获取触点x、y，不消费事件
                startPoint.x = event!!.x.toInt()
                startPoint.y = event.y.toInt()
            }
            false
        }
        tv.setOnLongClickListener {
            // 未选中时不能拖曳
            if (!tv.isSelected) return@setOnLongClickListener true
//                val intent = Intent()
//                intent.putExtra("childTrack", childTrackModel)
//
//                val item = ClipData.Item(intent)
//                val clipData = ClipData("child", arrayOf(ClipDescription.MIMETYPE_TEXT_INTENT), item)
            // 序列化有问题，因此取消使用clipData
            tv.startDrag(null, MyDragShadowBuilder(tv, startPoint), DragLocalData(tv, childTrackModel, mMediaTrackModel), 0)
            true
        }
        // 单击选中
        tv.setOnClickListener {
            multiViewModel?.let {
                if (tv.isSelected) {
                    tv.isSelected = false
                    it.cropModel.segmentId = -1
                    it.cropModel.containerId = -1
                } else {
                    it.updateSelectedStatusListener.update(it.cropModel.containerId, it.cropModel.segmentId)
                    tv.isSelected = true
                    it.cropModel.segmentId = childTrackModel.id
                    it.cropModel.containerId = mMediaTrackModel.id
                }
            }
        }
        tv.textSize = 14f
        tv.setTextColor(Color.WHITE)
        tv.text = when (childTrackModel.type) {
            MediaType.TYPE_VIDEO -> "Video ${childTrackModel.id}"
            MediaType.TYPE_AUDIO -> "Audio ${childTrackModel.id}"
            else -> "Unknown"
        }
        tv.background = generateBg(childTrackModel)
        return tv
    }

    private fun generateBg(childTrackModel: MediaTrackModel) : Drawable {
        val drawableSelected = GradientDrawable()
        drawableSelected.run {
            setColor(when (childTrackModel.type) {
                MediaType.TYPE_VIDEO -> mVideoColor
                MediaType.TYPE_AUDIO -> mAudioColor
                else -> Color.RED
            })
            shape = GradientDrawable.RECTANGLE
            setStroke(DisplayUtils.dip2px(context, 2f), Color.YELLOW)
        }
        val drawableDefault = GradientDrawable()
        drawableDefault.run {
            setColor(when (childTrackModel.type) {
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

    fun updateAllSegment(trackModel: MediaTrackModel) {
        this.mMediaTrackModel = trackModel
        resetSelected()
        mMediaTrackModel.childMedias.forEach {
            tvMap[it.key] = generateTv(it.value)
            addView(tvMap[it.key])
        }
        requestLayout()
//        updateAudioTrack(track)
    }

    fun clearLastSelectedStatus(segmentId: Int) {
        tvMap[segmentId]?.isSelected = false
    }

    private fun resetSelected() {
        tvMap.clear()
        multiViewModel?.run {
            cropModel.segmentId = -1
            cropModel.containerId = -1
        }
        removeAllViews()
    }

    fun updateChildView(childTrackModel: MediaTrackModel) {

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
                    tvMap[trackModel.id]?.background =
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
            val childTrack = mMediaTrackModel.childMedias[it.key]!!
            val childTv = it.value
            var w = measuredWidth - paddingLeft - paddingRight
            val h = measuredHeight
            var offsetLeft = 0
            var offsetRight = 0
            if (mMediaTrackModel.duration > 0) {
                offsetLeft = (childTrack.seqIn * w / mMediaTrackModel.duration).toInt()
                offsetRight = (childTrack.seqOut * w / mMediaTrackModel.duration).toInt()
                w = (childTrack.duration * w / mMediaTrackModel.duration).toInt()
            }
            childTv.layout(paddingLeft + offsetLeft, 0, paddingLeft + offsetRight, h)
            childTv.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), h)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
    }

    private class MyDragShadowBuilder(v: View, var point: Point) : View.DragShadowBuilder(v) {

        private val shadow = ColorDrawable(Color.LTGRAY)

        // Defines a callback that sends the drag shadow dimensions and touch point
        // back to the system.
        override fun onProvideShadowMetrics(size: Point, touch: Point) {
//            val width: Int = view.width / 2
//            val height: Int = view.height / 2
//            shadow.setBounds(0, 0, width, height)
            size.set(view.width, view.height)

            // Set the touch point's position to be in the middle of the drag shadow.
            // 此处设置触摸点
            touch.set(point.x, point.y)
        }

        // Defines a callback that draws the drag shadow in a Canvas that the system
        // constructs from the dimensions passed to onProvideShadowMetrics().
        override fun onDrawShadow(canvas: Canvas) {
//            shadow.draw(canvas)
            super.onDrawShadow(canvas)
        }
    }

    data class DragLocalData(val textView: TextView, val childTrackModel: MediaTrackModel, val containerTrackModel: MediaTrackModel)

    interface UpdateSelectedStatusListener {
        fun update(lastContainerId: Int, lastSegId: Int)
    }
}