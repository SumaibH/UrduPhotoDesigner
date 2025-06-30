package com.example.urduphotodesigner.common.canvas.model

import android.graphics.Color
import com.example.urduphotodesigner.common.canvas.enums.GradientOrientation
import com.example.urduphotodesigner.common.canvas.enums.GradientType

data class GradientItem(
    var colors: List<Int> = listOf(Color.BLACK, Color.GRAY),
    var positions: List<Float> = listOf(0f, 1f),
    val angle: Float = 0f,
    val scale: Float = 1f,
    val type: GradientType = GradientType.LINEAR,
    val radialRadiusFactor: Float = 0.5f,
    val sweepStartAngle: Float = 0f,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val orientation: GradientOrientation = GradientOrientation.HORIZONTAL,
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
        require(centerX in 0f..1f && centerY in 0f..1f) {
            "centerX/centerY must be within 0f…1f"
        }
    }

    /** Return a new item with stops reversed (colors & positions inverted). */
    fun swapped(): GradientItem {
        val invPos = positions.map { 1f - it }.reversed()
        return copy(
            colors    = colors.reversed(),
            positions = invPos
        )
    }

    /** Helper to copy with a new orientation (linear only). */
    fun withOrientation(o: GradientOrientation): GradientItem {
        val newAngle = when(o) {
            GradientOrientation.HORIZONTAL -> 0f
            GradientOrientation.VERTICAL   -> 90f
        }
        return copy(orientation = o, angle = newAngle)
    }

    /** Helper for sweep start-angle. */
    fun withSweepStart(angle: Float): GradientItem =
        copy(sweepStartAngle = angle)

    /** Helper for radial center. */
    fun withRadialCenter(x: Float, y: Float): GradientItem =
        copy(centerX = x.coerceIn(0f,1f), centerY = y.coerceIn(0f,1f))
}
