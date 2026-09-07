package com.webscare.urducanvas.common.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import androidx.core.widget.NestedScrollView
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.math.abs

class SpringNestedScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : NestedScrollView(context, attrs, defStyle) {

    private val MAX_OVERSCROLL_FRACTION = 0.30f
    private val RUBBER_EXPONENT         = 0.50

    private var velocityTracker: VelocityTracker? = null
    private var lastY      = 0f
    private var isBouncing = false
    private var lastFlingVelocity = 0f

    private var springAnim: SpringAnimation? = null

    // ── We animate the CONTENT child, not the scroll view itself.
    // This keeps the scroll view clipped inside its layout bounds so it
    // never overlaps the header/card above it.
    private val scrollChild get() = getChildAt(0)

    private val isAtTop    get() = scrollY <= 0
    private val isAtBottom get(): Boolean {
        val child = scrollChild ?: return false
        val maxScroll = child.height - (height - paddingTop - paddingBottom)
        return scrollY >= maxScroll.coerceAtLeast(0)
    }

    private val maxTranslation get() = height * MAX_OVERSCROLL_FRACTION

    // ── Gesture state ─────────────────────────────────────────────────────────

    /**
     * True while the user is dragging, whether the touch lands on this view or on
     * one of the RecyclerViews inside it.
     *
     * A plain OnTouchListener is not enough: the child lists are nested-scrolling
     * children, so a drag starting on one of them is handled by the child and
     * passed up through the nested-scroll callbacks — this view's own touch
     * methods never see that gesture at all.
     */
    var isGestureInProgress = false
        private set

    /** Fired when a drag ends, from either path. */
    var onGestureEnd: (() -> Unit)? = null

    override fun onStartNestedScroll(child: android.view.View, target: android.view.View, axes: Int, type: Int): Boolean {
        if (type == androidx.core.view.ViewCompat.TYPE_TOUCH) isGestureInProgress = true
        return super.onStartNestedScroll(child, target, axes, type)
    }

    override fun onStopNestedScroll(target: android.view.View, type: Int) {
        super.onStopNestedScroll(target, type)
        if (type == androidx.core.view.ViewCompat.TYPE_TOUCH) {
            isGestureInProgress = false
            onGestureEnd?.invoke()
        }
    }

    private var initialX = 0f
    private var initialY = 0f
    private var isDraggingHorizontally = false
    private val touchSlop by lazy { android.view.ViewConfiguration.get(context).scaledTouchSlop }

    // ── Touch ─────────────────────────────────────────────────────────────────

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        trackVelocity(ev)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
                lastY = ev.rawY
                isDraggingHorizontally = false
                if (isBouncing || abs(scrollChild?.translationY ?: 0f) > 0.5f) {
                    springAnim?.cancel()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingHorizontally) {
                    return false
                }
                val dx = abs(ev.x - initialX)
                val dy = abs(ev.y - initialY)
                // If the gesture is predominantly horizontal, do not intercept!
                // This lets horizontal RecyclerViews scroll smoothly without fighting.
                if (dx > touchSlop && dx > dy) {
                    isDraggingHorizontally = true
                    return false
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingHorizontally = false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
        if (disallowIntercept) {
            recycleVelocity()
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        trackVelocity(ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isGestureInProgress = false
                onGestureEnd?.invoke()
            }

            MotionEvent.ACTION_DOWN -> isGestureInProgress = true
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                springAnim?.cancel()
                lastFlingVelocity = 0f
                lastY = ev.rawY
                initialX = ev.x
                initialY = ev.y
                isDraggingHorizontally = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dy = lastY - ev.rawY
                lastY = ev.rawY

                val pullUp   = dy < 0 && isAtTop    && !canScrollVertically(-1)
                val pullDown = dy > 0 && isAtBottom && !canScrollVertically(1)

                if (pullUp || pullDown || isBouncing) {
                    applyRubberBand(-dy)
                    return true
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                val velocityY = captureVelocityY()
                recycleVelocity()

                val child = scrollChild
                if (child != null && (isBouncing || abs(child.translationY) > 0f)) {
                    springBack(velocityY)
                    return true
                }
            }
        }

        return super.onTouchEvent(ev)
    }

    override fun fling(velocityY: Int) {
        super.fling(velocityY)
        lastFlingVelocity = velocityY.toFloat()
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)

        if (clampedY && abs(lastFlingVelocity) > 0f && !isBouncing) {
            val child = scrollChild ?: return
            if (child.translationY == 0f) {
                val velocity = lastFlingVelocity
                lastFlingVelocity = 0f
                val sign = if (scrollY <= 0) 1f else -1f
                springFromFling(abs(velocity) * sign)
            }
        }
    }

    // ── Rubber-band: move only the content child ───────────────────────────────

    private fun applyRubberBand(delta: Float) {
        isBouncing = true
        springAnim?.cancel()
        val child = scrollChild ?: return
        child.translationY = (child.translationY + delta * 0.3f)
            .coerceIn(-maxTranslation, maxTranslation)
    }

    // ── Fling overscroll: spring the content child back ────────────────────────

    private fun springFromFling(velocity: Float) {
        val child = scrollChild ?: return
        springAnim?.cancel()
        isBouncing = true

        springAnim = SpringAnimation(child, SpringAnimation.TRANSLATION_Y, 0f).apply {
            spring = SpringForce(0f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness    = SpringForce.STIFFNESS_LOW
            }
            setStartVelocity(velocity * 0.3f)
            addEndListener { _, _, _, _ ->
                child.translationY = 0f
                isBouncing = false
            }
            start()
        }
    }

    // ── Snap back: spring the content child to rest ────────────────────────────

    private fun springBack(velocityY: Float) {
        val child = scrollChild ?: return
        if (abs(child.translationY) < 0.5f) {
            child.translationY = 0f
            isBouncing = false
            return
        }

        springAnim?.cancel()
        springAnim = SpringAnimation(child, SpringAnimation.TRANSLATION_Y, 0f).apply {
            spring = SpringForce(0f).apply {
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                stiffness    = SpringForce.STIFFNESS_LOW
            }
            setStartVelocity(velocityY * 0.3f)
            addEndListener { _, _, _, _ ->
                child.translationY = 0f
                isBouncing = false
            }
            start()
        }
    }

    // ── Velocity tracker ──────────────────────────────────────────────────────

    private fun trackVelocity(ev: MotionEvent) {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            velocityTracker?.recycle()
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(ev)
    }

    private fun captureVelocityY(): Float {
        velocityTracker?.computeCurrentVelocity(1000)
        return velocityTracker?.yVelocity ?: 0f
    }

    private fun recycleVelocity() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        springAnim?.cancel()
        scrollChild?.translationY = 0f
        recycleVelocity()
    }
}