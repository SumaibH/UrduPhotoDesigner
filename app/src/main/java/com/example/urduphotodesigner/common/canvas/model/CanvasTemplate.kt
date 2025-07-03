package com.example.urduphotodesigner.common.canvas.model

import java.io.Serializable

data class CanvasTemplate(
    val canvasElements: List<CanvasElement>,
    val canvasSize: CanvasSize,
    val backgroundColor: Int,
    val backgroundImage: String?, // Store bitmap as Base64 string
    val backgroundGradient: GradientItem,
) : Serializable