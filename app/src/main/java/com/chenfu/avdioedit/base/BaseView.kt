package com.chenfu.avdioedit.base

import android.content.Context
import android.util.AttributeSet
import android.view.View

interface BaseView {

    fun onResolveAttribute(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int)

    fun onInitialize(context: Context)
}