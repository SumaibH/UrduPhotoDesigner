package com.example.urduphotodesigner.common.canvas.model

import java.io.Serializable

data class CanvasSize(
    val name: String,
    val drawableResId: Int,
    val width: Float,
    val height: Float
): Serializable