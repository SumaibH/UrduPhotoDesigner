package com.example.urduphotodesigner.common.canvas.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.enums.ElementType
import com.example.urduphotodesigner.common.canvas.enums.LabelShape
import com.example.urduphotodesigner.common.canvas.sealed.ImageFilter
import java.io.Serializable
import java.util.UUID

private const val ICON_PADDING = 20f // Or whatever value suits your icon sizes and visual preference
private const val BOUNDS_PADDING = 20f // Padding you used for getBounds

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
    var lineSpacingMultiplier: Float = 1.0f,
    val id: String = UUID.randomUUID().toString(),
    var isLocked: Boolean = false,
    var zIndex: Int = 0,
    var isSelected: Boolean = false,
    var fontId: String? = null, // Store font ID for serialization
    // Properties of TextPaint for serialization
    var paintColor: Int = Color.BLACK,
    var paintTextSize: Float = 80f,
    var paintTextAlign: Paint.Align = Paint.Align.CENTER,
    var paintAlpha: Int = 255,
    // Border
    var hasBorder: Boolean = false,
    var borderColor: Int = Color.BLACK,
    var borderWidth: Float = 1f,

    // Shadow
    var hasShadow: Boolean = false,
    var shadowColor: Int = Color.GRAY,
    var shadowDx: Float = 1f,
    var shadowDy: Float = 1f,

    // Label
    var hasLabel: Boolean = false,
    var labelColor: Int = Color.YELLOW,
    var labelShape: LabelShape = LabelShape.RECTANGLE_FILL
) : Serializable {

    @Transient
    lateinit var paint: TextPaint

    init {
        paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        updatePaintProperties()
    }

    fun updatePaintProperties() {
        if (!::paint.isInitialized) paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.color = paintColor
        paint.textSize = paintTextSize
        paint.textAlign = paintTextAlign
        paint.alpha = paintAlpha

        if (hasShadow) {
            paint.setShadowLayer(8f, shadowDx, shadowDy, shadowColor)
        } else {
            paint.clearShadowLayer()
        }

        if (hasBorder) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = borderWidth
            paint.color = borderColor
        } else {
            paint.style = Paint.Style.FILL
        }

        paint.typeface = context?.let { ResourcesCompat.getFont(it, R.font.regular) } ?: Typeface.DEFAULT
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
                val lineHeight = (fm.bottom - fm.top) * lineSpacingMultiplier
                val lines = text.split("\n")
                lines.size * lineHeight
            } else {
                0f
            }
        } else {
            bitmap?.height?.toFloat() ?: 0f
        }
    }

    // This function returns bounds in the CANVAS coordinate system, scaled and positioned.
    // It's used for general element selection/deselection based on touch.
    // Note: The padding here (BOUNDS_PADDING) makes the tappable area for selection larger.
    fun getBounds(): RectF {
        val localContentWidth = getLocalContentWidth()
        val localContentHeight = getLocalContentHeight()

        // Scaled dimensions of the content itself
        val scaledContentWidth = localContentWidth * scale
        val scaledContentHeight = localContentHeight * scale

        // Add padding to the scaled content dimensions to get the final tappable/bounding box size
        val finalWidth = scaledContentWidth + BOUNDS_PADDING
        val finalHeight = scaledContentHeight + BOUNDS_PADDING

        return RectF(
            x - finalWidth / 2,
            y - finalHeight / 2,
            x + finalWidth / 2,
            y + finalHeight / 2
        )
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