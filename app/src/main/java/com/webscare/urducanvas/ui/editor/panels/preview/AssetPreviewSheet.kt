package com.webscare.urducanvas.ui.editor.panels.preview

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.webscare.urducanvas.R

/**
 * The asset preview, as a bottom sheet of its own.
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
        val onPrimary: (PreviewAsset) -> Unit,
        val onShare: ((PreviewAsset) -> Unit)?,
        val onDownload: ((PreviewAsset) -> Unit)?,
        val onDismissed: () -> Unit
    )

    var binding: Binding? = null

    private var preview: AssetPreviewView? = null

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
        // One listener only — a dialog keeps the last one set, so these cannot be
        // registered separately.
        dialog.setOnShowListener {
            // Open at full height. A peek would show the chips and hide the artwork, which
            // is the half of the preview worth opening it for.
            dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            dialog.behavior.skipCollapsed = true
            forceImmersiveMode()
        }
        // The card draws its own rounded surface; the window behind it must not add a
        // square one, or the corners are filled in.
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

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
        val d = resources.displayMetrics.density
        val view = AssetPreviewView(requireContext()).apply {
            setBackgroundResource(R.drawable.bg_preview_sheet)
            clipToOutline = true
            isClickable = true
            isFocusable = true
        }
        preview = view

        // Held off every edge so it reads as a card resting over the editor rather than a
        // page fixed to the bottom of the screen. The theme is edge-to-edge and does not
        // pad for system bars, so the bottom gap has to clear the navigation itself.
        val container = android.widget.FrameLayout(requireContext()).apply {
            val side = (SIDE_INSET_DP * d).toInt()
            val bottom = (BOTTOM_INSET_DP * d).toInt()
            addView(
                view,
                android.widget.FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(side, 0, side, bottom) }
            )
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                val nav = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                ).bottom
                v.setPadding(0, 0, 0, nav)
                insets
            }
        }

        if (bound == null) {
            // Recreated with nothing to show or call. Leave rather than pretend.
            container.post { dismissAllowingStateLoss() }
            return container
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
        return container
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

    fun setExpanded(expanded: Boolean) {
        preview?.setExpanded(expanded)
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

        /** How far the card is held off the screen edges. */
        private const val SIDE_INSET_DP = 10f
        private const val BOTTOM_INSET_DP = 8f
    }
}
