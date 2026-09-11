package com.webscare.urducanvas.ui.editor.panels.preview

import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintHelper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.webscare.urducanvas.ui.editor.EditorFragment

/**
 * Holds the asset preview inside one editor panel.
 *
 * The preview is not a dialog and not a second sheet: the panel pushes it over its
 * own content, the way the `[← Language]` breadcrumb already works. This class owns
 * the mechanics of that — parking the panel's rows out of the way, sliding the
 * preview in, taking the back press, and asking the sheet for more room when the
 * panel is too short — so a panel only has to say what the asset is and what its
 * actions do.
 *
 * [topAnchorId] and [startAnchorId] name the views the preview lays out beside
 * rather than over: a panel's drag handle stays reachable so its own drag and close
 * still work, and an adjustments rail stays put so the preview covers the grid
 * alone. Both default to the panel edges.
 */
class PanelPreviewHost(
    private val fragment: Fragment,
    private val panelRoot: ConstraintLayout,
    private val topAnchorId: Int = ConstraintLayout.LayoutParams.PARENT_ID,
    private val startAnchorId: Int = ConstraintLayout.LayoutParams.PARENT_ID
) {

    private var view: AssetPreviewView? = null

    /** Panel views taken out of the accessibility tree while the preview is up. */
    private val muted = mutableListOf<View>()

    private var showing = false
    private var holdingSheet = false

    /**
     * Reached through the view model because this is a helper, not a fragment, so Hilt has
     * nothing to inject into. Activity-scoped, which is the same instance every panel holds.
     */
    private val canvasViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(
            fragment.requireActivity()
        )[com.webscare.urducanvas.common.canvas.CanvasViewModel::class.java]
    }

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = hide()
    }

    init {
        // Added after the editor's own callback, so it wins while it is enabled.
        fragment.requireActivity().onBackPressedDispatcher
            .addCallback(fragment.viewLifecycleOwner, backCallback)
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
        val preview = view ?: AssetPreviewView(panelRoot.context).also { created ->
            created.id = View.generateViewId()
            // The panels' own selection toolbar sits at 8dp, so clear it.
            created.elevation = 10f * panelRoot.resources.displayMetrics.density
            // Opaque and on top is what hides the grid; clickable is what stops a
            // tap landing on the tile still sitting underneath it.
            created.isClickable = true
            created.isFocusable = true
            panelRoot.addView(created, layoutParams())
            view = created
        }

        preview.onBack = { hide() }
        preview.onPrimaryAction = { picked ->
            // The commit. Separate from the open above so the pair answers the question the
            // preview was built for: how often looking closer leads to using the asset
            // rather than backing out. Both go through this one place, so all seven panels
            // that own a preview are covered without any of them knowing about analytics.
            canvasViewModel.logToolAction(TOOL_PREVIEW, "use_asset", kindOf(picked))
            onPrimary(picked); hide()
        }
        preview.onShare = onShare
        preview.onDownload = onDownload
        preview.show(asset, expanded, primaryLabel)

        if (showing) return
        showing = true
        canvasViewModel.logToolAction(TOOL_PREVIEW, "open", kindOf(asset))
        mute()
        backCallback.isEnabled = true
        if (!expanded) {
            holdingSheet = editorSheet()?.holdAtHeight(previewHeightPx()) == true
        }

        preview.isVisible = true
        preview.translationX = panelRoot.width.toFloat()
        preview.animate()
            .translationX(0f)
            .setDuration(SLIDE_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun hide() {
        if (!showing) return
        showing = false
        backCallback.isEnabled = false

        unmute()
        releaseSheet()

        val preview = view ?: return
        preview.animate()
            .translationX(panelRoot.width.toFloat())
            .setDuration(SLIDE_MS)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                preview.isVisible = false
                preview.translationX = 0f
            }
            .start()
    }

    /** The panel changed height; the preview redraws its asset at the new size. */
    fun onPanelExpandedChanged(expanded: Boolean) {
        view?.setExpanded(expanded)
        // Expanding moved the sheet on its own terms, so there is nothing held to
        // put back — and collapsing already took it past where it was held.
        if (holdingSheet) holdingSheet = false
    }

    /** Progress on the preview's own download button. */
    fun setDownloading(active: Boolean) = view?.setDownloading(active)

    fun setDownloaded(done: Boolean) = view?.setDownloaded(done)

    /** Call from the host's `onDestroyView`. */
    fun release() {
        backCallback.isEnabled = false
        releaseSheet()
        unmute()
        view?.let {
            it.animate().cancel()
            panelRoot.removeView(it)
        }
        view = null
        showing = false
    }

    // ── Panel plumbing ────────────────────────────────────────────────────────

    private fun layoutParams() = ConstraintLayout.LayoutParams(0, 0).apply {
        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        if (topAnchorId != ConstraintLayout.LayoutParams.PARENT_ID) {
            topToTop = ConstraintLayout.LayoutParams.UNSET
            topToBottom = topAnchorId
        }
        if (startAnchorId != ConstraintLayout.LayoutParams.PARENT_ID) {
            startToStart = ConstraintLayout.LayoutParams.UNSET
            startToEnd = startAnchorId
        }
    }

    /**
     * Takes the covered rows out of the accessibility tree.
     *
     * Their visibility is deliberately left alone. The panels drive it themselves —
     * the collapsed/expanded header morph flips these same views, every frame of a
     * slide — so anything this stashed to restore later would be stale by the time
     * the preview closed. An opaque view on top already hides them and eats their
     * touches; this is only about what a screen reader walks into behind it.
     */
    private fun mute() {
        muted.clear()
        for (i in 0 until panelRoot.childCount) {
            val child = panelRoot.getChildAt(i)
            if (child === view) continue
            if (child.id != View.NO_ID && (child.id == topAnchorId || child.id == startAnchorId)) continue
            if (child is Guideline || child is ConstraintHelper) continue
            muted += child
            child.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }
    }

    private fun unmute() {
        muted.forEach { it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO }
        muted.clear()
    }

    private fun releaseSheet() {
        if (!holdingSheet) return
        holdingSheet = false
        editorSheet()?.releaseHeight()
    }

    private fun editorSheet() = generateSequence(fragment.parentFragment) { it.parentFragment }
        .filterIsInstance<EditorFragment>()
        .firstOrNull()
        ?.panelSheetBehavior()

    private fun previewHeightPx() =
        (PREVIEW_PANEL_DP * panelRoot.resources.displayMetrics.density).toInt()

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
    }

    companion object {
        private const val SLIDE_MS = 220L

        /**
         * One tool name for every panel's preview. Which panel it was is already on
         * `tool_panel_opened`, and the preview is the same feature wherever it appears.
         */
        private const val TOOL_PREVIEW = "asset_preview"

        /**
         * How tall a panel has to be for the preview to read: the well and paper
         * need to be worth looking at above the chips, the sample field and the
         * actions row. Collapsed panels are well short of it.
         */
        private const val PREVIEW_PANEL_DP = 340
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
