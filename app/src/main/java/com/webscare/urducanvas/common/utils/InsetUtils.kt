package com.webscare.urducanvas.common.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Edge-to-edge helpers.
 *
 * The window draws under both system bars (see `Base.Theme.UrduPhotoDesigner`),
 * so nothing reserves the status bar for a screen any more — each screen asks
 * for it here. Home is the exception: it paints its green header full-bleed and
 * sizes its own spacer, so it never calls these.
 */
object InsetUtils {

    /**
     * Pads [this] down by the status bar height, keeping whatever padding the
     * layout already declares. Re-applied on every inset pass so rotation,
     * multi-window and gesture-nav changes stay correct.
     */
    fun View.applyStatusBarTopPadding() {
        val basePaddingTop = paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = basePaddingTop + top)
            insets
        }
        ViewCompat.requestApplyInsets(this)
    }

    /**
     * As [applyStatusBarTopPadding], but also keeps the view clear of the
     * navigation bar / gesture handle at the bottom. For screens whose content
     * runs all the way to the bottom edge.
     */
    fun View.applySystemBarsPadding() {
        val basePaddingTop = paddingTop
        val basePaddingBottom = paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                top = basePaddingTop + bars.top,
                bottom = basePaddingBottom + bars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(this)
    }

    private fun View.updatePadding(
        top: Int = paddingTop,
        bottom: Int = paddingBottom
    ) = setPadding(paddingLeft, top, paddingRight, bottom)
}
