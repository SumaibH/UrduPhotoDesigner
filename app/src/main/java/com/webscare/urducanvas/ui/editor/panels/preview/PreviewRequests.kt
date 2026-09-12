package com.webscare.urducanvas.ui.editor.panels.preview

import android.graphics.Typeface
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.canvas.enums.ShapeType
import com.webscare.urducanvas.common.canvas.model.EmojiMeta
import com.webscare.urducanvas.common.utils.EmojiBitmapRenderer
import com.webscare.urducanvas.data.model.ImageEntity
import com.webscare.urducanvas.data.model.TextStylePreset
import com.webscare.urducanvas.ui.editor.panels.images.ImagesAdapter
import com.webscare.urducanvas.ui.editor.panels.objects.EmojiAdapter
import com.webscare.urducanvas.ui.editor.panels.shape.ShapeAdapter
import com.webscare.urducanvas.ui.editor.panels.text.styles.TextStyleThumbnailRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * What a picture grid does when one of its tiles asks for a preview.
 *
 * Three panels draw their stickers and images with the same [ImagesAdapter] —
 * Media, Objects and Shapes — so they open the preview the same way too, rather
 * than each keeping its own copy of the wiring.
 */
fun Fragment.showPicturePreview(
    adapter: ImagesAdapter?,
    entity: ImageEntity,
    breadcrumb: String,
    expanded: Boolean,
    primaryLabel: String = getString(R.string.preview_add_to_canvas)
) {
    val host = findPreviewHost() ?: return
    val grid = adapter ?: return
    host.show(
        asset = PreviewAsset.Picture(breadcrumb, entity),
        primaryLabel = primaryLabel,
        expanded = expanded,
        // Adding from the preview is the same act as tapping the tile, so it runs
        // the tile's own path — including the SVG resolve and the recents bookkeeping.
        onPrimary = { viewLifecycleOwner.lifecycleScope.launch { grid.selectImage(entity) } },
        onShare = { sharePicture(entity) }
    )
}

/**
 * An emoji has no file and no details — just the mark, drawn big.
 *
 * The tile is a few dozen pixels of a glyph, which is exactly the case where you
 * want to look closer before committing it to the canvas, so the preview renders
 * it again at full size rather than scaling the thumbnail up.
 */
fun Fragment.showEmojiPreview(
    adapter: EmojiAdapter?,
    meta: EmojiMeta,
    breadcrumb: String,
    expanded: Boolean,
    primaryLabel: String = getString(R.string.preview_add_to_canvas)
) {
    val host = findPreviewHost() ?: return
    val grid = adapter ?: return
    val context = context ?: return

    // Open on the shimmer straight away and fill the mark in when the renderer is
    // done. Waiting for the bitmap first meant a long-press did nothing visible for
    // as long as the render took, which reads as a press that missed.
    host.show(
        asset = PreviewAsset.Rendered(breadcrumb, meta.name, null),
        primaryLabel = primaryLabel,
        expanded = expanded,
        onPrimary = { viewLifecycleOwner.lifecycleScope.launch { grid.selectEmoji(meta) } }
    )

    viewLifecycleOwner.lifecycleScope.launch {
        val bitmap = withContext(Dispatchers.IO) {
            EmojiBitmapRenderer.render(context, meta.char, sizePx = EmojiAdapter.PREVIEW_RENDER_PX)
        }
        if (view == null) return@launch
        host.setRenderedBitmap(bitmap)
    }
}

/**
 * A style preset, drawn big in whatever typeface the grid is currently showing it in.
 *
 * The tile thumbnail is 180px, which is the one case where reusing it would not do —
 * a style is exactly the kind of thing you open a preview to look at closely — so
 * this asks the renderer for a fresh one at preview size.
 */
fun Fragment.showPresetPreview(
    preset: TextStylePreset,
    breadcrumb: String,
    expanded: Boolean,
    typeface: Typeface? = null,
    fontKey: String? = null,
    primaryLabel: String = getString(R.string.preview_add_to_canvas),
    onApply: (TextStylePreset) -> Unit
) {
    val host = findPreviewHost() ?: return
    val context = context ?: return
    val bitmap = runCatching {
        TextStyleThumbnailRenderer.renderForPreview(context, preset, PRESET_PREVIEW_PX, typeface)
    }.getOrElse {
        TextStyleThumbnailRenderer.getCachedOrGenerateThumbnail(context, preset, typeface, fontKey)
    }
    host.show(
        asset = PreviewAsset.Rendered(
            breadcrumb = breadcrumb,
            title = preset.name,
            bitmap = bitmap,
            details = listOf(preset.category.name.lowercase().replaceFirstChar { it.uppercase() })
        ),
        primaryLabel = primaryLabel,
        expanded = expanded,
        onPrimary = { onApply(preset) }
    )
}

/** Big enough to stay sharp at the preview's 240dp on a 3x screen. */
private const val PRESET_PREVIEW_PX = 720

/** Shapes are drawn once for the grid, so the preview reuses that bitmap. */
fun Fragment.showShapePreview(
    adapter: ShapeAdapter?,
    shape: ShapeType,
    breadcrumb: String,
    expanded: Boolean,
    primaryLabel: String = getString(R.string.preview_add_to_canvas)
) {
    val host = findPreviewHost() ?: return
    val grid = adapter ?: return
    host.show(
        asset = PreviewAsset.Rendered(breadcrumb, shape.displayName, grid.renderedBitmap(shape)),
        primaryLabel = primaryLabel,
        expanded = expanded,
        onPrimary = { grid.selectShape(shape) }
    )
}

/**
 * Pulls the picture out of Glide's cache and hands it to another app.
 *
 * Held by the fragment's view scope rather than the preview's: the chooser takes
 * the user out of the app, and a share that was already fetching should not be
 * cancelled by the preview closing behind them.
 */
fun Fragment.sharePicture(entity: ImageEntity) {
    val context = context ?: return
    viewLifecycleOwner.lifecycleScope.launch {
        val file = PreviewShare.pictureFile(context, entity)
        if (file == null || !PreviewShare.send(context, file)) {
            view?.let {
                Snackbar.make(it, R.string.preview_share_failed, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}
