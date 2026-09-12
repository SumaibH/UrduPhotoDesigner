package com.webscare.urducanvas.ui.editor.panels.text.styles

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnNextLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.canvas.enums.LabelShape
import androidx.core.view.isVisible
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.common.utils.Utils.addPressEffectWithLongClick
import com.webscare.urducanvas.data.model.TextPreset
import com.webscare.urducanvas.data.model.TextStylePreset
import com.webscare.urducanvas.databinding.LayoutTextStylePresetItemBinding

/**
 * One tile in the text panel's grid: either a style or a whole lockup.
 *
 * The two share a grid because "All" is one feed of both, and because a user browsing
 * for something to put on a canvas is not thinking about which of the two kinds of
 * thing it is. They share a tile and a size; what differs is only what gets drawn in
 * it and what a tap does.
 */
sealed class PanelCard {
    abstract val id: String

    data class Style(val style: TextStylePreset) : PanelCard() {
        override val id: String get() = style.id
    }

    data class Lockup(val preset: TextPreset) : PanelCard() {
        override val id: String get() = preset.id
    }
}

class TextStylesMainAdapter(
    /**
     * Asked to open the preview for a tile. Long-press while the panel is
     * collapsed, the eye button while it is expanded — the same rule every other
     * asset grid follows. Declared before [onPresetClick] so that stays the last
     * parameter and the existing trailing-lambda call sites keep binding to it.
     */
    private val onPreviewRequested: (TextStylePreset) -> Unit = {},
    /** Asked to insert a lockup. Separate from [onPresetClick]: it drops several elements, not paint. */
    private val onLockupClick: (TextPreset) -> Unit = {},
    /**
     * The face a lockup layer should be drawn in, by [com.webscare.urducanvas.data.model.FontEntity.file_name],
     * or null when that font is not on disk — the renderer then falls back, so the card
     * still reads and upgrades itself once the download finishes.
     */
    private val typefaceFor: (String) -> Typeface? = { null },
    /**
     * Whether a lockup needs a subscription — true if any font or style it uses does.
     * Asked rather than stored, so a card re-prices itself when a flag changes on the
     * dashboard instead of when content is re-released.
     */
    internal val isLockupPremium: (TextPreset) -> Boolean = { false },
    private val onPresetClick: (TextStylePreset) -> Unit
) : ListAdapter<PanelCard, TextStylesMainAdapter.PresetViewHolder>(DiffCallback()) {

    /**
     * The lockup currently fetching its fonts, and how far along it is.
     *
     * One at a time by construction: the card is inert while it downloads, so a second
     * tap on the same card does nothing and a tap on another one is what replaces this.
     */
    internal var downloadingLockupId: String? = null
    internal var downloadPercent: Int = 0

    /** Shows or clears the progress overlay. Pass null for [id] when it is finished. */
    fun setLockupDownload(id: String?, percent: Int) {
        val changed = listOf(downloadingLockupId, id).filterNotNull().distinct()
        downloadingLockupId = id
        downloadPercent = percent.coerceIn(0, 100)
        // Only the cards whose state actually moved, so the rest of the grid is not
        // re-rendered — a lockup bitmap is not cheap to draw.
        changed.forEach { changedId ->
            val index = currentList.indexOfFirst { it is PanelCard.Lockup && it.id == changedId }
            if (index >= 0) notifyItemChanged(index)
        }
    }

    /** Submits styles, for the callers that only ever have styles. */
    fun submitStyles(styles: List<TextStylePreset>, commitCallback: Runnable? = null) =
        submitList(styles.map { PanelCard.Style(it) }, commitCallback ?: Runnable {})

    /** Submits lockups. */
    fun submitLockups(presets: List<TextPreset>, commitCallback: Runnable? = null) =
        submitList(presets.map { PanelCard.Lockup(it) }, commitCallback ?: Runnable {})

    var slideOffset: Float = 0f
    var recyclerViewWidth: Int = 0
    var recyclerViewPadding: Int = 0

    var attachedRecyclerView: RecyclerView? = null
        private set

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        if (attachedRecyclerView == recyclerView) {
            attachedRecyclerView = null
        }
    }

    var isExpanded: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    var selectedPresetId: String? = null
        set(value) {
            val old = field
            field = value
            if (old == value) return
            // Only the two tiles whose ring actually moved. notifyDataSetChanged() here
            // rebuilt every holder -- and a lockup bitmap is not cheap to draw -- as well
            // as throwing away the scroll anchor, so picking a preset from halfway along
            // the shelf could drop you back at the start. Same discipline as
            // [setLockupDownload] right above.
            listOfNotNull(old, value).distinct().forEach { id ->
                val index = currentList.indexOfFirst { it.id == id }
                if (index >= 0) notifyItemChanged(index)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val binding = LayoutTextStylePresetItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PresetViewHolder(binding, this, onPreviewRequested, onLockupClick, typefaceFor, onPresetClick)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        holder.bind(getItem(position), selectedPresetId, slideOffset, recyclerViewWidth, recyclerViewPadding)
    }

    class PresetViewHolder(
        private val binding: LayoutTextStylePresetItemBinding,
        private val adapter: TextStylesMainAdapter,
        private val onPreviewRequested: (TextStylePreset) -> Unit,
        private val onLockupClick: (TextPreset) -> Unit,
        private val typefaceFor: (String) -> Typeface?,
        private val onPresetClick: (TextStylePreset) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        val cardRoot: MaterialCardView get() = binding.presetItemContainer
        val previewImg: ImageView get() = binding.presetPreviewImage
        val titleTxt: TextView get() = binding.presetTitleText

        fun bind(
            card: PanelCard,
            selectedPresetId: String?,
            slideOffset: Float,
            rvWidth: Int,
            rvPadding: Int
        ) {
            titleTxt.visibility = View.GONE
            previewImg.visibility = View.VISIBLE

            val isSelected = card.id == selectedPresetId
            val strokePx = (1.5f * cardRoot.context.resources.displayMetrics.density + 0.5f).toInt()
            cardRoot.strokeWidth = if (isSelected) strokePx else 0
            cardRoot.strokeColor = ContextCompat.getColor(cardRoot.context, R.color.appColor)

            // Sized before the bitmap is asked for: a lockup is rendered at the tile's
            // real pixel size, and asking before layout would draw it for the wrong box.
            updateSize(slideOffset, rvWidth, rvPadding)

            when (card) {
                is PanelCard.Style -> bindStyle(card.style)
                is PanelCard.Lockup -> bindLockup(card.preset)
            }
        }

        private fun bindStyle(preset: TextStylePreset) {
            previewImg.setImageBitmap(
                TextStyleThumbnailRenderer.getCachedOrGenerateThumbnail(cardRoot.context, preset)
            )
            // Reset what only the lockup path sets, so a recycled tile arrives clean.
            previewImg.alpha = 1f
            binding.isPremium.isVisible = preset.isPremium

            // One rule everywhere: long-press opens the preview only while collapsed,
            // and the eye button is what does it once there is room to draw one.
            if (adapter.isExpanded) {
                cardRoot.addPressEffect {
                    adapter.selectedPresetId = preset.id
                    onPresetClick(preset)
                }
            } else {
                cardRoot.addPressEffectWithLongClick(
                    onLongClick = { onPreviewRequested(preset) },
                    onClick = {
                        adapter.selectedPresetId = preset.id
                        onPresetClick(preset)
                    }
                )
            }

            binding.previewEye.apply {
                isVisible = adapter.isExpanded
                addPressEffect { onPreviewRequested(preset) }
            }
        }

        private fun bindLockup(preset: TextPreset) {
            val lp = cardRoot.layoutParams
            val side = lp?.width?.takeIf { it > 0 } ?: TILE_FALLBACK_PX
            val height = lp?.height?.takeIf { it > 0 } ?: side

            // Only the faces this lockup actually asks for, so a card is not rebuilt
            // every time some unrelated font finishes downloading.
            val typefaces = preset.fontIds.mapNotNull { id ->
                typefaceFor(id)?.let { id to it }
            }.toMap()

            previewImg.setImageBitmap(
                TextStyleThumbnailRenderer.getCachedOrGenerateLockup(
                    cardRoot.context, preset, typefaces, side, height
                )
            )

            binding.isPremium.isVisible = adapter.isLockupPremium(preset)

            // No preview sheet for lockups yet — the eye would open nothing.
            binding.previewEye.isVisible = false

            val isDownloading = preset.id == adapter.downloadingLockupId
            if (isDownloading) {
                // The card is its own progress indicator: the lockup dims and the
                // percentage sits over it, so the thing being waited for is the thing
                // showing the wait. Inert meanwhile — tapping twice must not insert twice.
                previewImg.alpha = DOWNLOADING_ALPHA
                titleTxt.text = cardRoot.context.getString(
                    R.string.percent_complete, adapter.downloadPercent
                )
                titleTxt.visibility = View.VISIBLE
                cardRoot.isClickable = false
                cardRoot.setOnClickListener(null)
            } else {
                previewImg.alpha = 1f
                titleTxt.visibility = View.GONE
                cardRoot.addPressEffect { onLockupClick(preset) }
            }
        }

        fun updateSize(slideOffset: Float, rvWidth: Int, rvPadding: Int) {
            val context = cardRoot.context
            val density = context.resources.displayMetrics.density
            val recyclerView = (cardRoot.parent as? RecyclerView) ?: adapter.attachedRecyclerView

            val marginEndPx = (6 * density).toInt()
            val marginBottomPx = (6 * density).toInt()

            val lm = recyclerView?.layoutManager as? androidx.recyclerview.widget.GridLayoutManager
            val spanCountCollapsed = lm?.spanCount?.coerceAtLeast(1) ?: 3

            val rvHeight = recyclerView?.height ?: 0
            val rvPaddingY = (recyclerView?.paddingTop ?: 0) + (recyclerView?.paddingBottom ?: 0)
            val availHeight = rvHeight - rvPaddingY

            if (availHeight <= 0) {
                // Guessing a height here wrote a size into lp.height that had
                // nothing to do with the row the layout manager would hand the
                // item, and GridLayoutManager honours an explicit child height
                // verbatim in its cross axis, so an oversized tile ran straight
                // into the row below. Wait for a real measurement instead.
                recyclerView?.doOnNextLayout { updateSize(slideOffset, rvWidth, rvPadding) }
                return
            }

            val computedCollapsedHeight =
                ((availHeight - (spanCountCollapsed * marginBottomPx)) / spanCountCollapsed).coerceAtLeast((24 * density).toInt())

            val collapsedSize = computedCollapsedHeight

            val effectiveWidth = if (rvWidth > 0) rvWidth else (recyclerView?.width ?: 0)
            val columnWidth = if (effectiveWidth > 0) {
                val spanCountExpanded = 3 // 3 columns in expanded mode
                val totalMarginW = (spanCountExpanded - 1) * marginEndPx
                ((effectiveWidth - rvPadding - totalMarginW) / spanCountExpanded).toInt()
            } else collapsedSize

            val currentSize = (collapsedSize + (columnWidth - collapsedSize) * slideOffset).toInt()
            val finalSize = currentSize.coerceAtLeast(1)

            val lp = cardRoot.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                if (lp.width != finalSize || lp.height != finalSize || lp.leftMargin != 0 || lp.topMargin != 0 || lp.rightMargin != marginEndPx || lp.bottomMargin != marginBottomPx) {
                    lp.width = finalSize
                    lp.height = finalSize
                    lp.leftMargin = 0
                    lp.topMargin = 0
                    lp.rightMargin = marginEndPx
                    lp.bottomMargin = marginBottomPx
                    cardRoot.layoutParams = lp
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PanelCard>() {
        override fun areItemsTheSame(oldItem: PanelCard, newItem: PanelCard): Boolean =
            oldItem::class == newItem::class && oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: PanelCard, newItem: PanelCard): Boolean =
            oldItem == newItem
    }

    companion object {
        /** Used only if a tile is bound before it has been measured; layout corrects it. */
        private const val TILE_FALLBACK_PX = 180

        /** How far the lockup fades behind its own progress figure. */
        private const val DOWNLOADING_ALPHA = 0.3f
    }
}
