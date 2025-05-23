package com.example.urduphotodesigner.common.canvas.model

data class ExportResolution(
    val name: String,
    val width: Int,
    val height: Int,
    val scaleFactor: Float = 1f
)