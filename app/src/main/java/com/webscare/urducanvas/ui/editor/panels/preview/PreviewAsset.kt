package com.webscare.urducanvas.ui.editor.panels.preview

import android.graphics.Bitmap
import com.webscare.urducanvas.data.model.FontEntity
import com.webscare.urducanvas.data.model.ImageEntity

/**
 * What the in-panel preview is showing.
 *
 * Three shapes cover every asset grid in the editor. Fonts and images carry their
 * entity because the preview reads details off it and the actions act on it;
 * shapes, emoji and style presets have no entity and no download state, so they
 * arrive already rendered.
 */
sealed class PreviewAsset {

    /** The tab this was opened from — the label on the back chip. */
    abstract val breadcrumb: String

    abstract val title: String
    abstract val isPremium: Boolean

    /** Whether the asset is on the device. Assets with nothing to fetch are always true. */
    abstract val isDownloaded: Boolean

    /** Pills under the paper, in order. Blank entries are dropped by the view. */
    abstract val details: List<String>

    /** False for assets that cannot be downloaded or shared as a file. */
    open val hasFile: Boolean get() = true

    data class Font(
        override val breadcrumb: String,
        val entity: FontEntity
    ) : PreviewAsset() {
        override val title get() = com.webscare.urducanvas.common.utils.Utils.cleanFontName(entity.font_name)
        override val isPremium get() = entity.is_premium && !entity.is_subscribed
        override val isDownloaded get() = entity.is_downloaded
        override val details
            get() = listOf(entity.font_language, entity.font_category, readableSize(entity.file_size))
    }

    data class Picture(
        override val breadcrumb: String,
        val entity: ImageEntity
    ) : PreviewAsset() {
        override val title
            get() = entity.alt_text?.takeIf { it.isNotBlank() }
                ?: entity.file_name.substringBeforeLast('.')
        override val isPremium get() = entity.is_premium && !entity.is_subscribed
        // Images are streamed and cached by Glide rather than recorded as downloaded,
        // so there is no per-item flag on the entity to read.
        override val isDownloaded get() = true
        override val details
            get() = listOf(
                entity.category,
                if (entity.file_name.endsWith(".svg", true)) "SVG" else "PNG",
                readableSize(entity.file_size)
            )
    }

    /**
     * Anything already rendered to a bitmap: shapes, emoji and style presets. Nothing
     * to download and no file to share, so the preview shows the mark and the one
     * action that applies.
     */
    data class Rendered(
        override val breadcrumb: String,
        override val title: String,
        val bitmap: Bitmap?,
        override val details: List<String> = emptyList()
    ) : PreviewAsset() {
        override val isPremium get() = false
        override val isDownloaded get() = true
        override val hasFile get() = false
    }

    companion object {
        /** file_size arrives as a byte count in a string, and sometimes already formatted. */
        internal fun readableSize(raw: String?): String {
            val bytes = raw?.trim()?.toLongOrNull() ?: return raw?.trim().orEmpty()
            if (bytes <= 0L) return ""
            val kb = bytes / 1024.0
            return if (kb < 1024) "${kb.toInt()} KB"
            else String.format(java.util.Locale.US, "%.1f MB", kb / 1024.0)
        }
    }
}
