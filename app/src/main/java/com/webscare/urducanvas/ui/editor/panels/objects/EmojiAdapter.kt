package com.webscare.urducanvas.ui.editor.panels.objects

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.canvas.model.EmojiMeta
import com.webscare.urducanvas.common.utils.EmojiBitmapRenderer
import com.webscare.urducanvas.common.utils.onBoxResized
import com.webscare.urducanvas.common.utils.removeBoxResizedWatcher
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.databinding.ItemEmojiBinding
import com.webscare.urducanvas.databinding.ItemEmojiExpandedBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmojiAdapter(
    private val context: Context,
    initialEmojis: List<EmojiMeta>,
    private val onEmojiClicked: (Bitmap) -> Unit,
    private val onEmojiLongPress: ((EmojiMeta) -> Unit)? = null,
    /** Long-press while collapsed, eye button while expanded. See FontsAdapter. */
    private val onPreviewRequested: (EmojiMeta) -> Unit = {}
) : RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder>() {

    companion object {
        const val TYPE_COLLAPSED    = 0
        const val TYPE_EXPANDED     = 1
        const val PAYLOAD_SELECTION = "emoji_selection_changed"

        /** What a tap renders at — big enough to scale up on the canvas. */
        const val PREVIEW_RENDER_PX = 512
    }

    private val emojis = initialEmojis.toMutableList()

    private val selectionShadow = mutableMapOf<String, Boolean>()
    fun isEmojiSelected(char: String): Boolean = selectionShadow[char] == true

    var isInMultiSelectMode: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    fun updateSelectionForChar(char: String, isSelected: Boolean) {
        selectionShadow[char] = isSelected
        val position = emojis.indexOfFirst { it.char == char }
        if (position >= 0) notifyItemChanged(position, PAYLOAD_SELECTION)
    }

    var attachedRecyclerView: androidx.recyclerview.widget.RecyclerView? = null
        private set

    /** See [com.webscare.urducanvas.common.utils.onBoxResized]. */
    private var boxWatcher: android.view.View.OnLayoutChangeListener? = null

    override fun onAttachedToRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
        boxWatcher = recyclerView.onBoxResized { rv -> resizeVisibleTiles(rv) }
    }

    override fun onDetachedFromRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerView.removeBoxResizedWatcher(boxWatcher)
        boxWatcher = null
        if (attachedRecyclerView == recyclerView) {
            attachedRecyclerView = null
        }
    }

    /** Re-measures every tile now that [rv] has been laid out at its settled height. */
    private fun resizeVisibleTiles(rv: androidx.recyclerview.widget.RecyclerView) {
        val rvPadding = rv.paddingLeft + rv.paddingRight
        recyclerViewWidth = rv.width
        recyclerViewPadding = rvPadding
        for (i in 0 until rv.childCount) {
            val holder = rv.getChildViewHolder(rv.getChildAt(i)) as? EmojiViewHolder ?: continue
            holder.updateSize(slideOffset, rv.width, rvPadding)
        }
    }

    fun clearSelectionShadow() {
        if (selectionShadow.isEmpty()) return
        selectionShadow.clear()
        notifyDataSetChanged()
    }

    var isExpanded: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    // Per-frame morph data forwarded by the fragment
    var slideOffset: Float = 0f
    var recyclerViewWidth: Int = 0
    var recyclerViewPadding: Int = 0

    fun updateData(newList: List<EmojiMeta>) {
        if (emojis.isEmpty()) {
            emojis.addAll(newList)
            notifyDataSetChanged()
            return
        }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = emojis.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                emojis[oldPos].char == newList[newPos].char
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                emojis[oldPos] == newList[newPos]
        })
        emojis.clear()
        emojis.addAll(newList)
        diff.dispatchUpdatesTo(this)
    }

    fun getCurrentEmojis(): List<EmojiMeta> = emojis.toList()

    /**
     * Renders [meta] and hands it to the panel — the tile's own tap path, lifted
     * out of the view holder so the asset preview's "Add to canvas" runs the same
     * code rather than a second copy of it.
     */
    suspend fun selectEmoji(meta: EmojiMeta) {
        val bitmap = withContext(Dispatchers.IO) {
            EmojiBitmapRenderer.render(context, meta.char, sizePx = PREVIEW_RENDER_PX)
        }
        onEmojiClicked(bitmap)
    }

    // getPaint() is no longer used — EmojiBitmapRenderer handles rendering
    fun getPaint(): android.graphics.Paint? = null

    override fun getItemViewType(position: Int): Int =
        if (isExpanded) TYPE_EXPANDED else TYPE_COLLAPSED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojiViewHolder =
        if (viewType == TYPE_EXPANDED) {
            EmojiViewHolder.Expanded(
                ItemEmojiExpandedBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ),
                adapter        = this,
                onEmojiClicked = onEmojiClicked,
                onLongPress    = onEmojiLongPress,
                onPreviewRequested = onPreviewRequested
            )
        } else {
            EmojiViewHolder.Collapsed(
                ItemEmojiBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ),
                adapter        = this,
                onEmojiClicked = onEmojiClicked,
                onLongPress    = null,   // no selection in collapsed
                onPreviewRequested = onPreviewRequested
            )
        }

    override fun onBindViewHolder(holder: EmojiViewHolder, position: Int) {
        val emoji = emojis[position]
        holder.bind(emoji, isEmojiSelected(emoji.char), isInMultiSelectMode)
    }

    override fun onBindViewHolder(
        holder: EmojiViewHolder, position: Int, payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) { onBindViewHolder(holder, position); return }
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.updateSelectionOnly(
                isEmojiSelected(emojis[position].char), isInMultiSelectMode
            )
        }
    }

    override fun getItemCount(): Int = emojis.size

    // ── ViewHolder ────────────────────────────────────────────────────────────

    sealed class EmojiViewHolder(
        itemView: View,
        private val adapter: EmojiAdapter,
        private val onEmojiClicked: (Bitmap) -> Unit,
        private val onLongPress: ((EmojiMeta) -> Unit)?,
        private val onPreviewRequested: (EmojiMeta) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        protected abstract val emojiText: android.widget.TextView

        // Both collapsed and expanded now have a loading anim
        protected abstract val loadingAnim: com.airbnb.lottie.LottieAnimationView

        protected open val selectionIcon: android.widget.ImageView? get() = null
        protected open val cardRoot: com.google.android.material.card.MaterialCardView? get() = null

        /** Only the expanded tile carries one; collapsed opens on long-press. */
        protected open val previewEye: android.widget.ImageView? get() = null

        private var renderJob: Job? = null
        private var boundEmoji: EmojiMeta? = null

        fun bind(emoji: EmojiMeta, isSelected: Boolean, inMultiSelectMode: Boolean) {
            boundEmoji = emoji
            emojiText.text = emoji.char
            loadingAnim.isVisible = false   // reset on rebind
            cardRoot?.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.selection))
            updateSelectionOnly(isSelected, inMultiSelectMode)
            updateSize(adapter.slideOffset, adapter.recyclerViewWidth, adapter.recyclerViewPadding)
            wireClicks(emoji)
        }

        fun updateSelectionOnly(isSelected: Boolean, inMultiSelectMode: Boolean) {
            selectionIcon?.apply {
                visibility = if (inMultiSelectMode) View.VISIBLE else View.GONE
                setImageResource(
                    if (isSelected) R.drawable.ic_selected_radio
                    else R.drawable.ic_unselected_radio
                )
            }
            val strokePx = (1.5f * itemView.context.resources.displayMetrics.density + 0.5f).toInt()
            cardRoot?.strokeWidth = if (isSelected) strokePx else 0
            if (isSelected) {
                cardRoot?.strokeColor =
                    ContextCompat.getColor(itemView.context, R.color.appColor)
            }
        }

        private fun wireClicks(emoji: EmojiMeta) {
            val tap = {
                val current = boundEmoji ?: emoji
                if (adapter.isInMultiSelectMode) {
                    onLongPress?.invoke(current)
                } else {
                    renderJob?.cancel()
                    renderJob = itemView.findViewTreeLifecycleOwner()
                        ?.lifecycleScope?.launch { renderAndDeliver(current) }
                }
            }
            itemView.addPressEffect { tap() }
            itemView.setOnLongClickListener {
                val current = boundEmoji ?: emoji
                // Expanded keeps multi-select on long-press; collapsed passes null for
                // onLongPress, so the gesture is free there and opens the preview.
                if (adapter.isExpanded) onLongPress?.invoke(current)
                else onPreviewRequested(current)
                true
            }
            previewEye?.apply {
                isVisible = true
                addPressEffect { onPreviewRequested(boundEmoji ?: emoji) }
            }
        }

        fun updateSize(slideOffset: Float, rvWidth: Int, rvPadding: Int) {
            val context = itemView.context
            val density = context.resources.displayMetrics.density
            val recyclerView = (itemView.parent as? androidx.recyclerview.widget.RecyclerView) ?: adapter.attachedRecyclerView

            val marginEndPx = (6 * density).toInt()
            val marginBottomPx = (6 * density).toInt()

            val lm = recyclerView?.layoutManager as? androidx.recyclerview.widget.GridLayoutManager
            val spanCount = lm?.spanCount?.coerceAtLeast(1) ?: 3

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
                ((availHeight - (spanCount * marginBottomPx)) / spanCount).coerceAtLeast((24 * density).toInt())

            val collapsedSize = computedCollapsedHeight

            val effectiveWidth = if (rvWidth > 0) rvWidth else (recyclerView?.width ?: 0)
            val columnWidth = if (effectiveWidth > 0) {
                val totalMarginW = 18 * density
                ((effectiveWidth - rvPadding - totalMarginW) / 3).toInt()
            } else collapsedSize

            val currentSize = (collapsedSize + (columnWidth - collapsedSize) * slideOffset).toInt()
            val finalSize = currentSize.coerceAtLeast(1)

            val lp = itemView.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            if (lp != null) {
                if (lp.width != finalSize || lp.height != finalSize || lp.rightMargin != marginEndPx || lp.bottomMargin != marginBottomPx) {
                    lp.width = finalSize
                    lp.height = finalSize
                    lp.rightMargin = marginEndPx
                    lp.bottomMargin = marginBottomPx
                    itemView.layoutParams = lp
                }
            }
        }

        private suspend fun renderAndDeliver(emoji: EmojiMeta) {
            // Show spinner, yield to let frame commit so spinner is visible
            loadingAnim.isVisible = true
            kotlinx.coroutines.yield()

            val ctx = itemView.context
            val bmp = withContext(Dispatchers.IO) {
                EmojiBitmapRenderer.render(ctx, emoji.char, sizePx = 512)
            }

            loadingAnim.isVisible = false
            onEmojiClicked(bmp)
        }

        // ── Subtypes ──────────────────────────────────────────────────────────

        class Collapsed(
            private val binding: ItemEmojiBinding,
            adapter: EmojiAdapter,
            onEmojiClicked: (Bitmap) -> Unit,
            onLongPress: ((EmojiMeta) -> Unit)?,
            onPreviewRequested: (EmojiMeta) -> Unit
        ) : EmojiViewHolder(binding.root, adapter, onEmojiClicked, onLongPress, onPreviewRequested) {
            override val emojiText   get() = binding.emojiText
            // Now has loadingAnim from updated item_emoji.xml layout
            override val loadingAnim get() = binding.loading
            override val cardRoot    get() = binding.root
        }

        class Expanded(
            private val binding: ItemEmojiExpandedBinding,
            adapter: EmojiAdapter,
            onEmojiClicked: (Bitmap) -> Unit,
            onLongPress: ((EmojiMeta) -> Unit)?,
            onPreviewRequested: (EmojiMeta) -> Unit
        ) : EmojiViewHolder(binding.root, adapter, onEmojiClicked, onLongPress, onPreviewRequested) {
            override val emojiText     get() = binding.emojiText
            override val loadingAnim   get() = binding.loading
            override val selectionIcon get() = binding.selection
            override val cardRoot      get() = binding.root
            override val previewEye    get() = binding.previewEye
        }
    }
}