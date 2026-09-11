package com.webscare.urducanvas.ui.editor.panels.preview

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.utils.Constants
import com.webscare.urducanvas.common.utils.SvgLoader
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.common.utils.isDarkModeEnabled
import com.webscare.urducanvas.data.model.FontEntity
import com.webscare.urducanvas.databinding.ViewAssetPreviewBinding
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

    init {
        binding.breadcrumbChip.addPressEffect { onBack?.invoke() }
        binding.primaryAction.addPressEffect { asset?.let { onPrimaryAction?.invoke(it) } }
        binding.shareAction.addPressEffect { asset?.let { onShare?.invoke(it) } }
        binding.downloadAction.addPressEffect { asset?.let { onDownload?.invoke(it) } }

        binding.sampleInput.addTextChangedListener { text ->
            sessionSampleText = text?.toString().orEmpty()
            renderFontSample()
        }
    }

    /**
     * Shows [asset]. [primaryLabel] differs by host — the adjustments panels apply to
     * the selected element ("Use on canvas"), the main panels add a new one
     * ("Add to canvas").
     */
    fun show(asset: PreviewAsset, expanded: Boolean, primaryLabel: String) {
        this.asset = asset
        this.expanded = expanded

        binding.breadcrumbLabel.text = asset.breadcrumb
        binding.previewTitle.text = asset.title
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
            Glide.with(this)
                .load(com.webscare.urducanvas.ui.editor.panels.images.resolveUrl(asset.entity))
                .into(binding.assetImage)
        }

        is PreviewAsset.Rendered -> {
            showImageOnly()
            binding.assetImage.setImageBitmap(asset.bitmap)
        }
    }

    private fun showImageOnly() {
        binding.fontSample.isVisible = false
        binding.sampleDivider.isVisible = false
        binding.alphabetRow.isVisible = false
        binding.numeralRow.isVisible = false
        binding.sampleInput.isVisible = false
        binding.assetImage.isVisible = true
        binding.assetImage.layoutParams = binding.assetImage.layoutParams.apply {
            height = dp(if (expanded) IMAGE_EXPANDED_DP else IMAGE_COLLAPSED_DP)
        }
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
            binding.assetImage.layoutParams = binding.assetImage.layoutParams.apply {
                height = dp(if (expanded) IMAGE_EXPANDED_DP else IMAGE_COLLAPSED_DP)
            }
            loadFontThumbnail(font.entity)
            return
        }

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
            if (relative.endsWith(".svg", ignoreCase = true)) {
                val scope = findViewTreeLifecycleOwner()?.lifecycleScope ?: return
                SvgLoader.load(
                    url = url,
                    imageView = binding.assetImage,
                    scope = scope,
                    cachedXml = null,
                    maxPx = 1024,
                    applyWhiteTint = dark
                ) { _, _ -> }
            } else {
                Glide.with(this).load(url).into(binding.assetImage)
            }
            return
        }

        val absolute = font.font_image?.takeIf { it.isNotBlank() }
        if (absolute != null) {
            Glide.with(this).load(absolute).into(binding.assetImage)
        } else {
            binding.assetImage.setImageResource(R.drawable.ic_font_thumbnail)
        }
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
        private const val IMAGE_COLLAPSED_DP = 130
        private const val IMAGE_EXPANDED_DP = 240
    }
}
