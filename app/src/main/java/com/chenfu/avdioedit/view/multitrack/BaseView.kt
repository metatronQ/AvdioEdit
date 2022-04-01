package com.chenfu.avdioedit.view.multitrack

import android.content.Context
import android.util.AttributeSet
import com.chenfu.avdioedit.viewmodel.MultiTrackViewModel

interface BaseView {

    fun onResolveAttribute(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)

    fun onInitialize(context: Context)

    fun setViewModel(multiTrackViewModel: MultiTrackViewModel)
}