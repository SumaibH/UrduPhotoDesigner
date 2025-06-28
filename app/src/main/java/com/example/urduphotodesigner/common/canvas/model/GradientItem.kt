package com.example.urduphotodesigner.common.canvas.model

import android.graphics.Color
import com.example.urduphotodesigner.common.canvas.enums.GradientType

data class GradientItem(
    var colors: List<Int> = listOf(Color.BLACK, Color.GRAY),
    var positions: List<Float> = listOf(0f, 1f),
    val angle: Float = 0f,
    val scale: Float = 1f,
    val type: GradientType = GradientType.LINEAR,
    val radialRadiusFactor: Float = 0.5f,
    var isSelected: Boolean = false
) {
    init {
        require(colors.size == positions.size) {
            "colors and positions must have the same length"
        }
        require(positions.all { it in 0f..1f }) {
            "positions must be within 0f…1f"
        }
        require(radialRadiusFactor in 0f..1f) {
            "radialRadiusFactor must be within 0f…1f"
        }
    }
}