package com.webscare.urducanvas.ui.navigation.preview

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.utils.Constants

import com.webscare.urducanvas.data.model.ExportResult
import com.webscare.urducanvas.data.model.FontEntity
import com.webscare.urducanvas.data.model.TemplateEntity
import com.webscare.urducanvas.ui.editor.panels.preview.PanelPreviewHost
import com.webscare.urducanvas.ui.editor.panels.preview.PreviewAsset
import com.webscare.urducanvas.ui.editor.panels.preview.PreviewShare
import kotlinx.coroutines.launch
import java.io.File

/**
 * What a navigation screen's tile does when it is long-pressed.
 *
 * The editor's equivalents live in `PreviewRequests.kt` and differ in two ways that matter.
 *
 * First, the primary action. "Add to canvas" is the only sensible verb in a panel, where
 * there is a canvas open and the whole point of the tile is to put something on it. On
 * Home there is no canvas, so each surface names what tapping the tile already does —
 * opening a saved project, opening a template, using a font — and the preview's button
 * runs that same path. A preview whose button did something the tile does not would be a
 * second way to do a thing, which is not what this gesture is for.
 *
 * Second, the host is asked for a taller sheet. In the editor the panel is holding the
 * bottom of the screen and the canvas above it has to stay visible; here nothing is.
 *
 * Note what is deliberately absent: no eye button anywhere. The eye exists only for the
 * editor's expanded panels, and no navigation screen has an expanded state, so every one
 * of them is permanently the collapsed case — long-press, and nothing drawn on the tile.
 */

/**
 * A saved project — the row Home calls Recent Projects.
 *
 * The thumbnail on the tile is the export itself, so the preview is the one place the user
 * can see what they made at a size worth looking at without reopening the editor. Sharing
 * it is free: the PNG is already on the device, which is the case [PreviewShare] handles
 * without fetching anything.
 *
 * The picture is the part that can be missing. A project whose export was deleted — or one
 * only ever saved, never exported — still has its `.json`, which is what actually reopens
 * it, and that is the majority case on a device that has been used for a while. So the
 * picture is looked for but not required: without one the preview opens on the project's
 * name and size and still offers to open it, exactly as tapping the tile would. Refusing to
 * open at all would make the gesture look broken on precisely the rows whose tiles are
 * already blank.
 */
fun Fragment.showProjectPreview(
    host: PanelPreviewHost?,
    project: ExportResult,
    onOpen: (ExportResult) -> Unit
) {
    val picture = sequenceOf(project.imagePath, project.thumbnailPath)
        .filterNotNull()
        .filter { it.isNotBlank() }
        .map(::File)
        .firstOrNull { it.exists() }

    host?.show(
        asset = PreviewAsset.Artwork(
            breadcrumb = getString(R.string.recent_projects),
            title = project.fileName,
            // Glide is handed the empty string rather than null when there is nothing to
            // draw, so the well stays blank instead of the request throwing.
            source = picture ?: "",
            kind = "project",
            details = listOfBlankSafe(
                project.resolution,
                picture?.let { PreviewAsset.readableSize(it.length().toString()) }
            ),
            shareFile = picture
        ),
        primaryLabel = getString(R.string.preview_open_project),
        expanded = false,
        onPrimary = { onOpen(project) },
        onShare = picture?.let { file -> { _: PreviewAsset -> shareLocalFile(file) } }
    )
}

/**
 * A template, from Home's Popular Templates row, from a Duaen-style trend row, or from the
 * Templates screen's own grid.
 *
 * Nothing is offered but the primary action. The thumbnail belongs to the asset host rather
 * than the user, so there is nothing to hand another app; and the download button is left
 * off deliberately — a template's download is already driven by the tile's own progress UI,
 * and a second trigger for it sitting in a sheet over the top would be two things racing
 * for one entity's state.
 */
fun Fragment.showTemplatePreview(
    host: PanelPreviewHost?,
    template: TemplateEntity,
    breadcrumb: String,
    onUse: (TemplateEntity) -> Unit
) {
    // A template mid-download has a tile that is already saying so. Opening a sheet over it
    // would offer a button for something in flight.
    if (template.is_downloading && !template.is_downloaded) return
    host?.show(
        asset = PreviewAsset.Artwork(
            breadcrumb = breadcrumb,
            title = template.template_name.ifBlank { template.category.orEmpty() },
            source = Constants.BASE_URL_GLIDE + template.thumbnail_url,
            kind = "template",
            isPremium = template.is_premium && !template.is_subscribed,
            // Dropped when the back chip is already the category, which is what a
            // category row's breadcrumb always is — "‹ Duaen … Duaen" says it twice.
            details = listOfBlankSafe(
                template.category?.takeIf { !it.equals(breadcrumb, ignoreCase = true) }
            )
        ),
        primaryLabel = getString(
            if (template.is_downloaded) R.string.preview_open_template
            else R.string.preview_use_template
        ),
        expanded = false,
        onPrimary = { onUse(template) }
    )
}

/**
 * A font, from Home's Popular Fonts row or the Popular Fonts screen.
 *
 * This is the one navigation surface that reuses the editor's own [PreviewAsset.Font]
 * wholesale, because it is the same entity and the preview already knows what to do with
 * one: the real typeface once the file is local, the type-your-own-words field, the
 * alphabet and numerals. Only the verb changes.
 *
 * The primary action mirrors the tile exactly, including the split the tile makes: a font
 * that is not on the device downloads, and one that is opens the editor with a sample of
 * it. Download and share are wired too — unlike templates, the font tile's download button
 * and this one drive the same single call, so there is no second state machine to race.
 */
fun Fragment.showFontPreview(
    host: PanelPreviewHost?,
    font: FontEntity,
    breadcrumb: String,
    onUse: (FontEntity) -> Unit,
    onDownload: (FontEntity) -> Unit
) {
    host?.show(
        asset = PreviewAsset.Font(breadcrumb, font),
        primaryLabel = getString(
            if (font.is_downloaded) R.string.preview_use_font
            else R.string.preview_get_font
        ),
        expanded = false,
        onPrimary = { onUse(font) },
        onShare = {
            val file = PreviewShare.fontFile(font)
            if (file == null || !PreviewShare.send(requireContext(), file)) {
                view?.let {
                    Snackbar.make(it, R.string.preview_share_failed, Snackbar.LENGTH_SHORT).show()
                }
            }
        },
        onDownload = { onDownload(font) }
    )
}

/** Hands a file already on the device to another app, off the fragment's own scope. */
private fun Fragment.shareLocalFile(file: File) {
    val context = context ?: return
    viewLifecycleOwner.lifecycleScope.launch {
        if (!PreviewShare.send(context, file)) {
            view?.let {
                Snackbar.make(it, R.string.preview_share_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}

/** The view drops blanks itself; this only spares the callers a chain of lets. */
private fun listOfBlankSafe(vararg values: String?): List<String> =
    values.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
