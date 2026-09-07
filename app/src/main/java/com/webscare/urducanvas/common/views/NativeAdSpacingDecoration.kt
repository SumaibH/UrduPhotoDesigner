package com.webscare.urducanvas.common.views

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * Normalises the vertical spacing around an injected native ad slot so a list
 * keeps the same rhythm whether or not an ad is showing.
 *
 * `AdNativeRecyclerAdapter` hands its ad holder a fixed 8dp top / 16dp bottom
 * margin. In a list of section rows — where every row already carries 18dp of
 * header padding — that reads as 8dp above the ad and 34dp below it, which is
 * the lopsided gap around the ad card.
 *
 * Rather than assume those numbers, this reads the margins the adapter actually
 * set and emits the delta needed to reach the target. Item offsets may be
 * negative, so the correction works in both directions and keeps working if the
 * ads library changes its own values.
 *
 * Screens that flip the same RecyclerView between a section list and a grid get
 * the right spacing either way — the layout manager in effect decides, so the
 * decoration can be installed once and left alone across adapter swaps.
 */
class NativeAdSpacingDecoration(context: Context) : RecyclerView.ItemDecoration() {

    /** Rows own the gap above the ad; the next row's header supplies the one below. */
    private val rowsTop = dp(context, 18f)
    private val rowsBottom = 0

    /** Side inset that lines the ad card up with the section titles above it. */
    private val rowsSide = dp(context, 19f)

    /** Grid cells carry no leading padding of their own, so the ad owns both sides. */
    private val gridTop = dp(context, 12f)
    private val gridBottom = dp(context, 12f)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (parent.getChildViewHolder(view)?.itemViewType != TYPE_NATIVE_AD) return
        val lp = view.layoutParams as? RecyclerView.LayoutParams ?: return

        val lm = parent.layoutManager
        val isGrid = lm is GridLayoutManager || lm is StaggeredGridLayoutManager

        outRect.top = (if (isGrid) gridTop else rowsTop) - lp.topMargin
        outRect.bottom = (if (isGrid) gridBottom else rowsBottom) - lp.bottomMargin

        // Grid cells set their own horizontal rhythm; only the row list needs the
        // ad card pulled out to the section-title margin.
        if (!isGrid) {
            outRect.left = rowsSide - lp.marginStart
            outRect.right = rowsSide - lp.marginEnd
        }
    }

    private fun dp(context: Context, value: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics
    ).toInt()

    private companion object {
        /** `AdNativeRecyclerAdapter.TYPE_AD` — the ads library's ad view type. */
        const val TYPE_NATIVE_AD = -99
    }
}
