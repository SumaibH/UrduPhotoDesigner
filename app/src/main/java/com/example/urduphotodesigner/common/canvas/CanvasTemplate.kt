package com.example.urduphotodesigner.common.canvas

data class CanvasTemplate(
    val canvasElements: List<CanvasElement>,
    val canvasSize: CanvasSize,
    val backgroundColor: Int,
    val backgroundImage: String?, // Store bitmap as Base64 string
    val backgroundGradientColors: IntArray?,
    val backgroundGradientPositions: FloatArray?
) : java.io.Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CanvasTemplate

        if (canvasElements != other.canvasElements) return false
        if (canvasSize != other.canvasSize) return false
        if (backgroundColor != other.backgroundColor) return false
        if (backgroundImage != other.backgroundImage) return false
        if (backgroundGradientColors != null) {
            if (other.backgroundGradientColors == null) return false
            if (!backgroundGradientColors.contentEquals(other.backgroundGradientColors)) return false
        } else if (other.backgroundGradientColors != null) return false
        if (backgroundGradientPositions != null) {
            if (other.backgroundGradientPositions == null) return false
            if (!backgroundGradientPositions.contentEquals(other.backgroundGradientPositions)) return false
        } else if (other.backgroundGradientPositions != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = canvasElements.hashCode()
        result = 31 * result + canvasSize.hashCode()
        result = 31 * result + (backgroundImage?.hashCode() ?: 0)
        result = 31 * result + (backgroundGradientColors?.contentHashCode() ?: 0)
        result = 31 * result + (backgroundGradientPositions?.contentHashCode() ?: 0)
        return result
    }
}