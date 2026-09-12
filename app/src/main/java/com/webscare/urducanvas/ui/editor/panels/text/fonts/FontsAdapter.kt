package com.webscare.urducanvas.ui.editor.panels.text.fonts

import android.graphics.drawable.PictureDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.utils.Constants
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.common.utils.Utils.addPressEffectWithLongClick
import com.webscare.urducanvas.common.utils.onBoxResized
import com.webscare.urducanvas.common.utils.removeBoxResizedWatcher
import com.webscare.urducanvas.common.utils.startShimmerSoft
import com.webscare.urducanvas.common.utils.isDarkModeEnabled
import com.webscare.urducanvas.data.model.FontEntity
import com.webscare.urducanvas.databinding.LayoutFontItemBinding

import com.webscare.urducanvas.databinding.LayoutFontItemExpandedBinding

class FontsAdapter(
    /**
     * Asked to open the preview for a tile. Long-press while the panel is collapsed,
     * the eye button while it is expanded — one rule, two states, because long-press
     * is free in the collapsed strip and taken by multi-select in the expanded grid.
     *
     * Declared before [onFontSelected] so that stays the last parameter and the
     * existing trailing-lambda call sites keep binding to it.
     */
    private val onPreviewRequested: (FontEntity) -> Unit = {},
    private val onFontSelected: (FontEntity, Boolean) -> Unit
) : ListAdapter<FontEntity, FontsAdapter.FontViewHolder>(DiffCallback()) {

    companion object {
        const val TYPE_COLLAPSED = 0
        const val TYPE_EXPANDED  = 1
    }

    private val downloadingIds = mutableSetOf<Int>()

    var slideOffset: Float = 0f
    var recyclerViewWidth: Int = 0
    var recyclerViewPadding: Int = 0

    var attachedRecyclerView: RecyclerView? = null
        private set

    /** See [com.webscare.urducanvas.common.utils.onBoxResized]. */
    private var boxWatcher: View.OnLayoutChangeListener? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
        boxWatcher = recyclerView.onBoxResized { rv -> resizeVisibleTiles(rv) }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerView.removeBoxResizedWatcher(boxWatcher)
        boxWatcher = null
        if (attachedRecyclerView == recyclerView) {
            attachedRecyclerView = null
        }
    }

    /**
     * Re-measures every tile now that [rv] has been laid out at its settled height.
     *
     * The width/padding come from the list itself rather than from the cached
     * [recyclerViewWidth]/[recyclerViewPadding], because this runs precisely when those
     * cached values are the ones that may be stale.
     */
    private fun resizeVisibleTiles(rv: RecyclerView) {
        val rvPadding = rv.paddingLeft + rv.paddingRight
        recyclerViewWidth = rv.width
        recyclerViewPadding = rvPadding
        for (i in 0 until rv.childCount) {
            val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? FontViewHolder ?: continue
            holder.updateSize(slideOffset, rv.width, rvPadding)
        }
    }

    fun addDownloadingId(id: Int) {
        if (downloadingIds.add(id)) {
            val pos = currentList.indexOfFirst { it.id == id }
            if (pos != -1) notifyItemChanged(pos)
        }
    }

    fun clearDownloadingId(id: Int) {
        if (downloadingIds.remove(id)) {
            val pos = currentList.indexOfFirst { it.id == id }
            if (pos != -1) notifyItemChanged(pos)
        }
    }

    var isExpanded: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    var selectedFontId: String? = null
        set(value) {
            val old = field
            field = value
            if (old != value) {
                old?.let { oldId ->
                    val oldPos = currentList.indexOfFirst { it.id.toString() == oldId }
                    if (oldPos != -1) notifyItemChanged(oldPos)
                }
                value?.let { newId ->
                    val newPos = currentList.indexOfFirst { it.id.toString() == newId }
                    if (newPos != -1) notifyItemChanged(newPos)
                }
            }
        }

    override fun getItemViewType(position: Int): Int =
        if (isExpanded) TYPE_EXPANDED else TYPE_COLLAPSED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_EXPANDED) {
            Expanded(
                LayoutFontItemExpandedBinding.inflate(inflater, parent, false),
                this,
                onFontSelected,
                onPreviewRequested
            )
        } else {
            Collapsed(
                LayoutFontItemBinding.inflate(inflater, parent, false),
                this,
                onFontSelected,
                onPreviewRequested
            )
        }
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        val font = getItem(position)
        val isDownloading = downloadingIds.contains(font.id)
        holder.bind(font, selectedFontId, isDownloading, slideOffset, recyclerViewWidth, recyclerViewPadding)
    }

    // ── ViewHolder ──────────────────────────────────────────────────────────

    sealed class FontViewHolder(
        itemView: View,
        private val adapter: FontsAdapter,
        private val onFontSelected: (FontEntity, Boolean) -> Unit,
        private val onPreviewRequested: (FontEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        abstract val cardRoot: com.google.android.material.card.MaterialCardView
        abstract val fontImage: android.widget.ImageView
        abstract val shimmer: com.facebook.shimmer.ShimmerFrameLayout
        abstract val loadingAnim: com.airbnb.lottie.LottieAnimationView
        abstract val premiumBadge: android.widget.ImageView
        abstract val downloadIcon: android.widget.ImageView
        open val fontNameTextView: android.widget.TextView? get() = null

        /** Only the expanded tile carries one; the collapsed tile opens on long-press. */
        open val previewEye: android.widget.ImageView? get() = null

        fun bind(
            font: FontEntity,
            selectedFontId: String?,
            isDownloading: Boolean,
            slideOffset: Float,
            rvWidth: Int,
            rvPadding: Int
        ) {
            val isSelected = font.id.toString() == selectedFontId
            val strokePx = (1.5f * cardRoot.context.resources.displayMetrics.density + 0.5f).toInt()

            cardRoot.strokeWidth = if (isSelected) strokePx else 0
            cardRoot.strokeColor = ContextCompat.getColor(
                cardRoot.context, R.color.appColor
            )

            fontNameTextView?.text = com.webscare.urducanvas.common.utils.Utils.cleanFontName(font.font_name)

            premiumBadge.isVisible = font.is_premium && !font.is_subscribed
            downloadIcon.visibility =
                if (font.is_downloaded || isDownloading) View.GONE else View.VISIBLE
            loadingAnim.visibility =
                if (isDownloading) View.VISIBLE else View.GONE

            // Tap is unchanged in both states, and carries no added delay: the
            // long-press helper below starts its timer on ACTION_DOWN and fires the
            // click immediately on a short ACTION_UP.
            val select = { onFontSelected(font, font.is_downloaded) }
            if (adapter.isExpanded) {
                itemView.addPressEffect { select() }
            } else {
                itemView.addPressEffectWithLongClick(
                    onLongClick = { onPreviewRequested(font) },
                    onClick = { select() }
                )
            }

            previewEye?.apply {
                isVisible = true
                addPressEffect { onPreviewRequested(font) }
            }

            updateSize(slideOffset, rvWidth, rvPadding)
            loadImage(font, fontImage, shimmer)
        }

        fun updateSize(slideOffset: Float, rvWidth: Int, rvPadding: Int) {
            val context = itemView.context
            val density = context.resources.displayMetrics.density
            val recyclerView = (itemView.parent as? RecyclerView) ?: adapter.attachedRecyclerView

            val marginEndPx = (6 * density).toInt()
            val marginBottomPx = (6 * density).toInt()

            val lm = recyclerView?.layoutManager as? androidx.recyclerview.widget.GridLayoutManager
            val spanCount = lm?.spanCount?.coerceAtLeast(1) ?: 3

            val rvHeight = recyclerView?.height ?: 0
            val rvPaddingY = (recyclerView?.paddingTop ?: 0) + (recyclerView?.paddingBottom ?: 0)
            val availHeight = rvHeight - rvPaddingY

            if (availHeight <= 0) {
                // No usable height yet — this is a fresh view, e.g. the panel
                // coming back from Text Properties. A guessed 70dp used to be
                // written straight into lp.height, and GridLayoutManager honours
                // an explicit child height verbatim in the cross axis, so tiles
                // taller than their row spilled into the row below and the grid
                // looked like it was drawn on top of itself. Re-run once the
                // list has a real height instead.
                recyclerView?.doOnNextLayout { updateSize(slideOffset, rvWidth, rvPadding) }
                return
            }

            val collapsedSize =
                ((availHeight - (spanCount * marginBottomPx)) / spanCount)
                    .coerceAtLeast((24 * density).toInt())

            val effectiveWidth = if (rvWidth > 0) rvWidth else (recyclerView?.width ?: 0)
            val columnWidth = if (effectiveWidth > 0) {
                val spanCountExpanded = 3
                val totalMarginW = (spanCountExpanded - 1) * marginEndPx
                ((effectiveWidth - rvPadding - totalMarginW) / spanCountExpanded).toInt()
            } else collapsedSize

            val currentSize = (collapsedSize + (columnWidth - collapsedSize) * slideOffset).toInt()
            val finalSize = currentSize.coerceAtLeast(1)

            val lp = cardRoot.layoutParams as? android.view.ViewGroup.MarginLayoutParams
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

        private fun loadImage(
            font: FontEntity,
            imageView: android.widget.ImageView,
            shimmer: com.facebook.shimmer.ShimmerFrameLayout
        ) {
            val isDarkMode = imageView.context.isDarkModeEnabled()
            shimmer.startShimmerSoft(isDarkMode)

            if (isDarkMode) {
                imageView.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
            } else {
                imageView.clearColorFilter()
            }

            if (font.image_url.isEmpty()) {
                if (font.font_image?.isNotEmpty() == true) {
                    Glide.with(imageView.context)
                        .load(font.font_image)
                        .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean): Boolean = false
                            override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: Target<android.graphics.drawable.Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                                if (imageView.context.isDarkModeEnabled()) {
                                    imageView.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
                                }
                                return false
                            }
                        })
                        .into(imageView)
                    shimmer.hideShimmer()
                } else {
                    imageView.setImageResource(R.drawable.ic_font_thumbnail)
                    shimmer.hideShimmer()
                }
                return
            }

            val imgUrl = Constants.BASE_URL_GLIDE + font.image_url
            val isSvg = font.image_url.endsWith(".svg", ignoreCase = true)
            if (isSvg) {
                com.webscare.urducanvas.common.utils.SvgLoader.load(
                    url = imgUrl,
                    imageView = imageView,
                    scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main),
                    cachedXml = null,
                    maxPx = 1024,
                    applyWhiteTint = isDarkMode
                ) { _, _ ->
                    shimmer.stopShimmer()
                    shimmer.setShimmer(null)
                    if (imageView.context.isDarkModeEnabled()) {
                        imageView.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
                    }
                }
            } else {
                Glide.with(imageView.context)
                    .load(imgUrl)
                    .centerInside()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean): Boolean {
                            shimmer.stopShimmer(); shimmer.setShimmer(null); return false
                        }
                        override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: Target<android.graphics.drawable.Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                            if (imageView.context.isDarkModeEnabled()) {
                                imageView.setColorFilter(android.graphics.Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN)
                            }
                            shimmer.stopShimmer(); shimmer.setShimmer(null); return false
                        }
                    })
                    .into(imageView)
            }
        }
    }

    class Collapsed(
        private val binding: LayoutFontItemBinding,
        adapter: FontsAdapter,
        onFontSelected: (FontEntity, Boolean) -> Unit,
        onPreviewRequested: (FontEntity) -> Unit
    ) : FontViewHolder(binding.root, adapter, onFontSelected, onPreviewRequested) {
        override val cardRoot     get() = binding.root
        override val fontImage    get() = binding.font
        override val shimmer      get() = binding.shimmerLayout
        override val loadingAnim  get() = binding.loading
        override val premiumBadge get() = binding.isPremium
        override val downloadIcon get() = binding.download
    }

    class Expanded(
        private val binding: LayoutFontItemExpandedBinding,
        adapter: FontsAdapter,
        onFontSelected: (FontEntity, Boolean) -> Unit,
        onPreviewRequested: (FontEntity) -> Unit
    ) : FontViewHolder(binding.root, adapter, onFontSelected, onPreviewRequested) {
        override val cardRoot     get() = binding.root
        override val fontImage    get() = binding.font
        override val shimmer      get() = binding.shimmerLayout
        override val loadingAnim  get() = binding.loading
        override val premiumBadge get() = binding.isPremium
        override val downloadIcon get() = binding.download
        override val fontNameTextView get() = binding.fontName
        override val previewEye get() = binding.previewEye
    }

    class DiffCallback : DiffUtil.ItemCallback<FontEntity>() {
        override fun areItemsTheSame(oldItem: FontEntity, newItem: FontEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FontEntity, newItem: FontEntity) =
            oldItem == newItem
    }
}