package com.chenfu.avdioedit.view.multitrack.widget

class ScaleRational(var num: Int = 0, ///< Numerator
                    var den: Int = 1 ///< Denominator
) {
    companion object {
        fun zero(): ScaleRational = ScaleRational(0, 1)
    }
}