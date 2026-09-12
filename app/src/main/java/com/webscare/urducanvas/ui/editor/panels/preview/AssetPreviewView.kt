package com.webscare.urducanvas.ui.editor.panels.preview

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.utils.Constants
import com.webscare.urducanvas.common.utils.SvgLoader
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.common.utils.isDarkModeEnabled
import com.webscare.urducanvas.common.utils.startShimmerSoft
import com.webscare.urducanvas.data.model.FontEntity
import com.webscare.urducanvas.data.model.ImageEntity
import com.webscare.urducanvas.databinding.ViewAssetPreviewBinding
import com.webscare.urducanvas.ui.editor.panels.images.resolveUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import java.io.File

/**
 * The asset preview the panels push over their own grid.
 *
 * Deliberately panel-agnostic: it is handed a [PreviewAsset] and four callbacks and
 * knows nothing about fonts panels, sticker grids or the sheet it sits in. The host
 * decides where it goes and how tall the sheet becomes.
 */
class AssetPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewAssetPreviewBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    var onBack: (() -> Unit)? = null
    var onPrimaryAction: ((PreviewAsset) -> Unit)? = null
    var onShare: ((PreviewAsset) -> Unit)? = null
    var onDownload: ((PreviewAsset) -> Unit)? = null

    private var asset: PreviewAsset? = null
    private var expanded = false

    /**
     * Set by the sheet before [show]. True only outside the editor, where there is no panel
     * holding the bottom of the screen and no canvas above that has to stay visible, so the
     * artwork gets a taller well than either editor state. Left false for the panels, whose
     * two heights are unchanged.
     */
    var tall = false

    private val zoom = PreviewZoom(binding.previewPaper, binding.zoomLayer)

    /**
     * Drives the SVG loads.
     *
     * Deliberately not a lifecycle scope. The sheet calls [show] from inside its own
     * onCreateView, and the fragment only attaches a ViewTreeLifecycleOwner to this view
     * after that returns — so findViewTreeLifecycleOwner() was still null at exactly the
     * moment it was asked for, and every SVG thumbnail gave up and left the page blank.
     * Since most font thumbnails are SVGs, that was every font not yet on the device.
     */
    private val svgScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        zoom.attach()
        binding.breadcrumbChip.addPressEffect { onBack?.invoke() }
        binding.primaryAction.addPressEffect { asset?.let { onPrimaryAction?.invoke(it) } }
        binding.shareAction.addPressEffect { asset?.let { onShare?.invoke(it) } }
        binding.downloadAction.addPressEffect { asset?.let { onDownload?.invoke(it) } }

        binding.sampleInput.addTextChangedListener { text ->
            sessionSampleText = text?.toString().orEmpty()
            renderFontSample()
        }
    }

    override fun onDetachedFromWindow() {
        svgScope.coroutineContext.cancelChildren()
        super.onDetachedFromWindow()
    }

    /**
     * Shows [asset]. [primaryLabel] differs by host — the adjustments panels apply to
     * the selected element ("Use on canvas"), the main panels add a new one
     * ("Add to canvas").
     */
    fun show(asset: PreviewAsset, expanded: Boolean, primaryLabel: String) {
        this.asset = asset
        this.expanded = expanded
        // A new asset always starts at life size; carrying the last one's zoom over
        // would open the preview already halfway into something else.
        zoom.reset()
        applyWellHeight()

        binding.breadcrumbLabel.text = asset.breadcrumb
        binding.previewTitle.text = asset.title
        // A picture with nothing readable in its file name falls back to the set it came
        // from, which is the word already on the back chip — "‹ Islamic Architecture
        // Islamic Architecture" says it twice and names nothing. Sooner have the chip
        // alone than an echo of it.
        binding.previewTitle.isVisible = !asset.title.equals(asset.breadcrumb, ignoreCase = true)
        binding.proPill.isVisible = asset.isPremium
        binding.primaryAction.text = primaryLabel

        // Nothing to fetch or hand to another app for a rendered mark.
        binding.shareAction.isVisible = asset.canShare
        binding.downloadAction.isVisible = asset.canDownload

        renderDetails(asset)
        renderBody(asset)
        setDownloaded(asset.isDownloaded)
        setDownloading(false)
    }

    fun setExpanded(expanded: Boolean) {
        if (this.expanded == expanded) return
        this.expanded = expanded
        applyWellHeight()
        asset?.let { renderBody(it) }
    }

    fun setDownloading(active: Boolean) {
        binding.downloadSpinner.isVisible = active
        binding.downloadIcon.isVisible = !active
    }

    fun setDownloaded(done: Boolean) {
        binding.downloadAction.setBackgroundResource(
            if (done) R.drawable.bg_preview_action_round_done
            else R.drawable.bg_preview_action_round
        )
        binding.downloadIcon.setImageResource(
            if (done) R.drawable.ic_done_small_stroke else R.drawable.ic_download
        )
        // The font can be rendered live only once its file is on the device.
        asset?.let { if (it is PreviewAsset.Font) renderFontSample() }
        renderDetails(asset ?: return)
    }

    // ── Body ─────────────────────────────────────────────────────────────────

    private fun renderBody(asset: PreviewAsset) = when (asset) {
        is PreviewAsset.Font -> {
            binding.assetImage.isVisible = false
            binding.fontSample.isVisible = true
            binding.sampleInput.isVisible = true
            binding.sampleInput.setText(sessionSampleText)
            binding.sampleInput.setSelection(binding.sampleInput.text?.length ?: 0)
            renderFontSample()
        }

        is PreviewAsset.Picture -> {
            showImageOnly()
            loadPicture(asset.entity)
        }

        is PreviewAsset.Artwork -> {
            showImageOnly()
            loadArtwork(asset)
        }

        is PreviewAsset.Rendered -> {
            showImageOnly()
            // Shapes and presets arrive already drawn; an emoji is still in the
            // renderer, and sends its bitmap along in a moment.
            if (asset.bitmap == null) startLoading() else finishLoading()
            binding.assetImage.setImageBitmap(asset.bitmap)
        }
    }

    /** The bitmap for a [PreviewAsset.Rendered] that was still being drawn at [show]. */
    fun setRenderedBitmap(bitmap: android.graphics.Bitmap?) {
        if (asset !is PreviewAsset.Rendered) return
        binding.assetImage.setImageBitmap(bitmap)
        finishLoading()
    }

    // ── Loading ──────────────────────────────────────────────────────────────

    private fun startLoading() {
        binding.assetShimmer.isVisible = true
        binding.assetShimmer.startShimmerSoft(context.isDarkModeEnabled())
    }

    private fun finishLoading() {
        binding.assetShimmer.stopShimmer()
        binding.assetShimmer.setShimmer(null)
        binding.assetShimmer.isVisible = false
    }

    /** Both of Glide's endings mean the shimmer has done its job. */
    private val shimmerStopper = object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean
        ): Boolean {
            finishLoading(); return false
        }

        override fun onResourceReady(
            resource: Drawable, model: Any, target: Target<Drawable>?,
            dataSource: DataSource, isFirstResource: Boolean
        ): Boolean {
            finishLoading(); return false
        }
    }

    private fun showImageOnly() {
        binding.fontSample.isVisible = false
        binding.sampleDivider.isVisible = false
        binding.alphabetRow.isVisible = false
        binding.numeralRow.isVisible = false
        binding.sampleInput.isVisible = false
        binding.assetImage.isVisible = true
    }

    /**
     * Draws the sample in the font's own face when the file is local.
     *
     * Before that there is no typeface to draw with, so the tile's own thumbnail
     * stands in and the type-your-own-words field is disabled — typing would
     * otherwise appear to do nothing.
     */
    private fun renderFontSample() {
        val font = asset as? PreviewAsset.Font ?: return
        val local = font.entity.file_path
            ?.takeIf { it.isNotBlank() && File(it).exists() }
            ?.let { runCatching { Typeface.createFromFile(it) }.getOrNull() }

        binding.sampleInput.isEnabled = local != null
        binding.sampleInput.hint = context.getString(
            if (local != null) R.string.preview_type_your_words
            else R.string.preview_download_to_type
        )

        if (local == null) {
            // Fall back to the thumbnail the grid tile already shows.
            binding.fontSample.isVisible = false
            binding.sampleDivider.isVisible = false
            binding.alphabetRow.isVisible = false
            binding.numeralRow.isVisible = false
            binding.assetImage.isVisible = true
            loadFontThumbnail(font.entity)
            return
        }

        // The face is on the device, so the specimen draws this frame.
        finishLoading()
        binding.assetImage.isVisible = false
        binding.fontSample.isVisible = true
        binding.fontSample.typeface = local
        binding.fontSample.text = sessionSampleText.ifBlank { DEFAULT_SAMPLE }
        binding.fontSample.setTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            if (expanded) SAMPLE_EXPANDED_SP else SAMPLE_COLLAPSED_SP
        )

        // The full face is only worth the room once the panel is expanded.
        binding.sampleDivider.isVisible = expanded
        binding.alphabetRow.isVisible = expanded
        binding.numeralRow.isVisible = expanded
        if (expanded) {
            binding.alphabetRow.typeface = local
            binding.alphabetRow.text = URDU_ALPHABET
            binding.numeralRow.typeface = local
            binding.numeralRow.text = URDU_NUMERALS
        }
    }

    /**
     * The tile's thumbnail, loaded the way the tile loads it.
     *
     * Not as simple as handing the URL to Glide: `image_url` is a path relative to
     * the asset host and has to be prefixed, most of them are SVGs that Glide
     * cannot decode, and `font_image` is the absolute-URL fallback used only when
     * there is no `image_url` at all. Loading the bare field showed an empty page
     * for every font that wasn't downloaded — which is exactly the case this
     * fallback exists for.
     */
    private fun loadFontThumbnail(font: FontEntity) {
        val relative = font.image_url
        val dark = context.isDarkModeEnabled()
        if (dark) {
            binding.assetImage.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        } else {
            binding.assetImage.clearColorFilter()
        }

        if (relative.isNotEmpty()) {
            val url = Constants.BASE_URL_GLIDE + relative
            startLoading()
            if (relative.endsWith(".svg", ignoreCase = true)) {
                loadSvg(url, cachedXml = null, whiteTint = dark)
            } else {
                Glide.with(this).load(url).listener(shimmerStopper).into(binding.assetImage)
            }
            return
        }

        val absolute = font.font_image?.takeIf { it.isNotBlank() }
        if (absolute != null) {
            startLoading()
            Glide.with(this).load(absolute).listener(shimmerStopper).into(binding.assetImage)
        } else {
            binding.assetImage.setImageResource(R.drawable.ic_font_thumbnail)
            finishLoading()
        }
    }

    /**
     * The picture, loaded the way its own tile loads it.
     *
     * Most stickers and every shape are SVGs, and Glide cannot decode one — handing it
     * the url drew nothing at all, which is why a blob or an ornament opened onto an
     * empty page. The entity's cached xml goes along so a picture the grid has already
     * parsed is not fetched and parsed a second time.
     */
    private fun loadPicture(entity: ImageEntity) {
        binding.assetImage.clearColorFilter()
        startLoading()
        val url = resolveUrl(entity)
        if (entity.file_name.endsWith(".svg", ignoreCase = true)) {
            loadSvg(url, cachedXml = entity.bitmapData, whiteTint = false)
        } else {
            Glide.with(this).load(url).listener(shimmerStopper).into(binding.assetImage)
        }
    }

    /**
     * A template thumbnail or a saved project's export, drawn big.
     *
     * Both are ordinary rasters — no SVG path and no white tint. The one thing worth doing
     * for a saved project is keying the cache on the file's timestamp, the way the Recents
     * tile does: re-saving a project rewrites the same path, and without the signature the
     * preview would show the copy Glide cached before the edit.
     */
    private fun loadArtwork(artwork: PreviewAsset.Artwork) {
        binding.assetImage.clearColorFilter()
        startLoading()
        var request = Glide.with(this).load(artwork.source)
        (artwork.source as? File)?.let {
            request = request.signature(
                com.bumptech.glide.signature.ObjectKey(it.lastModified())
            )
        }
        request.listener(shimmerStopper).into(binding.assetImage)
    }

    /**
     * SvgLoader announces success through its callback but returns silently when the file
     * cannot be parsed, so the shimmer is stopped on the job ending as well — otherwise a
     * picture that fails to load shimmers for as long as the preview is open.
     */
    private fun loadSvg(url: String, cachedXml: String?, whiteTint: Boolean) {
        val job: Job = SvgLoader.load(
            url = url,
            imageView = binding.assetImage,
            scope = svgScope,
            cachedXml = cachedXml,
            maxPx = SVG_MAX_PX,
            applyWhiteTint = whiteTint
        ) { _, _ -> finishLoading() }
        job.invokeOnCompletion { post { finishLoading() } }
    }

    /**
     * The artwork gets a fixed height rather than a minimum one.
     *
     * The shimmer covering the well while an asset loads is match_parent, and a
     * match_parent child measures against everything going spare in a wrap_content
     * parent: the well grew to the height of the screen, the sheet opened full height
     * with the pills and the buttons pushed off the bottom of it, and it only settled
     * once the asset arrived. Fixing the well also hands the picture the whole page
     * instead of penning it into a short letterbox with dead paper underneath.
     */
    private fun applyWellHeight() {
        binding.previewWell.layoutParams = binding.previewWell.layoutParams.apply {
            height = dp(
                when {
                    tall -> WELL_TALL_DP
                    expanded -> WELL_EXPANDED_DP
                    else -> WELL_COLLAPSED_DP
                }
            )
        }
        binding.previewWell.requestLayout()
    }

    // ── Detail pills ─────────────────────────────────────────────────────────

    private fun renderDetails(asset: PreviewAsset) {
        binding.chipRow.removeAllViews()
        asset.details.filter { it.isNotBlank() }.forEach { binding.chipRow.addView(pill(it, false)) }
        if (asset.canDownload) {
            binding.chipRow.addView(
                pill(
                    context.getString(
                        if (asset.isDownloaded) R.string.preview_downloaded
                        else R.string.preview_not_downloaded
                    ),
                    asset.isDownloaded
                )
            )
        }
    }

    private fun pill(label: String, downloaded: Boolean) = TextView(context).apply {
        text = label
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f)
        typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.medium)
        includeFontPadding = false
        gravity = android.view.Gravity.CENTER
        setPadding(dp(9), 0, dp(9), 0)
        height = dp(24)
        setBackgroundResource(
            if (downloaded) R.drawable.bg_preview_chip_downloaded else R.drawable.bg_preview_chip
        )
        setTextColor(
            ContextCompat.getColor(context, if (downloaded) R.color.sub_save_text else R.color.black)
        )
        if (downloaded) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_done_small_stroke, 0, 0, 0)
            compoundDrawablePadding = dp(4)
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        /**
         * The words typed into the sample field, kept for the session so switching
         * fonts keeps trying the same phrase rather than resetting each time.
         */
        var sessionSampleText: String = ""

        private const val DEFAULT_SAMPLE = "اردو کینوس"
        private const val URDU_ALPHABET =
            "ا ب پ ت ٹ ث ج چ ح خ د ڈ ذ ر ڑ ز ژ س ش ص ض ط ظ ع غ ف ق ک گ ل م ن و ہ ھ ی ے"
        private const val URDU_NUMERALS = "۰ ۱ ۲ ۳ ۴ ۵ ۶ ۷ ۸ ۹"

        private const val SAMPLE_COLLAPSED_SP = 32f
        private const val SAMPLE_EXPANDED_SP = 48f
        private const val WELL_COLLAPSED_DP = 210
        private const val WELL_EXPANDED_DP = 320

        /**
         * Off the editor. Taller than the expanded panel because nothing is competing for
         * the screen there — no panel below and no canvas above to keep in view.
         */
        private const val WELL_TALL_DP = 420
        private const val SVG_MAX_PX = 1024
    }
}
