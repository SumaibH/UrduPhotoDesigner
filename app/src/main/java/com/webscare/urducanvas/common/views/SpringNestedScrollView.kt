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

    /**
     * The content's velocity when the last gesture let go, in scroll pixels per second:
     * positive means the content was moving up (a header would be collapsing), negative
     * down. Zero for a release without a fling. Valid inside [onGestureEnd].
     */
    var releaseVelocityY = 0f
        private set

    /** Which way the last drag moved the content: 1 up (collapsing), -1 down, 0 unknown. */
    var lastDragDirection = 0
        private set

    /** The child list driving the current nested gesture, if any. */
    private var nestedTarget: android.view.View? = null

    override fun onStartNestedScroll(child: android.view.View, target: android.view.View, axes: Int, type: Int): Boolean {
        if (type == androidx.core.view.ViewCompat.TYPE_TOUCH) {
            isGestureInProgress = true
            releaseVelocityY = 0f
            lastDragDirection = 0
            nestedTarget = target
        }
        return super.onStartNestedScroll(child, target, axes, type)
    }

    override fun onNestedPreScroll(target: android.view.View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (type == androidx.core.view.ViewCompat.TYPE_TOUCH && dy != 0) {
            lastDragDirection = if (dy > 0) 1 else -1
        }
        super.onNestedPreScroll(target, dx, dy, consumed, type)
    }

    override fun onNestedPreFling(target: android.view.View, velocityX: Float, velocityY: Float): Boolean {
        // The child reports its fling before it tells us the gesture is over.
        releaseVelocityY = velocityY
        return super.onNestedPreFling(target, velocityX, velocityY)
    }

    override fun onStopNestedScroll(target: android.view.View, type: Int) {
        super.onStopNestedScroll(target, type)
        if (type == androidx.core.view.ViewCompat.TYPE_TOUCH) {
            isGestureInProgress = false
            onGestureEnd?.invoke()
        }
    }

    /** Ends any fling in flight — this view's own, and the child list's if it started one. */
    fun stopFlings() {
        lastFlingVelocity = 0f
        fling(0)
        (nestedTarget as? androidx.recyclerview.widget.RecyclerView)?.stopScroll()
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
            MotionEvent.ACTION_DOWN -> {
                isGestureInProgress = true
                releaseVelocityY = 0f
                lastDragDirection = 0
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
                if (abs(dy) > 1f) lastDragDirection = if (dy > 0) 1 else -1

                val pullUp   = dy < 0 && isAtTop    && !canScrollVertically(-1)
                val pullDown = dy > 0 && isAtBottom && !canScrollVertically(1)

                if (pullUp || pullDown || isBouncing) {
                    applyRubberBand(-dy)
                    return true
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                // Finger velocity is positive downwards; content velocity is the reverse.
                val velocityY = captureVelocityY()
                recycleVelocity()

                val child = scrollChild
                if (child != null && (isBouncing || abs(child.translationY) > 0f)) {
                    springBack(velocityY)
                    endGesture(-velocityY)
                    return true
                }
                // The gesture ends after the parent has had its say, so a listener that
                // wants to take over from the fling this starts can stop it.
                val handled = super.onTouchEvent(ev)
                endGesture(-velocityY)
                return handled
            }
        }

        return super.onTouchEvent(ev)
    }

    private fun endGesture(contentVelocityY: Float) {
        releaseVelocityY = contentVelocityY
        isGestureInProgress = false
        onGestureEnd?.invoke()
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