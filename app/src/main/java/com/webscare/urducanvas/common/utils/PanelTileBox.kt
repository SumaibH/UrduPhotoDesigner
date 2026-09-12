package com.webscare.urducanvas.common.utils

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Keeps a bottom panel's tile sizes honest across a sheet animation.
 *
 * Every panel sizes its tiles from its own list's height — a collapsed tile is
 * `(availHeight - margins) / spanCount` — and reads that height straight off the
 * RecyclerView at the moment it is asked. `PanelSheetBehavior` drives the sheet with a
 * deliberately bouncy spring (`DAMPING_RATIO_MEDIUM_BOUNCY`), so while the panel is
 * moving, every height read is one the panel is merely passing through.
 *
 * That on its own would be harmless, because the last write would win. What makes it a
 * bug is the ordering at the end: the spring's final frame — and `snapTo(immediate)`,
 * which does the same thing in one step — writes the resting guideline and emits the
 * settled offset SYNCHRONOUSLY, before the layout pass that gives the list its resting
 * height. So the size that sticks was computed from the frame before the last, i.e. from
 * the overshoot. Measured on a Pixel 8 Pro at the user's geometry, a collapse from the
 * expanded panel computed `final=608` while the list still reported its expanded 1889 px,
 * and the strip it landed in was 526 px tall — three rows of 608 px in 526 px of strip is
 * exactly the "tiles drawn on top of each other" the user reported.
 *
 * The spring's own end listener cannot carry the fix: panels learn the slide offset
 * through `MainViewModel.panelSlideOffset`, a StateFlow, and StateFlow conflates equal
 * values — re-emitting the settled offset from the end listener delivers nothing. The
 * authoritative moment is not "the spring stopped" but "the list has been laid out at the
 * height the spring stopped at", which is what this watches.
 *
 * It fires only when the box genuinely changed size, so scrolling and rebinding cost
 * nothing. The re-run it triggers cannot feed back into it — the sheet sizes the list, the
 * tiles do not — so one extra layout pass settles it.
 */
fun RecyclerView.onBoxResized(resync: (RecyclerView) -> Unit): View.OnLayoutChangeListener {
    val listener = View.OnLayoutChangeListener {
            v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        val sizeChanged =
            (right - left) != (oldRight - oldLeft) || (bottom - top) != (oldBottom - oldTop)
        if (sizeChanged) (v as? RecyclerView)?.let(resync)
    }
    addOnLayoutChangeListener(listener)
    return listener
}

/** Undoes [onBoxResized]. Null-tolerant so detach paths can stay one-liners. */
fun RecyclerView.removeBoxResizedWatcher(listener: View.OnLayoutChangeListener?) {
    listener?.let { removeOnLayoutChangeListener(it) }
}
