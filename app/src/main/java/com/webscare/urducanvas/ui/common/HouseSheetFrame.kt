package com.webscare.urducanvas.ui.common

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.webscare.urducanvas.common.utils.Utils.keepBelowStatusBar

/**
 * The app's bottom-sheet frame, as the Create Canvas sheet defines it.
 *
 * Full width and full height, not fit-to-contents, opened at a fraction of the screen
 * and draggable from there to just below the status bar. Every sheet that wants that
 * shape was writing the same dozen lines into its own `onStart`, and each copy had
 * drifted a little — one set the state before the ratio, one forgot `isDraggable`, one
 * left a dead `background` assignment immediately overwritten by the next line.
 *
 * This owns the frame and nothing else. The header, the title, the close control and the
 * content are each sheet's own business; a sheet adopting this keeps the look it had.
 *
 * ## Why the content has to be wrapped
 *
 * A full-height frame opened part-way up leaves the rest of itself below the bottom of
 * the display. Content laid out to match_parent therefore puts its bottom — the action
 * row, the last list items, whatever a percent guideline resolves against — down there
 * where nobody can reach it. So the content is wrapped and the wrapper padded by exactly
 * the overhang, which leaves the content measuring the part of the sheet that is
 * actually on screen, and hands back the space as the sheet is dragged up.
 *
 * The padding is recomputed on every frame the sheet is drawn rather than from a layout
 * listener or [BottomSheetBehavior.BottomSheetCallback]. The frame is moved with
 * `offsetTopAndBottom`, which changes where it is drawn without laying anything out, so
 * a layout listener never hears about it; and a state set before the first layout settles
 * with no animation, so the first resting position never comes through `onSlide` either.
 * Drawing is the one thing that does happen every time it moves.
 */
class HouseSheetFrame(private val fragment: DialogFragment) {

    private var frame: View? = null
    private var wrapper: FrameLayout? = null

    /** Kept so [setOpenRatio] can move the stop without the caller repeating itself. */
    private var openRatio = DEFAULT_OPEN_RATIO

    /**
     * Puts [content] inside the wrapper this frame pads, and returns what the fragment
     * should hand back from `onCreateView`.
     */
    fun wrap(content: View): View {
        val box = FrameLayout(content.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(
                content,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        wrapper = box
        return box
    }

    /**
     * Applies the frame. Call from `onStart`, after `super`.
     *
     * [openRatio] is how much of the screen the sheet takes when it appears — the one
     * thing that legitimately differs between sheets, since a four-item chooser and a
     * full font library do not want the same opening height.
     */
    fun attach(openRatio: Float = DEFAULT_OPEN_RATIO) {
        this.openRatio = openRatio
        val sheet = fragment.dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        frame = sheet

        // The content paints the rounded surface. A second one behind it would square
        // the corners back off.
        sheet.setBackgroundResource(android.R.color.transparent)
        sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        BottomSheetBehavior.from(sheet).apply {
            isFitToContents = false
            skipCollapsed = true
            isDraggable = true
            // The ratio goes on before the state, not after. Settling against the old
            // stop and then moving it parks the sheet at a height nothing asked for.
            halfExpandedRatio = openRatio
            state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }

        // Owns expandedOffset — a full-height sheet without it draws its own header
        // behind the clock.
        sheet.keepBelowStatusBar()

        sheet.viewTreeObserver.addOnPreDrawListener(visibleHeightWatcher)
    }

    /** Call from `onStop`, before `super`. */
    fun detach() {
        frame?.let {
            if (it.viewTreeObserver.isAlive) {
                it.viewTreeObserver.removeOnPreDrawListener(visibleHeightWatcher)
            }
        }
        frame = null
    }

    /** Call from `onDestroyView`. */
    fun release() {
        detach()
        wrapper = null
    }

    /**
     * Moves the opening stop while the sheet is up.
     *
     * Only re-settles a sheet still sitting on that stop. Someone who has dragged it up
     * has said what they want it to be, and a change behind the scrim is not a reason to
     * take it back.
     */
    fun setOpenRatio(ratio: Float) {
        if (openRatio == ratio) return
        openRatio = ratio
        val sheet = frame ?: return
        val behavior = runCatching { BottomSheetBehavior.from(sheet) }.getOrNull() ?: return
        val settled = behavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED
        behavior.halfExpandedRatio = ratio
        if (settled) behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    /**
     * Converts a content height in dp into the fraction of the screen that shows it.
     *
     * Sheets that know how tall they want to open — rather than what fraction they want
     * to take — express it this way, so the sheet keeps its size across displays instead
     * of growing with them.
     */
    fun ratioForHeight(dp: Float, min: Float = MIN_RATIO, max: Float = MAX_RATIO): Float {
        val metrics = fragment.resources.displayMetrics
        val screenDp = metrics.heightPixels / metrics.density
        if (screenDp <= 0f) return min
        return (dp / screenDp).coerceIn(min, max)
    }

    private fun applyVisibleHeight(): Boolean {
        val sheet = frame ?: return false
        val box = wrapper ?: return false
        val parentHeight = (sheet.parent as? View)?.height ?: return false
        if (parentHeight <= 0 || sheet.height <= 0) return false
        val below = (sheet.top + sheet.height - parentHeight).coerceAtLeast(0)
        val bottom = below + systemBarInset()
        if (box.paddingBottom == bottom) return false
        box.setPadding(0, 0, 0, bottom)
        return true
    }

    /**
     * The navigation bar, read off the host activity rather than this window.
     *
     * These sheets run edge to edge and several of them hide the navigation for
     * themselves, so their own insets report nothing to clear while the bar is still
     * there. The activity's window carries the real value.
     */
    private fun systemBarInset(): Int =
        fragment.activity?.window?.decorView
            ?.let { ViewCompat.getRootWindowInsets(it) }
            ?.getInsets(WindowInsetsCompat.Type.systemBars())
            ?.bottom ?: 0

    /**
     * Cheap in the ordinary case: the padding only changes while the sheet is moving,
     * and an unchanged value costs a comparison. Cancelling the draw on a change is what
     * keeps the content from being painted a frame behind the sheet.
     */
    private val visibleHeightWatcher =
        ViewTreeObserver.OnPreDrawListener { !applyVisibleHeight() }

    companion object {
        /** What the Create Canvas sheet opens at. */
        const val DEFAULT_OPEN_RATIO = 0.75f

        /** Below this it stops reading as a sheet; above it there is nothing left to drag. */
        const val MIN_RATIO = 0.45f
        const val MAX_RATIO = 0.9f
    }
}
