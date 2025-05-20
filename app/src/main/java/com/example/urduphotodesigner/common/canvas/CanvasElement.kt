package com.example.urduphotodesigner.common.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.urduphotodesigner.R
import java.util.UUID

private const val ICON_PADDING = 20f // Or whatever value suits your icon sizes and visual preference
private const val BOUNDS_PADDING = 20f // Padding you used for getBounds

data class CanvasElement(
    val context: Context,
    var type: ElementType,
    var text: String = "",
    var bitmap: Bitmap? = null,
    var x: Float = 0f,
    var y: Float = 0f,
    var scale: Float = 1f,
    var rotation: Float = 0f,
    var paint: TextPaint = TextPaint().apply {
        color = Color.BLACK
        textSize = 80f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.regular)
    },
    var lineSpacingMultiplier: Float = 1.0f,
    val id: String = UUID.randomUUID().toString(),
    var isLocked: Boolean = false,
    var zIndex: Int = 0,
    var isSelected: Boolean = false
) {

    constructor(
        context: Context,
        type: ElementType,
        text: String = "",
        bitmap: Bitmap? = null,
        x: Float = 0f,
        y: Float = 0f,
        scale: Float = 1f,
        rotation: Float = 0f,
        paint: TextPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 80f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = ResourcesCompat.getFont(context, R.font.regular)
        },
        lineSpacingMultiplier: Float = 1.0f,
        id: String = UUID.randomUUID().toString(),
        isLocked: Boolean = false,
        zIndex: Int = 0,
        isSelected: Boolean = false,
        applyProperties: Boolean = true
    ) : this(
        context,
        type,
        text,
        bitmap,
        x,
        y,
        scale,
        rotation,
        paint,
        lineSpacingMultiplier,
        id,
        isLocked,
        zIndex,
        isSelected
    ) {
        if (applyProperties) {
            // No further action needed, the default values are already applied
        }
    }

    fun getLocalContentWidth(): Float {
        return if (type == ElementType.TEXT) {
            val lines = text.split("\n")
            lines.maxOfOrNull { line -> paint.measureText(line) } ?: 0f
        } else {
            bitmap?.width?.toFloat() ?: 0f
        }
    }

    fun getLocalContentHeight(): Float {
        return if (type == ElementType.TEXT) {
            val fm = paint.fontMetrics
            val lineHeight = (fm.bottom - fm.top) * lineSpacingMultiplier
            val lines = text.split("\n")
            lines.size * lineHeight
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