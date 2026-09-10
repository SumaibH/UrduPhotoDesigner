package com.webscare.urducanvas.ui.splash

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.text.TextPaint
import android.view.View
import android.view.animation.PathInterpolator
import com.webscare.urducanvas.ui.splash.SplashArt.Companion.lerp
import kotlin.math.roundToInt

/**
 * The bare splash — ground, art, calligraphy and wordmark — redrawn above the
 * navigation host so it can morph into Home's header while Home lays itself out
 * underneath.
 *
 * Drives everything off one [progress]: the ground shrinks from the full screen to the
 * header's bottom edge and grows the header's rounded corners on the way; the wordmark
 * travels and shrinks from the middle of the splash onto the title's baseline; the
 * calligraphy drifts and scales onto the spot the header keeps it; the highlight sweep
 * fades. Until a [target] is known it simply paints the resting splash. Everything is
 * drawn in a single pass with no layout and cached paths, so a low-end phone can hold
 * 60 fps.
 */
class SplashExitOverlay(context: Context, private val spec: SplashExitSpec) : View(context) {

    private val art = SplashArt(context)
    private val clip = Path()
    private val radii = FloatArray(8)
    private val textPaint = TextPaint(spec.textPaint)
    private val watermarkRect = RectF()
    private val location = IntArray(2)

    /** Where to land. Set once Home's header and title are laid out. */
    var target: SplashLandingTarget? = null
        set(value) {
            field = value
            invalidate()
        }

    /** 0 is the resting splash, 1 the overlay sitting exactly on Home's header. */
    var progress: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (clamped != field) {
                field = clamped
                invalidate()
            }
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        art.setSize(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        // Everything in the spec and the target is in window coordinates.
        getLocationInWindow(location)
        val left = location[0].toFloat()
        val top = location[1].toFloat()
        canvas.translate(-left, -top)
        val fullBottom = top + h

        val landing = target
        val collapse = if (landing == null) 0f else COLLAPSE_EASE.getInterpolation((progress / COLLAPSE_END).coerceIn(0f, 1f))
        val sweepAlpha = 1f - (progress / SWEEP_END).coerceIn(0f, 1f)

        val bottom = if (landing == null) fullBottom else lerp(fullBottom, landing.headerBottom, collapse)
        val radius = if (landing == null) 0f else lerp(0f, landing.cornerRadius, collapse)

        // The ground: a rect growing the header's bottom corners, clipped and painted.
        radii[4] = radius; radii[5] = radius; radii[6] = radius; radii[7] = radius
        clip.rewind()
        clip.addRoundRect(left, top, left + w, bottom, radii, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clip)
        canvas.save()
        canvas.translate(left, top)
        art.drawGround(canvas, bottom - top)
        art.drawLattice(canvas, bottom - top, 1f)
        art.drawSweep(canvas, sweepAlpha)
        canvas.restore()

        // The calligraphy drifts from where the splash keeps it to where the header does.
        val watermark = spec.watermark
        if (watermark != null && spec.watermarkAlpha > 0f) {
            val end = landing?.watermarkBounds
            if (end == null) {
                watermarkRect.set(spec.watermarkBounds)
            } else {
                watermarkRect.set(
                    lerp(spec.watermarkBounds.left, end.left, collapse),
                    lerp(spec.watermarkBounds.top, end.top, collapse),
                    lerp(spec.watermarkBounds.right, end.right, collapse),
                    lerp(spec.watermarkBounds.bottom, end.bottom, collapse)
                )
            }
            watermark.setBounds(
                watermarkRect.left.roundToInt(), watermarkRect.top.roundToInt(),
                watermarkRect.right.roundToInt(), watermarkRect.bottom.roundToInt()
            )
            watermark.alpha = (255f * spec.watermarkAlpha).roundToInt()
            watermark.draw(canvas)
        }
        canvas.restore()

        // The wordmark rides from the middle of the splash onto the title's baseline.
        if (landing == null) {
            textPaint.textSize = spec.textPaint.textSize
            textPaint.letterSpacing = spec.textPaint.letterSpacing
            canvas.drawText(spec.text, spec.textX, spec.textBaseline, textPaint)
        } else {
            textPaint.textSize = lerp(spec.textPaint.textSize, landing.titlePaint.textSize, collapse)
            textPaint.letterSpacing = lerp(spec.textPaint.letterSpacing, landing.titlePaint.letterSpacing, collapse)
            val x = lerp(spec.textX, landing.titleX, collapse)
            val baseline = lerp(spec.textBaseline, landing.titleBaseline, collapse)
            canvas.drawText(spec.text, x, baseline, textPaint)
        }
    }

    companion object {
        /** The share of [progress] the collapse takes; the rest dissolves the overlay onto Home. */
        const val COLLAPSE_END = 0.8f

        /** The share of [progress] by which the highlight sweep is gone. */
        const val SWEEP_END = 0.5f

        /**
         * A gentler start than the bloom's ease: the ground lifts off the page slowly
         * enough for the content rising beneath it to be there when it is uncovered.
         */
        val COLLAPSE_EASE = PathInterpolator(0.4f, 0f, 0.2f, 1f)
    }
}
