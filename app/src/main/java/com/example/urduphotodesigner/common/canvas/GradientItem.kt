package com.example.urduphotodesigner.common.canvas

import android.graphics.drawable.GradientDrawable

data class GradientItem(
    val colors: List<Int>,
    val orientation: GradientDrawable.Orientation,
    var isSelected: Boolean = false
)
