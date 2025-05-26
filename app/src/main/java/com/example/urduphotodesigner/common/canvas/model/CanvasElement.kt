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
    var paintAlpha: Int = 255
) : Serializable {

    @Transient
    lateinit var paint: TextPaint // Changed to lateinit var

    init {
        // Initialize paint with default values.
        // We will set typeface separately if context is available.
        paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = paintColor
            textSize = paintTextSize
            textAlign = paintTextAlign
            alpha = paintAlpha
            // Initial typeface is set here, but might be overridden if fontId is present later
            context?.let {
                typeface = ResourcesCompat.getFont(it, R.font.regular) // Default font
            } ?: run {
                typeface = Typeface.DEFAULT // Fallback if context is null
            }
        }
    }

    // Removed the secondary constructor as it was redundant and causing conflicting overloads.
    // The primary constructor with default values and the init block handle all initialization.

    // Helper function to update transient paint properties from serializable ones
    // This is still useful to ensure the transient paint object reflects the latest serializable properties
    fun updatePaintProperties() {
        if (!::paint.isInitialized) {
            paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        }
        paint.color = paintColor
        paint.textSize = paintTextSize
        paint.textAlign = paintTextAlign
        paint.alpha = paintAlpha

        // Re-apply font if fontId is set and context is available
        if (type == ElementType.TEXT && fontId != null && context != null) {
            // This assumes you have a way to get FontEntity from fontId here.
            // For now, we'll leave this part to be handled by ViewModel,
            // or you'd need a font lookup mechanism here.
            // However, this function is called when deserializing or re-applying context.
            // A safer approach is to pass the actual font file path or typeface directly if possible.
            // For now, let's keep the font application in ViewModel for single source of truth.
            // The main purpose of updatePaintProperties is for the other paint properties.
            // The typeface will be explicitly set by ViewModel.
        } else if (context != null) {
            // Ensure a default font is set if no custom font is selected
            paint.typeface = ResourcesCompat.getFont(context!!, R.font.regular)
        } else {
            paint.typeface = Typeface.DEFAULT
        }
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