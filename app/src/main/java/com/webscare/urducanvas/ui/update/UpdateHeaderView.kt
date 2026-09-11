package com.webscare.urducanvas.ui.update

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.webscare.urducanvas.R
import com.webscare.urducanvas.ui.splash.SplashArt
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The header band of the update sheet.
 *
 * The app has no dialogs and no stock illustrations, so rather than drop a generic
 * download arrow on a flat panel this paints the same ground the splash and the Home
 * header do — [SplashArt]'s green gradient, khatam lattice and highlight sweep — and
 * lands the app's own mark on it.
 *
 * The movement is deliberately slight: the mark rises into place once, then breathes
 * on a long sine while two rings pulse out from behind it and the highlight travels
 * across the band. Nothing loops fast enough to compete with the words underneath it.
 */
class UpdateHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val art = SplashArt(context)
    private val density = resources.displayMetrics.density

    private val mark: Drawable? = AppCompatResources
        .getDrawable(context, R.drawable.ic_urdu_canvas)
        ?.mutate()
        ?.also { DrawableCompat.setTint(it, Color.WHITE) }

    private val discPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = RING_STROKE_DP * density
        color = Color.WHITE
    }

    /** Top corners only: the band sits in the sheet's own rounded top. */
    private val clip = Path()
    private val bounds = RectF()

    private var startedAt = 0L
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            invalidate()
            postOnAnimation(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startedAt = System.nanoTime()
        running = true
        postOnAnimation(tick)
    }

    override fun onDetachedFromWindow() {
        running = false
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        art.setSize(w, h)

        bounds.set(0f, 0f, w.toFloat(), h.toFloat())
        val radius = CORNER_DP * density
        clip.rewind()
        clip.addRoundRect(
            bounds,
            floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f),
            Path.Direction.CW
        )

        discPaint.shader = RadialGradient(
            w / 2f, h / 2f, discRadius(),
            intArrayOf(
                Color.argb(56, 255, 255, 255),
                Color.argb(20, 255, 255, 255),
                Color.argb(0, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.62f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val elapsed = (System.nanoTime() - startedAt) / 1_000_000f

        canvas.save()
        canvas.clipPath(clip)

        art.drawGround(canvas, h)
        art.drawLattice(canvas, h, LATTICE_STRENGTH)
        drawTravellingSweep(canvas, w, elapsed)

        // The mark rises once and then holds; everything after is the breath.
        val entry = SplashArt.EMPHASIZED.getInterpolation(
            (elapsed / ENTRY_MS).coerceIn(0f, 1f)
        )
        val breath = sin(elapsed / BREATH_MS * TWO_PI).toFloat()
        val cx = w / 2f
        val cy = h / 2f + SplashArt.lerp(RISE_DP * density, 0f, entry) + breath * BOB_DP * density

        drawRings(canvas, cx, cy, elapsed, entry)

        discPaint.alpha = (255f * entry).roundToInt()
        canvas.drawCircle(cx, cy, discRadius(), discPaint)

        drawMark(canvas, cx, cy, entry, breath)

        canvas.restore()
    }

    /**
     * The sweep travels rather than sitting still, so the band reads as lit from a
     * moving source. It is off-canvas for most of the cycle — a highlight that came
     * round every second would be a strobe, not a sheen.
     */
    private fun drawTravellingSweep(canvas: Canvas, w: Float, elapsed: Float) {
        val phase = (elapsed % SWEEP_CYCLE_MS) / SWEEP_CYCLE_MS
        val travel = SplashArt.lerp(-w, w * 2f, phase)
        // Full strength in the middle of the pass, nothing at either end.
        val strength = sin(phase * Math.PI).toFloat()
        canvas.save()
        canvas.translate(travel, 0f)
        art.drawSweep(canvas, strength * SWEEP_STRENGTH)
        canvas.restore()
    }

    /** Two rings out of the disc, half a cycle apart, so one is always on its way out. */
    private fun drawRings(canvas: Canvas, cx: Float, cy: Float, elapsed: Float, entry: Float) {
        if (entry <= 0f) return
        val base = discRadius()
        for (i in 0 until RINGS) {
            val phase = ((elapsed / RING_CYCLE_MS) + i.toFloat() / RINGS) % 1f
            val radius = SplashArt.lerp(base * 0.92f, base * RING_REACH, phase)
            // Fades the whole way out, and holds back until the mark has landed.
            val alpha = (1f - phase) * RING_ALPHA * entry
            ringPaint.alpha = (255f * alpha).roundToInt()
            canvas.drawCircle(cx, cy, radius, ringPaint)
        }
    }

    private fun drawMark(canvas: Canvas, cx: Float, cy: Float, entry: Float, breath: Float) {
        val drawable = mark ?: return
        val targetHeight = MARK_HEIGHT_DP * density
        val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight
        val markHeight = targetHeight
        val markWidth = targetHeight * ratio

        // Scales up into place with the rise, then keeps the faintest of pulses.
        val scale = SplashArt.lerp(ENTRY_SCALE, 1f, entry) + breath * BREATH_SCALE

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(scale, scale)
        drawable.alpha = (255f * entry).roundToInt()
        drawable.setBounds(
            (-markWidth / 2f).roundToInt(),
            (-markHeight / 2f).roundToInt(),
            (markWidth / 2f).roundToInt(),
            (markHeight / 2f).roundToInt()
        )
        drawable.draw(canvas)
        canvas.restore()
    }

    private fun discRadius() = min(width, height) * DISC_FRACTION

    companion object {
        private const val CORNER_DP = 30f
        private const val MARK_HEIGHT_DP = 52f
        private const val RING_STROKE_DP = 1.1f
        private const val RISE_DP = 14f
        private const val BOB_DP = 2.5f

        private const val DISC_FRACTION = 0.34f
        private const val LATTICE_STRENGTH = 1f
        private const val SWEEP_STRENGTH = 0.9f

        private const val ENTRY_MS = 620f
        private const val ENTRY_SCALE = 0.82f
        private const val BREATH_MS = 3200f
        private const val BREATH_SCALE = 0.012f
        private const val RING_CYCLE_MS = 2600f
        private const val SWEEP_CYCLE_MS = 5200f

        private const val RINGS = 2
        private const val RING_REACH = 1.95f
        private const val RING_ALPHA = 0.30f

        private const val TWO_PI = (2.0 * Math.PI).toFloat()
    }
}
