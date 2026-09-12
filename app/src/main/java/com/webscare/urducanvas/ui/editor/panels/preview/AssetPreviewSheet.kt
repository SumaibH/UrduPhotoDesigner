package com.webscare.urducanvas.ui.editor.panels.preview

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.view.WindowInsetsController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.utils.Utils.keepBelowStatusBar

/**
 * The asset preview, as a bottom sheet of its own.
 *
 * Built to the same shape as the Create Canvas sheet — full width, a full-height frame
 * that is not fit-to-contents, opened half expanded and draggable the rest of the way,
 * with the rounded [R.drawable.bottom_sheet_bg] surface and the status-bar guard. It
 * used to force STATE_EXPANDED on a wrap-content card held off every edge, which is a
 * modal dialog wearing a sheet's clothes: nothing to drag and nowhere to drag it to.
 *
 * It used to be drawn inside whichever panel opened it, which meant the panel had to park
 * its own rows and give its background away to make room, and put them all back
 * afterwards. A sheet over the editor needs none of that: the panel underneath is simply
 * left alone, and the machinery that kept the two in step stops existing.
 *
 * The view is built in code rather than inflated because [AssetPreviewView] is a custom
 * view that draws its own contents; there is no layout to give it.
 *
 * Callbacks are handed over by [PanelPreviewHost] just before the sheet is shown, and are
 * deliberately not persisted. If the process is recreated while the preview is open there
 * is nothing to call back into, so the sheet dismisses itself rather than coming back as a
 * card whose buttons do nothing.
 */
class AssetPreviewSheet : BottomSheetDialogFragment() {

    /** What the preview shows and what its buttons do, for one showing. */
    class Binding(
        val asset: PreviewAsset,
        val primaryLabel: String,
        val expanded: Boolean,
        /** Outside the editor nothing competes for the screen, so the card is given more of it. */
        val tall: Boolean,
        val onPrimary: (PreviewAsset) -> Unit,
        val onShare: ((PreviewAsset) -> Unit)?,
        val onDownload: ((PreviewAsset) -> Unit)?,
        val onDismissed: () -> Unit
    )

    var binding: Binding? = null

    private var preview: AssetPreviewView? = null

    /** The sheet frame itself, so the slide can be measured against it. */
    private var sheetFrame: View? = null

    /**
     * Tracks the panel's own height while the sheet is up, so a preview opened over a
     * collapsed panel and then expanded grows with it. [Binding.expanded] is the value
     * at the moment the sheet was shown and does not move.
     */
    private var expandedNow = false

    /** Survives a rebind while the sheet is up, so re-showing the same asset is cheap. */
    private var pendingDownloading: Boolean? = null
    private var pendingDownloaded: Boolean? = null

    /** Set when a rendered asset arrives before the view exists to show it. */
    private var pendingRenderedBitmap: android.graphics.Bitmap? = null

    // The app's existing modal-sheet theme: transparent surface and edge-to-edge, which is
    // what lets the card below draw its own rounded shape and sit clear of the edges.
    override fun getTheme(): Int = R.style.CustomBottomSheetDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        // The sheet draws its own rounded surface; the window behind it must not add a
        // square one, or the corners are filled in.
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    /**
     * The Create Canvas sheet's structure, and only its structure: a full-height frame
     * that is not fit-to-contents, so there is somewhere above the opening height to
     * drag to, and a half-expanded stop that decides how much of the screen it takes
     * when it appears. The header, the title and the content are the preview's own.
     */
    override fun onStart() {
        super.onStart()

        val sheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return
        sheetFrame = sheet

        // The preview paints bottom_sheet_bg itself. A second surface behind it would
        // square the rounded top back off.
        sheet.setBackgroundResource(android.R.color.transparent)
        sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        BottomSheetBehavior.from(sheet).apply {
            isFitToContents = false
            skipCollapsed = true
            isDraggable = true
            // The ratio goes on before the state, not after. Settling against the old
            // stop and then moving it leaves the sheet parked at a height nothing
            // asked for, until something else happens to make it settle again.
            halfExpandedRatio = openRatio()
            state = BottomSheetBehavior.STATE_HALF_EXPANDED
            removeBottomSheetCallback(slideCallback)
            addBottomSheetCallback(slideCallback)
        }

        // Owns expandedOffset — this sheet is full height, so without it the top of the
        // sheet sits behind the clock.
        sheet.keepBelowStatusBar()

        // The sheet is moved with offsetTopAndBottom, which changes where it is drawn
        // without laying anything out — so neither a layout listener nor, on the first
        // settle, the slide callback ever reports the position it came to rest at. Every
        // move does redraw it, so the check rides along with the draw instead.
        sheet.viewTreeObserver.addOnPreDrawListener(visibleHeightWatcher)

        forceImmersiveMode()
    }

    override fun onStop() {
        sheetFrame?.let {
            if (it.viewTreeObserver.isAlive) {
                it.viewTreeObserver.removeOnPreDrawListener(visibleHeightWatcher)
            }
            runCatching { BottomSheetBehavior.from(it) }
                .getOrNull()?.removeBottomSheetCallback(slideCallback)
        }
        sheetFrame = null
        super.onStop()
    }

    /**
     * How much of the screen the sheet takes when it opens.
     *
     * This is where the preview's three sizes now live. They used to be three fixed
     * well heights, which a half-expanded sheet makes meaningless: the frame's height
     * is what decides how tall the sheet opens, and a fixed well inside it would only
     * have decided where the dead space went. So the well flexes instead, and the
     * heights it used to take are converted here into the fraction of the screen that
     * leaves the artwork the same size it was before — the editor's collapsed panel
     * shortest, its expanded panel taller, and the navigation screens' `tall` preview
     * taller still, which is the distinction that had to survive.
     *
     * Expressed in dp rather than as three flat fractions so the artwork keeps its
     * size across screens instead of growing with the display.
     */
    private fun openRatio(): Float {
        val wellDp = when {
            binding?.tall == true -> WELL_TALL_DP
            expandedNow -> WELL_EXPANDED_DP
            else -> WELL_COLLAPSED_DP
        }
        val screenDp = resources.displayMetrics.heightPixels / resources.displayMetrics.density
        if (screenDp <= 0f) return MIN_RATIO
        return ((wellDp + CHROME_DP) / screenDp).coerceIn(MIN_RATIO, MAX_RATIO)
    }

    /**
     * Keeps the preview's own column as tall as the part of the sheet that is on screen.
     *
     * The frame is full height and only its top is visible until it is dragged all the
     * way up, so the rest hangs below the display. Left alone, a match_parent column
     * would put its buttons down there. Padding the bottom by exactly the overhang puts
     * them back on the last visible row, and the well — the one weighted child — takes
     * whatever the drag has just handed over.
     */
    private fun applyVisibleHeight(): Boolean {
        val sheet = sheetFrame ?: return false
        val view = preview ?: return false
        val parentHeight = (sheet.parent as? View)?.height ?: return false
        if (parentHeight <= 0 || sheet.height <= 0) return false
        val below = (sheet.top + sheet.height - parentHeight).coerceAtLeast(0)
        val bottom = below + bottomInset()
        if (view.paddingBottom == bottom) return false
        view.setPadding(0, 0, 0, bottom)
        return true
    }

    /**
     * The navigation bar, read off the host activity rather than this window.
     *
     * The sheet runs edge to edge and hides the navigation for itself, so its own
     * insets report nothing to clear — while the bar is still there the moment the
     * sheet goes away. The activity's window carries the real value.
     */
    private fun bottomInset(): Int =
        activity?.window?.decorView
            ?.let { androidx.core.view.ViewCompat.getRootWindowInsets(it) }
            ?.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            ?.bottom ?: 0

    private val slideCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            applyVisibleHeight()
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            applyVisibleHeight()
        }
    }

    /**
     * Re-measures against the sheet's current position on every frame it is drawn.
     *
     * Cheap in the ordinary case: the padding only changes while the sheet is actually
     * moving, and an unchanged value costs a comparison. Cancelling the draw on a change
     * is what keeps the content from being painted one frame behind the sheet.
     */
    private val visibleHeightWatcher =
        ViewTreeObserver.OnPreDrawListener { !applyVisibleHeight() }

    /**
     * Keeps the navigation bar hidden for this window, matching the editor underneath.
     * Re-applied whenever the system shows it again, which it does on any transient
     * swipe.
     */
    private fun forceImmersiveMode() {
        val window = dialog?.window ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.apply {
                hide(WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val bound = binding
        expandedNow = bound?.expanded ?: false

        // Full width and full height, meeting the bottom of the screen with only its top
        // corners rounded — the sheet surface the rest of the app uses. It used to be a
        // card held off every edge, which is most of why the preview read as a dialog.
        val view = AssetPreviewView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundResource(R.drawable.bottom_sheet_bg)
            clipToOutline = true
            isClickable = true
            isFocusable = true
        }
        preview = view

        if (bound == null) {
            // Recreated with nothing to show or call. Leave rather than pretend.
            view.post { dismissAllowingStateLoss() }
            return view
        }

        view.onBack = { dismissAllowingStateLoss() }
        view.onPrimaryAction = { picked ->
            bound.onPrimary(picked)
            dismissAllowingStateLoss()
        }
        view.onShare = bound.onShare
        view.onDownload = bound.onDownload
        view.show(bound.asset, bound.expanded, bound.primaryLabel)
        pendingDownloading?.let(view::setDownloading)
        pendingDownloaded?.let(view::setDownloaded)
        pendingRenderedBitmap?.let(view::setRenderedBitmap)
        return view
    }

    /** Re-points an already-open sheet at a new asset without closing and reopening it. */
    fun rebind(bound: Binding) {
        binding = bound
        // Belongs to the asset that was showing, not this one.
        pendingRenderedBitmap = null
        val view = preview ?: return
        view.onPrimaryAction = { picked ->
            bound.onPrimary(picked)
            dismissAllowingStateLoss()
        }
        view.onShare = bound.onShare
        view.onDownload = bound.onDownload
        view.show(bound.asset, bound.expanded, bound.primaryLabel)
    }

    /**
     * The panel underneath changed height. The content redraws at the new size, and the
     * sheet's opening stop moves with it — but only while the sheet is still sitting on
     * that stop. Someone who has dragged the preview up to full height has said what
     * they want it to be, and a panel behind the scrim is not a reason to take it back.
     */
    fun setExpanded(expanded: Boolean) {
        preview?.setExpanded(expanded)
        if (expandedNow == expanded) return
        expandedNow = expanded
        val sheet = sheetFrame ?: return
        val behavior = runCatching { BottomSheetBehavior.from(sheet) }.getOrNull() ?: return
        val settled = behavior.state == BottomSheetBehavior.STATE_HALF_EXPANDED
        behavior.halfExpandedRatio = openRatio()
        if (settled) behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    /**
     * Hands over a rendered asset that was still being drawn when the sheet opened.
     * Held if the view is not up yet, the same way the download state is.
     */
    fun setRenderedBitmap(bitmap: android.graphics.Bitmap?) {
        pendingRenderedBitmap = bitmap
        preview?.setRenderedBitmap(bitmap)
    }

    fun setDownloading(active: Boolean) {
        pendingDownloading = active
        preview?.setDownloading(active)
    }

    fun setDownloaded(done: Boolean) {
        pendingDownloaded = done
        preview?.setDownloaded(done)
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        // Covers every way out -- the back button, the scrim, a drag down, and the
        // primary action -- so the host never has to guess whether it is still open.
        binding?.onDismissed?.invoke()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        preview?.onBack = null
        preview?.onPrimaryAction = null
        preview?.onShare = null
        preview?.onDownload = null
        preview = null
    }

    companion object {
        const val TAG = "asset_preview_sheet"

        /**
         * How tall the artwork should be when the sheet opens, for each of the three
         * surfaces that show a preview. These were the well's own fixed heights before
         * the sheet became draggable; they are the same numbers, now read as the target
         * the opening ratio is solved for.
         */
        private const val WELL_COLLAPSED_DP = 210f
        private const val WELL_EXPANDED_DP = 320f

        /**
         * Off the editor. Taller than the expanded panel because nothing is competing for
         * the screen there — no panel below and no canvas above to keep in view.
         */
        private const val WELL_TALL_DP = 420f

        /**
         * Everything in the sheet that is not the well: the header and its rule, the
         * body's padding, the detail pills, the sample field and the action row. Taken
         * as always present — the field only shows for fonts, so a picture opens with a
         * little more artwork than it asked for rather than a gap under its buttons.
         */
        private const val CHROME_DP = 200f

        /** Below this it stops reading as a sheet; above it there is nothing left to drag. */
        private const val MIN_RATIO = 0.45f
        private const val MAX_RATIO = 0.9f
    }
}
