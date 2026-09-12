package com.webscare.urducanvas.ui.editor.panels.preview

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import kotlin.math.hypot

/**
 * Pinch, drag and double-tap to look closer at the previewed asset.
 *
 * [surface] takes the touches — the paper — and [target] is what moves: the layer
 * holding whatever the paper is showing. Transforming that layer rather than the
 * paper means one gesture covers every asset the preview can hold, including a
 * font specimen, which is live text and not an image at all. The paper itself
 * stays put and clips, so a zoomed asset is cropped by the page rather than
 * spilling over the well.
 *
 * The app already has a [com.webscare.urducanvas.common.views.ZoomableImageView],
 * and it is the wrong tool here twice over: it only zooms a `Drawable`, and its
 * swipe-down-to-dismiss has no off switch — inside a sheet that would fight the
 * panel's own drag.
 */
class PreviewZoom(
    private val surface: View,
    private val target: View
) {

    private var scale = 1f
    private var panX = 0f
    private var panY = 0f

    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private var animator: ValueAnimator? = null

    private val touchSlop = ViewConfiguration.get(surface.context).scaledTouchSlop

    private val scaleDetector = ScaleGestureDetector(
        surface.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                animator?.cancel()
                scale = (scale * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
                apply()
                return true
            }
        }
    )

    private val tapDetector = GestureDetector(
        surface.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                animateTo(if (scale > 1.05f) 1f else DOUBLE_TAP_SCALE)
                return true
            }
        }
    )

    @SuppressLint("ClickableViewAccessibility")
    fun attach() {
        surface.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            tapDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    dragging = false
                    // Already zoomed, so a one-finger drag is a pan and the sheet must
                    // not read it as a drag to dismiss.
                    if (scale > 1f) holdGesture()
                }

                // The second finger means a pinch. Consuming is not enough on its own:
                // the sheet's behavior intercepts from the parent, and a pinch with any
                // downward drift would be taken as a swipe to dismiss.
                MotionEvent.ACTION_POINTER_DOWN -> holdGesture()

                MotionEvent.ACTION_MOVE -> if (scale > 1f && !scaleDetector.isInProgress) {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    if (!dragging && hypot(dx, dy) > touchSlop) {
                        dragging = true
                        holdGesture()
                    }
                    if (dragging) {
                        animator?.cancel()
                        panX += dx
                        panY += dy
                        lastX = event.rawX
                        lastY = event.rawY
                        apply()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    // A pinch that ended below life size springs back rather than
                    // leaving the asset stranded smaller than the page.
                    if (scale < 1f) animateTo(1f)
                }
            }
            // Always consumed: returning false on the DOWN would mean never seeing the
            // second finger, and there would be no pinch to detect.
            true
        }
    }

    /**
     * Claims the rest of this gesture from every ancestor.
     *
     * Consuming the event is not enough: a parent can still intercept, and the sheet
     * this sits in does exactly that — anything downward becomes a drag to dismiss.
     * Only asked for once a gesture is known to be a zoom or a pan, so a plain
     * one-finger drag on an unzoomed asset still closes the sheet the usual way.
     */
    private fun holdGesture() {
        surface.parent?.requestDisallowInterceptTouchEvent(true)
    }

    /** Back to life size. Called whenever the preview changes what it is showing. */
    fun reset() {
        animator?.cancel()
        animator = null
        scale = 1f
        panX = 0f
        panY = 0f
        apply()
    }

    private fun animateTo(end: Float) {
        animator?.cancel()
        val start = scale
        animator = ValueAnimator.ofFloat(start, end).apply {
            duration = SETTLE_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                scale = it.animatedValue as Float
                // Pulling back towards life size drags the pan home with it, so the
                // asset does not finish off-centre.
                val t = if (end == start) 1f else (scale - start) / (end - start)
                if (end <= 1f) {
                    panX *= (1f - t)
                    panY *= (1f - t)
                }
                apply()
            }
            start()
        }
    }

    private fun apply() {
        // Panning is bounded by how much of the layer the zoom has pushed out of
        // view; at life size that is zero, which re-centres for free.
        val maxX = target.width * (scale - 1f) / 2f
        val maxY = target.height * (scale - 1f) / 2f
        panX = panX.coerceIn(-maxX.coerceAtLeast(0f), maxX.coerceAtLeast(0f))
        panY = panY.coerceIn(-maxY.coerceAtLeast(0f), maxY.coerceAtLeast(0f))

        target.scaleX = scale
        target.scaleY = scale
        target.translationX = panX
        target.translationY = panY
    }

    companion object {
        /** Below life size only during a pinch; it springs back on release. */
        private const val MIN_SCALE = 0.6f
        private const val MAX_SCALE = 5f
        private const val DOUBLE_TAP_SCALE = 2.5f
        private const val SETTLE_MS = 180L
    }
}
