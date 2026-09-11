package com.webscare.urducanvas.ui.editor.panels.preview

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.webscare.urducanvas.common.utils.SvgLoader
import com.webscare.urducanvas.data.model.FontEntity
import com.webscare.urducanvas.data.model.ImageEntity
import com.webscare.urducanvas.ui.editor.panels.images.resolveUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hands a previewed asset to another app.
 *
 * Same `ACTION_SEND` through the app's FileProvider that the Files screen uses;
 * the only extra work is that panel assets are not always on disk yet. Fonts are
 * a real file once downloaded, pictures are streamed and have to be pulled out of
 * Glide's cache, and SVGs are text the app may already be holding.
 */
object PreviewShare {

    /** The font's own `.ttf`/`.otf`, or null while it is still only a URL. */
    fun fontFile(font: FontEntity): File? =
        font.file_path?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.exists() }

    /**
     * A local copy of [image], fetched if need be. Runs on IO — pictures come out
     * of Glide's disk cache and SVGs are written from the XML the entity carries.
     */
    suspend fun pictureFile(context: Context, image: ImageEntity): File? =
        withContext(Dispatchers.IO) {
            val url = resolveUrl(image)
            val target = File(context.cacheDir, SHARE_DIR)
                .apply { mkdirs() }
                .let { File(it, safeName(image.file_name)) }

            runCatching {
                if (image.file_name.endsWith(".svg", ignoreCase = true)) {
                    val xml = image.bitmapData?.takeIf { it.trimStart().startsWith("<") }
                        ?: SvgLoader.resolve(url, image.bitmapData, false)?.second
                        ?: return@runCatching null
                    target.writeText(xml)
                } else {
                    val cached = Glide.with(context).asFile().load(url).submit().get()
                    cached.copyTo(target, overwrite = true)
                }
                target
            }.getOrNull()
        }

    /** Fires the chooser. Returns false when the file went missing under us. */
    fun send(context: Context, file: File): Boolean {
        if (!file.exists()) return false
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull() ?: return false

        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeFor(file)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                null
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        )
        return true
    }

    private fun mimeFor(file: File) = when (file.extension.lowercase()) {
        "ttf" -> "font/ttf"
        "otf" -> "font/otf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "svg" -> "image/svg+xml"
        else -> "application/octet-stream"
    }

    /** File names arrive from the API, so they are not to be trusted as paths. */
    private fun safeName(raw: String): String {
        val cleaned = raw.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
        return cleaned.takeIf { it.isNotBlank() && it != "." && it != ".." } ?: "asset"
    }

    private const val SHARE_DIR = "shared"
}
