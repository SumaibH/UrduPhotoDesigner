package com.webscare.urducanvas.ui.editor.panels.preview

import androidx.fragment.app.Fragment
import com.webscare.urducanvas.R
import com.webscare.urducanvas.data.model.FontEntity

/**
 * The font preview's own download and share.
 *
 * Both font panels — the text panel's shelf and the adjustments rail — already
 * watch `fontDownloadStates`, and both react to a finished download by applying
 * the font and closing the panel. That is right when the user picked a font and
 * is waiting to use it, and wrong when they only pressed Download inside a
 * preview they are still reading. So the preview keeps its own id alongside the
 * panel's, and the panel hands finished downloads to whichever of the two asked
 * for them.
 *
 * @param download the panel's own download call, so analytics and the tile's
 *   progress spinner behave exactly as they do for a tap.
 * @param primaryLabel `preview_add_to_canvas` in the main panels,
 *   `preview_use_on_canvas` in the adjustments panels. Passed as a resource id at
 *   the call site rather than held as a constant here: a `const val` holding an
 *   `R.string` inlines into its callers, and one stale class file then means a
 *   `NoSuchMethodError` at the first long-press rather than a compile error.
 */
class FontPreviewController(
    private val fragment: Fragment,
    private val host: () -> PanelPreviewHost?,
    private val breadcrumb: () -> String,
    private val expanded: () -> Boolean,
    private val primaryLabel: Int,
    private val download: (FontEntity) -> Unit,
    private val onUse: (FontEntity) -> Unit
) {

    /** The font this preview is downloading, if any. */
    var downloadingId: Int? = null
        private set

    /** Share was asked for on a font that wasn't on the device yet. */
    private var shareWhenReady = false

    fun open(font: FontEntity) {
        val panel = host() ?: return
        panel.show(
            asset = PreviewAsset.Font(breadcrumb(), font),
            primaryLabel = fragment.getString(primaryLabel),
            expanded = expanded(),
            onPrimary = { onUse(font) },
            onShare = { share(font) },
            onDownload = { start(font) }
        )
    }

    /** True when [id] is the download this preview started. */
    fun owns(id: Int) = downloadingId == id

    /** The font landed: the preview can now draw in its real face. */
    fun onDownloaded(font: FontEntity) {
        downloadingId = null
        host()?.setDownloading(false)
        if (host()?.isShowing == true) open(font)
        if (shareWhenReady) {
            shareWhenReady = false
            send(font)
        }
    }

    fun onFailed() {
        downloadingId = null
        shareWhenReady = false
        host()?.setDownloading(false)
    }

    fun reset() {
        downloadingId = null
        shareWhenReady = false
    }

    /**
     * A font is only a file once it has been downloaded, so sharing one that
     * hasn't been fetches it first and picks the share back up on the way out.
     */
    private fun share(font: FontEntity) {
        if (send(font)) return
        shareWhenReady = true
        start(font)
    }

    private fun send(font: FontEntity): Boolean {
        val file = PreviewShare.fontFile(font) ?: return false
        val context = fragment.context ?: return false
        return PreviewShare.send(context, file)
    }

    private fun start(font: FontEntity) {
        if (downloadingId == font.id) return
        downloadingId = font.id
        host()?.setDownloading(true)
        download(font)
    }

}
