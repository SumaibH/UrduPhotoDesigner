package com.example.urduphotodesigner.common.canvas.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Typeface
import android.text.TextPaint
import com.example.urduphotodesigner.common.canvas.enums.BlendType
import com.example.urduphotodesigner.common.canvas.enums.ElementType
import com.example.urduphotodesigner.common.canvas.enums.LabelShape
import com.example.urduphotodesigner.common.canvas.enums.LetterCasing
import com.example.urduphotodesigner.common.canvas.enums.ListStyle
import com.example.urduphotodesigner.common.canvas.enums.TextAlignment
import com.example.urduphotodesigner.common.canvas.enums.TextDecoration
import com.example.urduphotodesigner.common.canvas.sealed.ImageFilter
import java.io.Serializable
import java.util.UUID

private const val ICON_PADDING =
    20f // Or whatever value suits your icon sizes and visual preference

// Make CanvasElement Serializable to allow it to be passed via Bundles and saved.
data class CanvasElement(
    // Context is transient and should not be serialized. It will be re-provided on load.
    @Transient var context: Context? = null, // Made nullable for deserialization
    var type: ElementType,
    var text: String = "",
    // Bitmap is also transient. It needs to be handled separately for serialization (e.g., to Base64 or URI).
    @Transient var bitmap: Bitmap? = null,
    // Add bitmapData to store the Base64 encoded string of the bitmap for serialization
    var bitmapData: String? = null,
    var imageFilter: ImageFilter? = null,
    var x: Float = 0f,
    var y: Float = 0f,
    var scale: Float = 1f,
    var rotation: Float = 0f,
    val id: String = UUID.randomUUID().toString(),
    var isLocked: Boolean = false,
    var zIndex: Int = 0,
    var isSelected: Boolean = false,
    var fontId: String? = null, // Store font ID for serialization
    // Properties of TextPaint for serialization
    var paintColor: Int = Color.BLACK,
    var paintTextSize: Float = 80f,
    var paintAlpha: Int = 255,
    // Border
    var hasStroke: Boolean = false,
    var strokeColor: Int = Color.BLACK,
    var strokeWidth: Float = 1f,

    // Shadow
    var hasShadow: Boolean = false,
    var shadowColor: Int = Color.GRAY,
    var shadowDx: Float = 1f,
    var shadowDy: Float = 1f,
    var shadowRadius: Float = 8f,
    var shadowOpacity: Int = 64,

    // Label
    var hasLabel: Boolean = false,
    var labelColor: Int = Color.YELLOW,
    var labelShape: LabelShape = LabelShape.RECTANGLE_FILL,

    var lineSpacing: Float = 1.0f,
    var letterSpacing: Float = 0f,
    var letterCasing: LetterCasing = LetterCasing.NONE,
    var textDecoration: Set<TextDecoration> = emptySet(),
    var alignment: TextAlignment = TextAlignment.CENTER,
    var currentIndent: Float = 0f,
    var listStyle: ListStyle = ListStyle.NONE,

    // text fill gradient
    var fillGradientColors: IntArray? = null,
    var fillGradientPositions: FloatArray? = null,

    // text stroke gradient
    var strokeGradientColors: IntArray? = null,
    var strokeGradientPositions: FloatArray? = null,

    @Transient
    var originalTypeface: Typeface? = null,
    var hasBlur: Boolean = false,
    var blurValue: Float = 10f,  // Blur radius value
    var blendType: BlendType = BlendType.SRC_OVER
) : Serializable {

    @Transient
    lateinit var paint: TextPaint

    init {
        paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        updatePaintProperties()
    }

    fun updatePaintProperties() {
        if (!::paint.isInitialized) paint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        // Basic properties
        paint.color = paintColor
        paint.textSize = paintTextSize
        paint.alpha = paintAlpha
    }

    fun getLocalContentWidth(): Float {
        return if (type == ElementType.TEXT) {
            val lines = text.split("\n")
            // Ensure paint is initialized before using it
            if (::paint.isInitialized) {
                lines.maxOfOrNull { line -> paint.measureText(line) } ?: 0f
            } else {
                0f
            }
        } else {
            bitmap?.width?.toFloat() ?: 0f
        }
    }

    fun getLocalContentHeight(): Float {
        return if (type == ElementType.TEXT) {
            // Ensure paint is initialized before using it
            if (::paint.isInitialized) {
                val fm = paint.fontMetrics
                val lineHeight = (fm.bottom - fm.top) * lineSpacing
                val lines = text.split("\n")
                lines.size * lineHeight
            } else {
                0f
            }
        } else {
            bitmap?.height?.toFloat() ?: 0f
        }
    }

    // This function MUST return icon positions in the ELEMENT'S LOCAL, UNSCALED coordinate system
    // where (0,0) is the element's pivot point (center in your case).
    fun getIconPositions(): Map<String, PointF> {
        // Use the local, unscaled content dimensions
        val localW = getLocalContentWidth()
        val localH = getLocalContentHeight()

        // Calculate icon positions relative to the local center (0,0) of the element
        // These are coordinates in the element's own unscaled space.
        // The positions define the center of where the icon should be.
        // For example, top-right corner of the local content box:
        val iconOffsetX =
            localW / 2f + ICON_PADDING // Position icons slightly outside the content box
        val iconOffsetY = localH / 2f + ICON_PADDING

        return mapOf(
            // Example: Delete icon at the top-right of the *local content*
            "delete" to PointF(iconOffsetX, -iconOffsetY),
            // Example: Rotate icon at the bottom-right
            "rotate" to PointF(iconOffsetX, iconOffsetY),
            // Example: Resize icon at the bottom-left
            "resize" to PointF(-iconOffsetX, iconOffsetY),
            "edit" to PointF(-iconOffsetX, -iconOffsetY)
            // Adjust signs and axes as per your desired icon layout (e.g. top-left, bottom-left etc)
        )
    }
}