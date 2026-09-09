package com.webscare.urducanvas.common.views

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.webscare.urducanvas.R

/**
 * A one-sided drop shadow that follows the rounded corners of the surface next
 * to it.
 *
 * The editor chrome cannot use elevation: the header would throw a shadow
 * upward over the status bar and the panel would throw one down over the bottom
 * bar. The replacement was a flat gradient strip, which lifts the surface but
 * runs straight across the full width — so a header with 15dp bottom corners,
 * or a panel with 30dp top corners, sat on a shadow with square ones and the
 * rounding read as a rendering glitch.
 *
 * This draws the surface's own silhouette instead, blurred, positioned so its
 * body falls outside the strip and only the blur that spills past the edge
 * survives the view's own clip. The corners therefore curve exactly as the
 * surface does, with no shadow on any other side.
 *
 * **The strip must overlap the surface by [cornerRadius]** — a negative
 * `layout_marginTop` for [Edge.BOTTOM], a negative `layout_marginBottom` for
 * [Edge.TOP] — and sit at a lower `translationZ` than it. The shadow that
 * belongs in the notch beside a rounded corner lives *above* the surface's
 * bottom edge; clipping the strip to start at that edge cut it away and left a
 * straight band running past two corners that had no shadow at all. The
 * surface's own opaque background hides the overlapping part, and the notch —
 * where that background is transparent — lets the curve through.
 *
 * Height should be [cornerRadius] plus at least `shadowBlurRadius` so the
 * falloff is not cut short.
 */
class EdgeShadowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Edge { TOP, BOTTOM }

    var edge: Edge = Edge.BOTTOM
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var cornerRadius: Float = 0f
        set(value) {
            if (field == value) return
            field = value
            invalidate()
        }

    var blurRadius: Float = 0f
        set(value) {
            if (field == value) return
            field = value
            rebuildPaint()
            invalidate()
        }

    var shadowTint: Int = DEFAULT_TINT
        set(value) {
            if (field == value) return
            field = value
            rebuildPaint()
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()
    private val rect = RectF()
    private val radii = FloatArray(8)

    init {
        val density = resources.displayMetrics.density
        blurRadius = 8f * density

        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.EdgeShadowView)
            edge = if (a.getInt(R.styleable.EdgeShadowView_shadowEdge, 1) == 0) Edge.TOP else Edge.BOTTOM
            cornerRadius = a.getDimension(R.styleable.EdgeShadowView_shadowCornerRadius, 0f)
            blurRadius = a.getDimension(R.styleable.EdgeShadowView_shadowBlurRadius, blurRadius)
            shadowTint = a.getColor(R.styleable.EdgeShadowView_shadowTint, DEFAULT_TINT)
            a.recycle()
        }

        // BlurMaskFilter is not honoured by every hardware-accelerated canvas.
        // The strip is a few dp tall, so a software layer costs nothing here.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        rebuildPaint()
    }

    private fun rebuildPaint() {
        paint.color = shadowTint
        paint.maskFilter =
            if (blurRadius > 0f) BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL) else null
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f || blurRadius <= 0f) return

        // Tall enough that the far end of the silhouette never blurs back into
        // view, whatever the strip's height.
        val body = h + blurRadius * 4f
        val r = cornerRadius

        path.reset()
        if (edge == Edge.BOTTOM) {
            // Surface sits above, overlapping us by its corner radius, so its
            // bottom edge lands r into the strip rather than on our top edge.
            rect.set(0f, r - body, w, r)
            radii.fill(0f)
            radii[4] = r; radii[5] = r   // bottom-right
            radii[6] = r; radii[7] = r   // bottom-left
        } else {
            // Surface sits below; its top edge lands r up from our bottom edge.
            rect.set(0f, h - r, w, h - r + body)
            radii.fill(0f)
            radii[0] = r; radii[1] = r   // top-left
            radii[2] = r; radii[3] = r   // top-right
        }
        path.addRoundRect(rect, radii, Path.Direction.CW)
        canvas.drawPath(path, paint)
    }

    private companion object {
        val DEFAULT_TINT = Color.argb(38, 0, 0, 0)
    }
}
