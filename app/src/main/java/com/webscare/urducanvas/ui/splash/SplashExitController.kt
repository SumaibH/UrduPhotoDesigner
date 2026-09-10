package com.webscare.urducanvas.ui.splash

import android.animation.ValueAnimator
import android.graphics.RectF
import android.os.Bundle
import android.os.Looper
import android.os.MessageQueue
import android.os.SystemClock
import android.text.TextPaint
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.webscare.urducanvas.BuildConfig
import com.webscare.urducanvas.ui.splash.SplashArt.Companion.EMPHASIZED

/**
 * Runs the splash's exit: the bare green ground morphing into Home's header, the
 * wordmark landing in the title slot, Home's page rising in underneath as the ground
 * lifts, and the header's own content blooming in once the ground has settled.
 *
 * [begin] puts a [SplashExitOverlay] above the navigation host that paints the bare
 * splash pixel for pixel, so the splash fragment can be popped without anything on
 * screen changing. Home is then created and laid out under that static overlay — the
 * expensive part happens while nothing is moving. The animation starts only once Home's
 * first frame is up and the main thread has gone quiet (or [IDLE_WAIT_MAX_MS] has
 * passed), from the header and title Home actually laid out, so it lands exactly and
 * runs on nothing but property changes and a single custom draw. Started any sooner, on
 * a slow phone, it would play into a thread still busy with Home's start-up and skip
 * every frame. If no landing screen turns up at all the overlay simply fades away.
 */
class SplashExitController(
    private val container: ViewGroup,
    private val fragments: FragmentManager,
    private val onRunningChanged: (running: Boolean) -> Unit
) {

    private var overlay: SplashExitOverlay? = null
    private var animator: ValueAnimator? = null
    private var callbacks: FragmentManager.FragmentLifecycleCallbacks? = null
    private var noLandingTimeout: Runnable? = null
    private var idleHandler: MessageQueue.IdleHandler? = null
    private var idleTimeout: Runnable? = null
    private var content: List<View> = emptyList()
    private var headerContent: List<View> = emptyList()
    private var beganAt = 0L

    val isRunning: Boolean get() = overlay != null

    fun begin(spec: SplashExitSpec) {
        cancel()
        beganAt = SystemClock.uptimeMillis()
        val view = SplashExitOverlay(container.context, spec).apply {
            // Above everything else in the container, shadowless since it has no outline.
            elevation = OVERLAY_ELEVATION_DP * resources.displayMetrics.density
        }
        container.addView(
            view,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        overlay = view
        onRunningChanged(true)

        val watcher = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(
                fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?
            ) {
                if (f !is SplashLanding) return
                fm.unregisterFragmentLifecycleCallbacks(this)
                callbacks = null
                // Home exists now; however long it takes to draw, it will draw.
                noLandingTimeout?.let { view.removeCallbacks(it) }
                noLandingTimeout = null
                log("landing view created")
                // Hidden before Home's first frame, so both rises start from nothing.
                val density = v.resources.displayMetrics.density
                content = f.landingContent()
                content.forEach {
                    it.alpha = 0f
                    it.translationY = RISE_DP * density
                }
                headerContent = f.landingHeaderContent()
                headerContent.forEach {
                    it.alpha = 0f
                    it.translationY = HEADER_RISE_DP * density
                }
                v.doOnPreDraw {
                    log("landing first frame")
                    landWhenIdle(f)
                }
            }
        }
        callbacks = watcher
        fragments.registerFragmentLifecycleCallbacks(watcher, false)

        noLandingTimeout = Runnable {
            if (overlay === view) {
                log("no landing screen; dismissing")
                dismiss()
            }
        }.also { view.postDelayed(it, NO_LANDING_TIMEOUT_MS) }
    }

    /** Waits for the main thread to drain Home's start-up work, then lands. */
    private fun landWhenIdle(landing: SplashLanding) {
        val view = overlay ?: return
        val handler = MessageQueue.IdleHandler {
            clearIdleWait()
            land(landing)
            false
        }
        val timeout = Runnable {
            clearIdleWait()
            land(landing)
        }
        idleHandler = handler
        idleTimeout = timeout
        Looper.myQueue().addIdleHandler(handler)
        view.postDelayed(timeout, IDLE_WAIT_MAX_MS)
    }

    private fun clearIdleWait() {
        idleHandler?.let { Looper.myQueue().removeIdleHandler(it) }
        idleTimeout?.let { overlay?.removeCallbacks(it) }
        idleHandler = null
        idleTimeout = null
    }

    private fun land(landing: SplashLanding) {
        val view = overlay ?: return
        val header = landing.landingHeader()
        val title = landing.landingTitle()
        if (header == null || title == null || header.height == 0) {
            dismiss()
            return
        }

        val location = IntArray(2)
        header.getLocationInWindow(location)
        val headerBottom = location[1] + header.height.toFloat()
        title.getLocationInWindow(location)
        view.target = SplashLandingTarget(
            headerBottom = headerBottom,
            cornerRadius = HEADER_CORNER_DP * view.resources.displayMetrics.density,
            titleX = location[0] + title.compoundPaddingLeft.toFloat(),
            titleBaseline = location[1] + title.baseline.toFloat(),
            titlePaint = TextPaint(title.paint),
            watermarkBounds = landing.landingWatermark()?.let { drawnBoundsInWindow(it) }
        )
        log("landing: header bottom ${headerBottom.toInt()}px, title at (${location[0]}, ${location[1] + title.baseline})")

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = EXIT_MS
            interpolator = LinearInterpolator()
            addUpdateListener {
                val p = it.animatedValue as Float
                view.progress = p
                // The last stretch dissolves the overlay onto the header it now matches.
                view.alpha = 1f - ((p - SplashExitOverlay.COLLAPSE_END) / (1f - SplashExitOverlay.COLLAPSE_END)).coerceIn(0f, 1f)
            }
            doOnEnd {
                log("landed")
                finish()
            }
            start()
        }
        // The page rises as the ground lifts off it; the header's own content blooms in
        // once the ground has settled into the header.
        content.forEach { rise(it, RISE_DELAY_MS, RISE_MS) }
        headerContent.forEach { rise(it, HEADER_RISE_DELAY_MS, HEADER_RISE_MS) }
    }

    private fun rise(view: View, delay: Long, duration: Long) {
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(duration)
            .setInterpolator(EMPHASIZED)
            .withLayer()
            .start()
    }

    /**
     * Where an image view actually paints its drawable, in window coordinates, with its
     * scale type, scale and translation applied — the header's calligraphy is scaled up
     * and pushed off the corner, so its view bounds say nothing about where the ink is.
     */
    private fun drawnBoundsInWindow(view: ImageView): RectF? {
        val drawable = view.drawable ?: return null
        val parent = view.parent as? View ?: return null
        val rect = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        view.imageMatrix.mapRect(rect)
        rect.offset(view.paddingLeft.toFloat(), view.paddingTop.toFloat())
        view.matrix.mapRect(rect)
        val location = IntArray(2)
        parent.getLocationInWindow(location)
        rect.offset(location[0] + view.left.toFloat(), location[1] + view.top.toFloat())
        return rect
    }

    /** No landing screen came: fade the overlay out and let whatever is there show. */
    private fun dismiss() {
        val view = overlay ?: return
        restoreContent()
        view.animate().alpha(0f).setDuration(DISMISS_MS).withEndAction { finish() }.start()
    }

    private fun finish() {
        val view = overlay ?: return
        overlay = null
        animator = null
        container.removeView(view)
        content = emptyList()
        headerContent = emptyList()
        onRunningChanged(false)
    }

    private fun restoreContent() {
        (content + headerContent).forEach {
            it.animate().cancel()
            it.alpha = 1f
            it.translationY = 0f
        }
    }

    /** Drops everything at once (the activity is going away). */
    fun cancel() {
        callbacks?.let { fragments.unregisterFragmentLifecycleCallbacks(it) }
        callbacks = null
        clearIdleWait()
        animator?.cancel()
        animator = null
        val view = overlay ?: return
        noLandingTimeout?.let { view.removeCallbacks(it) }
        noLandingTimeout = null
        view.animate().cancel()
        restoreContent()
        finish()
    }

    private fun log(what: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "$what (+${SystemClock.uptimeMillis() - beganAt}ms)")
        }
    }

    private companion object {
        const val TAG = "SplashExit"
        const val EXIT_MS = 700L

        /** The page starts rising the moment the ground starts lifting off it. */
        const val RISE_DELAY_MS = 0L
        const val RISE_MS = 450L
        const val RISE_DP = 24f

        /** The header's content blooms in as the ground settles into the header. */
        const val HEADER_RISE_DELAY_MS = 520L
        const val HEADER_RISE_MS = 340L
        const val HEADER_RISE_DP = 10f

        const val DISMISS_MS = 250L
        const val HEADER_CORNER_DP = 28f
        const val OVERLAY_ELEVATION_DP = 64f

        /** How long to wait for any landing screen to be created before giving up. */
        const val NO_LANDING_TIMEOUT_MS = 2500L

        /**
         * The most the collapse waits for the main thread to go quiet after Home's first
         * frame. Generous: the resting splash is what shows meanwhile, and a collapse that
         * starts into Home's start-up work drops most of its frames on a slow phone.
         */
        const val IDLE_WAIT_MAX_MS = 2000L
    }
}
