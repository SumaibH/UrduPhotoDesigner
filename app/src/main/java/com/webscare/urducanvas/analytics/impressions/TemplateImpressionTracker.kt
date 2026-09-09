package com.webscare.urducanvas.analytics.impressions

import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.webscare.urducanvas.R
import com.webscare.urducanvas.analytics.AnalyticsTracker
import com.webscare.urducanvas.data.model.TemplateEntity

/**
 * Reports a `template_impression` the first time a template card is actually looked at.
 *
 * "Looked at" means at least [VISIBLE_FRACTION] of the card is on screen. Binding is not
 * the same thing and must not be used as a stand-in: a RecyclerView binds rows slightly
 * off screen and rebinds them every time they are recycled, so counting binds would report
 * impressions nobody saw and report the same card repeatedly on a scroll back and forth.
 * That inflates the denominator of every click-through rate computed from these events,
 * which is worse than having no impression metric at all.
 *
 * One tracker per screen, shared across however many lists that screen shows. Each template
 * is reported at most once per tracker, so a card seen in two rows of the same screen counts
 * once, and returning to the screen later starts a fresh tracker and counts again — an
 * impression per visit rather than per scroll.
 *
 * Home nests horizontal category rows inside a vertical list, so a card can come into view
 * because either axis moved. Rather than track both, this listens once at the view-tree
 * level, which fires for any scroll anywhere in the window, and re-walks the screen.
 */
class TemplateImpressionTracker(
    private val analyticsTracker: AnalyticsTracker,
    private val placement: String
) {

    private val reportedTemplateIds = mutableSetOf<Int>()
    private var root: View? = null

    private var observedTree: ViewTreeObserver? = null
    private var lastSweepUptimeMs = 0L

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener { sweepThrottled() }
    private val drawListener = ViewTreeObserver.OnGlobalLayoutListener { sweepThrottled() }

    /**
     * Starts watching everything under [root] — pass the fragment's root view.
     *
     * Deliberately a view-tree walk rather than a per-list one. A screen can show templates
     * in several places at once, and Home puts them in horizontal lists nested inside a
     * vertical one, so there is no single adapter whose positions describe what is on
     * screen. Cards identify themselves through the `R.id.tag_template` tag their adapter
     * sets at bind time; everything else in the tree — ad slots, headers, shimmer
     * placeholders — has no tag and is skipped, so this needs no knowledge of them.
     */
    fun start(root: View) {
        this.root = root

        if (observedTree == null) {
            root.viewTreeObserver.let { tree ->
                observedTree = tree
                tree.addOnScrollChangedListener(scrollListener)
                tree.addOnGlobalLayoutListener(drawListener)
            }
        }
        // The first screenful never scrolls, so it would otherwise go unreported.
        root.post { sweep() }
    }

    /** Call from `onDestroyView`. Leaving the listeners attached leaks the whole screen. */
    fun stop() {
        observedTree?.takeIf { it.isAlive }?.let { tree ->
            tree.removeOnScrollChangedListener(scrollListener)
            tree.removeOnGlobalLayoutListener(drawListener)
        }
        observedTree = null
        root = null
        reportedTemplateIds.clear()
    }

    private fun sweepThrottled() {
        // Scroll callbacks arrive per frame; the sweep itself only walks the visible
        // children, but there is no reason to run it sixty times a second.
        val now = SystemClock.uptimeMillis()
        if (now - lastSweepUptimeMs < SWEEP_INTERVAL_MS) return
        lastSweepUptimeMs = now
        sweep()
    }

    private fun sweep() {
        root?.let { visit(it) }
    }

    private fun visit(view: View) {
        // An invisible subtree cannot contain anything the user is looking at, so pruning
        // here is what keeps the walk cheap on a screen as busy as Home.
        if (!view.isShown) return

        val template = view.getTag(R.id.tag_template) as? TemplateEntity
        if (template != null) {
            if (visibleFractionOf(view) >= VISIBLE_FRACTION && reportedTemplateIds.add(template.id)) {
                analyticsTracker.logTemplateImpression(
                    templateId = template.id,
                    name = template.template_name,
                    category = template.category,
                    isPremium = template.is_premium,
                    placement = placement
                )
            }
            return
        }

        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                visit(view.getChildAt(index) ?: continue)
            }
        }
    }

    /**
     * How much of [view] the user can actually see, as a fraction of its area.
     *
     * `getGlobalVisibleRect` already accounts for every ancestor's clipping, so a card
     * half out of its row, or in a row half off the bottom of the screen, both come back
     * correctly reduced without this having to reason about the nesting.
     */
    private fun visibleFractionOf(view: View): Float {
        if (!view.isShown) return 0f
        val area = view.width.toLong() * view.height.toLong()
        if (area <= 0L) return 0f

        val visible = Rect()
        if (!view.getGlobalVisibleRect(visible)) return 0f
        val visibleArea = visible.width().toLong() * visible.height().toLong()
        return (visibleArea.toFloat() / area.toFloat()).coerceIn(0f, 1f)
    }

    private companion object {
        const val VISIBLE_FRACTION = 0.5f
        const val SWEEP_INTERVAL_MS = 200L
    }
}
