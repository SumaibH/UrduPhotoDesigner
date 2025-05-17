package com.example.urduphotodesigner.data.model.editor

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

data class TextBoxItem(
    var text: String,
    var x: Float,
    var y: Float,
    var fontSize: Float = 32f,
    var color: Int = Color.BLACK,
    var rotation: Float = 0f,
    var opacity: Int = 255,
    var alignment: Paint.Align = Paint.Align.LEFT,
    var typeface: Typeface = Typeface.DEFAULT,
    var isSelected: Boolean = false
)
