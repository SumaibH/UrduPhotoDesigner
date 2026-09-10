package com.webscare.urducanvas.ui.splash

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.animation.PathInterpolator
import androidx.core.content.ContextCompat
import com.webscare.urducanvas.R
import kotlin.math.roundToInt

/**
 * The Home header's artwork, drawn at any size and any height: the deep-green ground
 * gradient running top-left to bottom-right, the khatam (eight-point star) lattice
 * dissolving column by column away from the left edge, and the broad white highlight
 * sweeping up to the top right — the same three ingredients as `bg_home_header_art`.
 *
 * Shared by [SplashGroundView] and [SplashExitOverlay] so the splash and its exit paint
 * identical pixels, and the exit can shrink the ground into the header without the
 * artwork changing under it.
 */
class SplashArt(context: Context) {

    val density: Float = context.resources.displayMetrics.density

    private val groundColors = intArrayOf(
        ContextCompat.getColor(context, R.color.home_header_start),
        ContextCompat.getColor(context, R.color.home_header_center),
        ContextCompat.getColor(context, R.color.home_header_end)
    )
    private val groundPaint = Paint()
    private val groundMatrix = Matrix()
    private var groundShader: LinearGradient? = null

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = STAR_STROKE_DP * density
        color = Color.WHITE
    }
    private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = SWEEP_WIDTH_DP * density
    }

    private val diamond = Path()
    private val square = Path()
    private val sweep = Path()

    private var width = 0f
    private var height = 0f

    init {
        val d = density
        // One cell of the lattice, in a 38dp tile: a diamond over a square makes the star.
        diamond.moveTo(19f * d, 3f * d)
        diamond.lineTo(35f * d, 19f * d)
        diamond.lineTo(19f * d, 35f * d)
        diamond.lineTo(3f * d, 19f * d)
        diamond.close()
        square.addRect(7.5f * d, 7.5f * d, 30.5f * d, 30.5f * d, Path.Direction.CW)
    }

    /** Sizes the gradient and the sweep to a frame. The lattice needs no sizing. */
    fun setSize(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        width = w.toFloat()
        height = h.toFloat()

        groundShader = LinearGradient(
            0f, 0f, width, height,
            groundColors, floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        ).also { groundPaint.shader = it }

        // The sweep was drawn on a 390 x 844 frame; stretch it to whatever this screen is.
        val sx = width / DESIGN_WIDTH
        val sy = height / DESIGN_HEIGHT
        sweep.rewind()
        sweep.moveTo(-30f * sx, 760f * sy)
        sweep.cubicTo(140f * sx, 640f * sy, 260f * sx, 420f * sy, 420f * sx, 120f * sy)
        sweepPaint.shader = LinearGradient(
            0f, 760f * sy, width, 120f * sy,
            intArrayOf(
                Color.argb(0, 255, 255, 255),
                Color.argb(11, 255, 255, 255),
                Color.argb(24, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    /**
     * The ground over (0, 0) to (width, [bottom]): the whole gradient squeezed to that
     * height, so a rect shrinking towards the header keeps a complete gradient inside it
     * and lands on exactly what the header paints.
     */
    fun drawGround(canvas: Canvas, bottom: Float) {
        val shader = groundShader ?: return
        groundMatrix.setScale(1f, bottom / height)
        shader.setLocalMatrix(groundMatrix)
        canvas.drawRect(0f, 0f, width, bottom, groundPaint)
    }

    /** The lattice down to [bottom], at [alpha] times its designed strength. */
    fun drawLattice(canvas: Canvas, bottom: Float, alpha: Float) {
        if (alpha <= 0f) return
        val cell = CELL_DP * density
        val origin = LATTICE_ORIGIN_DP * density
        COLUMN_ALPHAS.forEachIndexed { column, columnAlpha ->
            starPaint.alpha = (255f * columnAlpha * alpha).roundToInt()
            val x = origin + column * cell
            var y = origin
            while (y < bottom) {
                canvas.save()
                canvas.translate(x, y)
                canvas.drawPath(diamond, starPaint)
                canvas.drawPath(square, starPaint)
                canvas.restore()
                y += cell
            }
        }
    }

    /** The highlight sweep at [alpha] times its designed strength. */
    fun drawSweep(canvas: Canvas, alpha: Float) {
        if (alpha <= 0f) return
        sweepPaint.alpha = (255f * alpha).roundToInt()
        canvas.drawPath(sweep, sweepPaint)
    }

    companion object {
        const val DESIGN_WIDTH = 390f
        const val DESIGN_HEIGHT = 844f
        const val CELL_DP = 38f
        const val LATTICE_ORIGIN_DP = -12f
        const val STAR_STROKE_DP = 0.9f
        const val SWEEP_WIDTH_DP = 120f

        /** Stroke alpha per lattice column, left to right, as in the Home header art. */
        val COLUMN_ALPHAS = floatArrayOf(0.14f, 0.10f, 0.065f, 0.035f, 0.016f)

        /** The design's ease, shared by every splash movement: quick out of the gate, long settle. */
        val EMPHASIZED = PathInterpolator(0.2f, 0.8f, 0.2f, 1f)

        fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t
    }
}
