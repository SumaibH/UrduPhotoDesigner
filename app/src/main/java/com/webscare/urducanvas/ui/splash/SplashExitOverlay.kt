package com.webscare.urducanvas.ui.splash

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.text.TextPaint
import android.view.View
import android.view.animation.PathInterpolator
import com.webscare.urducanvas.ui.splash.SplashArt.Companion.lerp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The settled splash — ground, art, calligraphy, mark, wordmark, tagline and loading
 * line — redrawn above the navigation host so it can come apart into Home's header
 * while Home lays itself out underneath.
 *
 * Two states. Until a [target] is known it paints the splash exactly as the fragment
 * left it, with the loading line still running, so the wait for Home is indistinguishable
 * from the splash itself; the wait is anything from a tenth of a second to a couple of
 * seconds depending on the phone.
 *
 * Then everything runs off one [progress]. The ground shrinks to the header's bottom
 * edge, growing the header's corners on the way. The wordmark travels and shrinks onto
 * the title's baseline, and the mark and tagline ride the same path — shrinking toward
 * the title as one lockup — while they fade, so nothing ever vanishes from a picture
 * that is standing still. The loading line and publisher fade as the rising bottom edge
 * reaches them. The calligraphy drifts onto the spot the header keeps it.
 *
 * One draw pass, no layout, cached paths and paints, so a low-end phone can hold 60 fps.
 */
class SplashExitOverlay(context: Context, private val spec: SplashExitSpec) : View(context) {

    private val art = SplashArt(context)
    private val clip = Path()
    private val radii = FloatArray(8)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scratch = RectF()
    private val location = IntArray(2)

    private val wordPaint = TextPaint(spec.wordmark.paint)
    private val taglinePaint = spec.tagline?.let { TextPaint(it.paint) }
    private val publisherPaint = spec.publisher?.let { TextPaint(it.paint) }
    private val taglineAlpha = taglinePaint?.alpha ?: 0
    private val publisherAlpha = publisherPaint?.alpha ?: 0

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
        val collapse =
            if (landing == null) 0f
            else COLLAPSE_EASE.getInterpolation((progress / COLLAPSE_END).coerceIn(0f, 1f))

        val bottom = if (landing == null) fullBottom else lerp(fullBottom, landing.headerBottom, collapse)
        val radius = if (landing == null) 0f else lerp(0f, landing.cornerRadius, collapse)

        // ── The ground, and everything the collapsing edge is allowed to swallow ──
        radii[4] = radius; radii[5] = radius; radii[6] = radius; radii[7] = radius
        clip.rewind()
        clip.addRoundRect(left, top, left + w, bottom, radii, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clip)

        canvas.save()
        canvas.translate(left, top)
        art.drawGround(canvas, bottom - top)
        art.drawLattice(canvas, bottom - top, 1f)
        art.drawSweep(canvas, 1f - (progress / SWEEP_END).coerceIn(0f, 1f))
        canvas.restore()

        // The calligraphy drifts from where the splash keeps it to where the header does.
        val watermark = spec.watermark
        if (watermark != null && watermark.alpha > 0f) {
            val end = landing?.watermarkBounds
            if (end == null) {
                scratch.set(watermark.bounds)
            } else {
                scratch.set(
                    lerp(watermark.bounds.left, end.left, collapse),
                    lerp(watermark.bounds.top, end.top, collapse),
                    lerp(watermark.bounds.right, end.right, collapse),
                    lerp(watermark.bounds.bottom, end.bottom, collapse)
                )
            }
            watermark.drawable.setBounds(
                scratch.left.roundToInt(), scratch.top.roundToInt(),
                scratch.right.roundToInt(), scratch.bottom.roundToInt()
            )
            watermark.drawable.alpha = (255f * watermark.alpha).roundToInt()
            watermark.drawable.draw(canvas)
        }

        // The loading line and the publisher go out as the bottom edge climbs over them.
        val footer = 1f - (progress / FOOTER_FADE_END).coerceIn(0f, 1f)
        if (footer > 0f) {
            drawLoader(canvas, footer)
            val publisher = spec.publisher
            if (publisher != null && publisherPaint != null) {
                publisherPaint.alpha = (publisherAlpha * footer).roundToInt()
                canvas.drawText(publisher.text, publisher.x, publisher.baseline, publisherPaint)
            }
        }
        canvas.restore()

        // ── The lockup: mark, wordmark and tagline compacting onto the title ──
        val restSize = spec.wordmark.paint.textSize
        val wordSize = if (landing == null) restSize else lerp(restSize, landing.titlePaint.textSize, collapse)
        val wordX = if (landing == null) spec.wordmark.x else lerp(spec.wordmark.x, landing.titleX, collapse)
        val wordBaseline =
            if (landing == null) spec.wordmark.baseline
            else lerp(spec.wordmark.baseline, landing.titleBaseline, collapse)

        // The mark and tagline are carried by the wordmark's own journey, so the three
        // read as one thing gathering into the title rather than three things leaving.
        val lockupScale = if (restSize > 0f) wordSize / restSize else 1f
        val lockupDx = wordX - spec.wordmark.x
        val lockupDy = wordBaseline - spec.wordmark.baseline

        val mark = spec.mark
        val markFade = 1f - LEAVE_EASE.getInterpolation((progress / MARK_FADE_END).coerceIn(0f, 1f))
        if (mark != null && markFade > 0f) {
            canvas.save()
            canvas.translate(lockupDx, lockupDy)
            canvas.scale(lockupScale, lockupScale, spec.wordmark.x, spec.wordmark.baseline)
            // A touch tighter than the lockup, so it reads as folding in, not just shrinking.
            val tuck = lerp(1f, MARK_TUCK, 1f - markFade)
            canvas.scale(tuck, tuck, mark.bounds.centerX(), mark.bounds.centerY())
            mark.drawable.setBounds(
                mark.bounds.left.roundToInt(), mark.bounds.top.roundToInt(),
                mark.bounds.right.roundToInt(), mark.bounds.bottom.roundToInt()
            )
            mark.drawable.alpha = (255f * mark.alpha * markFade).roundToInt()
            mark.drawable.draw(canvas)
            canvas.restore()
        }

        val tagline = spec.tagline
        val taglineFade = 1f - LEAVE_EASE.getInterpolation((progress / TAGLINE_FADE_END).coerceIn(0f, 1f))
        if (tagline != null && taglinePaint != null && taglineFade > 0f) {
            canvas.save()
            canvas.translate(lockupDx, lockupDy)
            canvas.scale(lockupScale, lockupScale, spec.wordmark.x, spec.wordmark.baseline)
            taglinePaint.alpha = (taglineAlpha * taglineFade).roundToInt()
            canvas.drawText(tagline.text, tagline.x, tagline.baseline, taglinePaint)
            canvas.restore()
        }

        wordPaint.textSize = wordSize
        wordPaint.letterSpacing =
            if (landing == null) spec.wordmark.paint.letterSpacing
            else lerp(spec.wordmark.paint.letterSpacing, landing.titlePaint.letterSpacing, collapse)
        canvas.drawText(spec.wordmark.text, wordX, wordBaseline, wordPaint)

        // While Home is still laying itself out there is nothing to animate but this,
        // and a frozen loading line reads as a hung app. Only the line is invalidated.
        if (landing == null && spec.loaderTrack != null && isAttachedToWindow) {
            val track = spec.loaderTrack
            postInvalidateOnAnimation(
                (track.left - left).toInt() - 1, (track.top - top).toInt() - 1,
                (track.right - left).toInt() + 1, (track.bottom - top).toInt() + 1
            )
        }
    }

    /**
     * The splash's loading line, carried on. The indicator sweeps rather than fills: the
     * app has no idea how far along Home is, and this has to be able to run for a couple
     * of seconds on a slow phone without ever looking like it has finished.
     */
    private fun drawLoader(canvas: Canvas, alpha: Float) {
        val track = spec.loaderTrack ?: return
        val r = spec.loaderCorner

        fill.color = spec.loaderTrackColor
        fill.alpha = (Color.alpha(spec.loaderTrackColor) * alpha).roundToInt()
        canvas.drawRoundRect(track, r, r, fill)

        val phase = (SystemClock.uptimeMillis() % LOADER_CYCLE_MS) / LOADER_CYCLE_MS.toFloat()
        val segment = track.width() * LOADER_SEGMENT
        val startX = lerp(track.left - segment, track.right, LOADER_EASE.getInterpolation(phase))
        scratch.set(
            max(startX, track.left), track.top,
            min(startX + segment, track.right), track.bottom
        )
        if (scratch.width() <= 0f) return
        fill.color = spec.loaderColor
        fill.alpha = (Color.alpha(spec.loaderColor) * alpha).roundToInt()
        canvas.drawRoundRect(scratch, r, r, fill)
    }

    companion object {
        /**
         * The share of [progress] the collapse takes; the rest dissolves the overlay onto
         * Home. It ends early on purpose: the overlay can only ever paint an empty header,
         * so the sooner it hands over to the real one the sooner the header's own rows —
         * already unfolding underneath by then — are the thing being looked at.
         */
        const val COLLAPSE_END = 0.75f

        /** The share of [progress] by which the highlight sweep is gone. */
        const val SWEEP_END = 0.5f

        /** Shares of [progress] by which each piece of the splash has left. */
        const val FOOTER_FADE_END = 0.18f
        const val TAGLINE_FADE_END = 0.24f
        const val MARK_FADE_END = 0.34f

        /** How much tighter than the lockup the mark folds as it goes. */
        const val MARK_TUCK = 0.86f

        private const val LOADER_CYCLE_MS = 1500L
        private const val LOADER_SEGMENT = 0.42f

        /**
         * A gentler start than the bloom's ease: the ground lifts off the page slowly
         * enough for the content rising beneath it to be there when it is uncovered.
         */
        val COLLAPSE_EASE = PathInterpolator(0.4f, 0f, 0.2f, 1f)

        /** Pieces leave with a soft start, so their exit trails the movement rather than leading it. */
        private val LEAVE_EASE = PathInterpolator(0.3f, 0f, 0.6f, 1f)

        /** The loading line's sweep, eased at both ends like the indeterminate bar's. */
        private val LOADER_EASE = PathInterpolator(0.4f, 0f, 0.2f, 1f)
    }
}
