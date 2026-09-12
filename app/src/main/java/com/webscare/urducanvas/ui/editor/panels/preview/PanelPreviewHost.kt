package com.webscare.urducanvas.ui.editor.panels.preview

import androidx.fragment.app.Fragment
import dagger.hilt.android.EntryPointAccessors

/**
 * Opens the asset preview for one surface — an editor panel, or one of the navigation
 * screens' tile rows.
 *
 * The preview is a bottom sheet of its own. It used to be drawn inside the panel that
 * opened it, which meant the panel had to park its own rows, give away its background so a
 * gap could show through, ask the editor's sheet to grow, and put all of it back
 * afterwards -- and it needed an `onRestore` hook from each panel because the panels drive
 * those same views themselves and any stashed state went stale. A separate sheet needs
 * none of that: whatever is underneath is left exactly as it was.
 *
 * That is also what lets Home, Templates and Popular Fonts host one. The host needs
 * nothing from the editor: no panel root (the old constructor took one and never touched
 * it) and no `CanvasViewModel` — analytics now go straight to the singleton tracker, so a
 * long-press on Home does not construct the whole canvas graph to log one event.
 *
 * [tall] is the one thing the navigation screens ask for that the panels do not. In the
 * editor a panel is already holding the bottom of the screen and the canvas above it has
 * to stay visible; outside it nothing is competing, so the sheet is given more room.
 */
class PanelPreviewHost(
    private val fragment: Fragment,
    private val tall: Boolean = false
) {

    private var sheet: AssetPreviewSheet? = null
    private var showing = false

    /** Hilt has nothing to inject into here — this is a helper, not a fragment. */
    private val analyticsTracker by lazy {
        EntryPointAccessors.fromApplication(
            fragment.requireContext().applicationContext,
            com.webscare.urducanvas.di.AnalyticsEntryPoint::class.java
        ).analyticsTracker()
    }

    val isShowing: Boolean get() = showing

    // ── Open / close ──────────────────────────────────────────────────────────

    /**
     * Opens the preview on [asset]. [primaryLabel] differs by host: the main panels
     * add a new element ("Add to canvas"), the adjustments panels apply to the
     * selected one ("Use on canvas").
     *
     * The primary action closes the preview after it runs; [onShare] and
     * [onDownload] leave it open. Pass null for either to hide that button.
     */
    fun show(
        asset: PreviewAsset,
        primaryLabel: String,
        expanded: Boolean,
        onPrimary: (PreviewAsset) -> Unit,
        onShare: ((PreviewAsset) -> Unit)? = null,
        onDownload: ((PreviewAsset) -> Unit)? = null
    ) {
        val bound = AssetPreviewSheet.Binding(
            asset = asset,
            primaryLabel = primaryLabel,
            expanded = expanded,
            tall = tall,
            onPrimary = { picked ->
                // The commit. Separate from the open below so the pair answers the question
                // the preview was built for: how often looking closer leads to using the
                // asset rather than backing out. Both go through this one place, so every
                // surface that owns a preview is covered without any of them knowing about
                // analytics.
                analyticsTracker.logToolActionPerformed(TOOL_PREVIEW, "use_asset", kindOf(picked))
                onPrimary(picked)
            },
            onShare = onShare,
            onDownload = onDownload,
            // Every way out lands here -- back, the scrim, a drag down, or the primary
            // action -- so this is the only place that has to know the preview is closed.
            onDismissed = { showing = false }
        )

        // Tapping a second tile while the preview is up re-points it instead of stacking a
        // second sheet, which is what the in-panel version did by reusing one view.
        sheet?.takeIf { showing && it.isAdded }?.let {
            it.rebind(bound)
            return
        }

        analyticsTracker.logToolActionPerformed(TOOL_PREVIEW, "open", kindOf(asset))
        showing = true
        sheet = AssetPreviewSheet().apply {
            binding = bound
            show(fragment.childFragmentManager, AssetPreviewSheet.TAG)
        }
    }

    fun hide() {
        if (!showing) return
        showing = false
        sheet?.dismissAllowingStateLoss()
    }

    /** The panel changed height; the preview redraws its asset at the new size. */
    fun onPanelExpandedChanged(expanded: Boolean) {
        sheet?.setExpanded(expanded)
    }

    /** Hands over a rendered asset that was still being drawn when the sheet opened. */
    fun setRenderedBitmap(bitmap: android.graphics.Bitmap?) {
        sheet?.setRenderedBitmap(bitmap)
    }

    /** Progress on the preview's own download button. */
    fun setDownloading(active: Boolean) {
        sheet?.setDownloading(active)
    }

    fun setDownloaded(done: Boolean) {
        sheet?.setDownloaded(done)
    }

    /** Call from the host's `onDestroyView`. */
    fun release() {
        sheet?.let {
            it.binding = null
            if (it.isAdded) it.dismissAllowingStateLoss()
        }
        sheet = null
        showing = false
    }

    /**
     * Short, stable and locale-independent. The asset's breadcrumb would name the category,
     * but it is a translated string and would split one value across every language the app
     * ships in.
     *
     * `Rendered` covers emoji, shapes and style presets, so all three land in one bucket.
     * Which of them it was is already answerable: `tool_panel_opened` names the panel the
     * preview was opened from. Splitting them properly means carrying the kind on
     * [PreviewAsset.Rendered] itself, which is not this change's file to alter.
     */
    private fun kindOf(asset: PreviewAsset): String = when (asset) {
        is PreviewAsset.Font -> "font"
        is PreviewAsset.Picture -> "picture"
        is PreviewAsset.Rendered -> "rendered"
        is PreviewAsset.Artwork -> asset.kind
    }

    companion object {
        /**
         * One tool name for every panel's preview. Which panel it was is already on
         * `tool_panel_opened`, and the preview is the same feature wherever it appears.
         */
        private const val TOOL_PREVIEW = "asset_preview"
    }
}

/**
 * Implemented by the panel fragment that owns a [PanelPreviewHost]. The grids
 * live in child fragments, so the tile that asks for a preview is usually a
 * couple of levels below the panel that shows it.
 */
interface PreviewHostOwner {
    val previewHost: PanelPreviewHost?
}

/** Walks up to the panel hosting this grid. */
fun Fragment.findPreviewHost(): PanelPreviewHost? =
    generateSequence(this as Fragment?) { it.parentFragment }
        .filterIsInstance<PreviewHostOwner>()
        .firstNotNullOfOrNull { it.previewHost }
