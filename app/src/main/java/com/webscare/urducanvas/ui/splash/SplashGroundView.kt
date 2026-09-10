package com.webscare.urducanvas.ui.splash

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.roundToInt

/**
 * The splash ground: the Home header's artwork ([SplashArt]) stretched over the whole
 * screen, with two levers the bloom animates.
 *
 * [latticeAlpha] scales the lattice and the sweep together, so the ground can be
 * revealed first and the detail faded up behind it. [iconTintAlpha] lays the launcher
 * icon's own two greens over the ground, so the disc growing out of the icon starts in
 * the icon's colour and deepens into the brand gradient as it spreads.
 */
class SplashGroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val art = SplashArt(context)
    private val tintPaint = Paint()
    private var tintColors: IntArray? = null

    /** 0 hides the lattice and the sweep, 1 shows them at their designed strength. */
    var latticeAlpha: Float = 1f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (clamped != field) {
                field = clamped
                invalidate()
            }
        }

    /** 1 paints the ground entirely in the icon's greens, 0 leaves the brand gradient alone. */
    var iconTintAlpha: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (clamped != field) {
                field = clamped
                invalidate()
            }
        }

    /** The icon's lighter (top-left) and darker (bottom-right) greens, sampled off the icon itself. */
    fun setIconTint(light: Int, dark: Int) {
        tintColors = intArrayOf(light, dark)
        rebuildTint()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        art.setSize(w, h)
        rebuildTint()
    }

    private fun rebuildTint() {
        val colors = tintColors
        if (colors == null || width == 0 || height == 0) {
            tintPaint.shader = null
            return
        }
        tintPaint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(), colors, null, Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val h = height.toFloat()
        art.drawGround(canvas, h)
        if (iconTintAlpha > 0f && tintPaint.shader != null) {
            tintPaint.alpha = (255f * iconTintAlpha).roundToInt()
            canvas.drawRect(0f, 0f, width.toFloat(), h, tintPaint)
        }
        art.drawLattice(canvas, h, latticeAlpha)
        art.drawSweep(canvas, latticeAlpha)
    }
}
