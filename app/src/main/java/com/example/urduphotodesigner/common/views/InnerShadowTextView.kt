package com.example.urduphotodesigner.common.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.urduphotodesigner.R

class EngravedInnerShadowTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var displayText: String = "Engraved Shadow"
        set(value) {
            field = value
            invalidate()
        }

    private val textSizePx = 100f
    private val blurRadius = 2.5f // Adjusted for a very crisp, fine engraved line
    private val darkShadowOffsetX = 2f // Small offset for depth
    private val darkShadowOffsetY = 2f
    private val lightHighlightOffsetX = -1.5f // Opposite direction for highlight
    private val lightHighlightOffsetY = -1.5f

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        // Base paint for text shape and final text fill.
        // Its color will be overwritten for specific drawing steps.
        color = Color.BLACK
        style = Paint.Style.FILL
        typeface = Typeface.create(ResourcesCompat.getFont(context, R.font.poppins), Typeface.BOLD)
        textSize = textSizePx
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width
        val height = height

        if (width <= 0 || height <= 0) return

        // 1. Draw the background color of the view/canvas. This is the "material" for engraving.
        val backgroundColor = Color.parseColor("#FFFFFF") // Light gray, similar to Image 1
        canvas.drawColor(backgroundColor)

        // Calculate text position (centered)
        val textBounds = Rect()
        textPaint.getTextBounds(displayText, 0, displayText.length, textBounds)
        val x = (width - textBounds.width()) / 2f - textBounds.left // Center horizontally
        val y = height / 2f + (textBounds.height() / 2f) - textBounds.bottom // Center vertically

        // Save a layer to apply PorterDuffXfermode for the inner shadow effect.
        // Drawing everything within this layer, then compositing it back.
        val layerId = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // --- Step 2 & 3: Draw the blurred shadows and highlights within the layer ---

        // Paint for the dark inner shadow
        val darkShadowPaint = Paint(textPaint).apply {
            color = Color.argb(255, 50, 50, 50) // Opaque dark gray for the shadow
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            xfermode = null // Ensure no xfermode initially
        }
        // Draw the dark shadow, offset down and right
        canvas.drawText(displayText, x + darkShadowOffsetX, y + darkShadowOffsetY, darkShadowPaint)

        // Paint for the light inner highlight
        val lightHighlightPaint = Paint(textPaint).apply {
            color = Color.argb(255, 255, 255, 255) // Opaque white for the highlight
            maskFilter = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            xfermode = null // Ensure no xfermode initially
        }
        // Draw the light highlight, offset up and left
        canvas.drawText(displayText, x + lightHighlightOffsetX, y + lightHighlightOffsetY, lightHighlightPaint)

        // --- Step 4: Use the text shape as a mask to clip shadows/highlights inside ---

        // Paint for masking, using DST_IN
        val maskPaint = Paint(textPaint).apply {
            color = Color.BLACK // Color for the mask itself (any opaque color works)
            maskFilter = null // No blur for the mask
            // DST_IN: Keep destination pixels only where source pixels are present.
            // Source = current text being drawn (the opaque text shape).
            // Destination = what's already on the layer (the blurred shadows/highlights).
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        }
        // Draw the crisp text shape. This will clip the previously drawn shadows/highlights.
        canvas.drawText(displayText, x, y, maskPaint)

        // --- Step 5: Draw the main text fill color (the "cutout" effect) ---

        // Paint for the final text fill, making it appear "engraved" into the background
        val engravedFillPaint = Paint(textPaint).apply {
            color = backgroundColor // Make text color match background for cutout
            maskFilter = null // No blur
            // SRC_ATOP: Source is drawn over destination, but only where destination pixels exist.
            // This is useful for overlaying the text color without affecting the inner shadows' alpha.
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }
        // Draw the final text fill.
        canvas.drawText(displayText, x, y, engravedFillPaint)

        // Restore the canvas to apply the entire layer onto the main canvas.
        canvas.restoreToCount(layerId)
    }
}