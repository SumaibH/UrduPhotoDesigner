package com.webscare.urducanvas.ui.editor.panels.text.styles

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.common.utils.Utils.addPressEffectWithLongClick
import com.webscare.urducanvas.data.model.TextStylePreset

class TextStylesGridAdapter(
    private var presets: List<TextStylePreset>,
    /**
     * Asked to open the preview for a tile. One rule everywhere: long-press opens it
     * while the panel is collapsed, the eye button once there is room to draw one.
     * Declared before [onPresetClick] so that stays the last parameter and the
     * existing trailing-lambda call sites keep binding to it.
     */
    private val onPreviewRequested: (TextStylePreset) -> Unit = {},
    private val onPresetClick: (TextStylePreset) -> Unit
) : RecyclerView.Adapter<TextStylesGridAdapter.PresetViewHolder>() {

    var attachedRecyclerView: RecyclerView? = null
        private set

    /** Height the item sizes were last computed against, so we only re-lay out on a change. */
    private var lastMeasuredHeight: Int = 0

    /**
     * The first bind happens before the RecyclerView has been measured, so [updateSize] falls
     * back to its fixed default and the cards come out small — which is why the panel only
     * looked right after being reopened. Re-run the size pass as soon as a real height lands.
     */
    private val sizeOnLayout = View.OnLayoutChangeListener { v, _, top, _, bottom, _, oldTop, _, oldBottom ->
        val height = bottom - top
        if (height > 0 && height != lastMeasuredHeight && (bottom - top) != (oldBottom - oldTop)) {
            lastMeasuredHeight = height
            v.post {
                if (itemCount > 0) notifyDataSetChanged()
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        attachedRecyclerView = recyclerView
        lastMeasuredHeight = recyclerView.height
        recyclerView.addOnLayoutChangeListener(sizeOnLayout)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        recyclerView.removeOnLayoutChangeListener(sizeOnLayout)
        if (attachedRecyclerView == recyclerView) {
            attachedRecyclerView = null
        }
    }

    /**
     * Whether the panel drawing this grid is open to its full height.
     *
     * Both hosts -- the Styles page of Text Adjustments and 3D -> Presets -- live in the
     * adjustments sheet, which `EditorFragment.nonExpandableDestinations` locks collapsed,
     * so in practice this stays false and the tiles take the collapsed gesture. It is a
     * property rather than a constant so a host that *can* expand sets it and gets the eye
     * button, the same way [TextStylesMainAdapter] does.
     */
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
            if (old != value) {
                notifyDataSetChanged()
            }
        }

    var currentTypeface: Typeface? = null
        private set
    var currentFontKey: String? = null
        private set

    fun updateTypeface(typeface: Typeface?, fontKey: String?) {
        if (currentTypeface != typeface || currentFontKey != fontKey) {
            currentTypeface = typeface
            currentFontKey = fontKey
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_text_style_preset_item, parent, false)
        return PresetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        val preset = presets[position]
        holder.titleTxt.visibility = View.GONE
        holder.previewImg.visibility = View.VISIBLE

        val isNone = preset.id == TextStylePreset.NONE_ID
        // A null selection means no preset has been applied — which is exactly what the
        // None cell represents, so it carries the selection ring until something else does.
        val isSelected = preset.id == selectedPresetId || (isNone && selectedPresetId == null)
        val cardView = holder.itemView as? MaterialCardView
        if (cardView != null) {
            val strokePx = (1.5f * cardView.context.resources.displayMetrics.density + 0.5f).toInt()
            cardView.strokeWidth = if (isSelected) strokePx else 0
            cardView.strokeColor = ContextCompat.getColor(cardView.context, R.color.appColor)
        }

        val density = holder.itemView.resources.displayMetrics.density
        if (isNone) {
            holder.previewImg.setImageResource(R.drawable.ic_none)
            holder.previewImg.imageTintList = ContextCompat.getColorStateList(
                holder.itemView.context,
                if (isSelected) R.color.appColor else R.color.gray
            )
            // A glyph is not a thumbnail: at the preview's own 4dp padding the slashed
            // circle grew to the full cell and shouted over every real style beside it.
            val glyphPad = (13 * density).toInt()
            holder.previewImg.setPadding(glyphPad, glyphPad, glyphPad, glyphPad)
        } else {
            holder.previewImg.imageTintList = null
            val previewPad = (4 * density).toInt()
            holder.previewImg.setPadding(previewPad, previewPad, previewPad, previewPad)
            val bmp = TextStyleThumbnailRenderer.getCachedOrGenerateThumbnail(
                holder.itemView.context,
                preset,
                currentTypeface,
                currentFontKey
            )
            holder.previewImg.setImageBitmap(bmp)
        }

        holder.updateSize(attachedRecyclerView)

        // One rule everywhere: long-press opens the preview only while collapsed, and the
        // eye button is what does it once there is room to draw one. Tap is unchanged in
        // both states and carries no added delay -- the long-press helper starts its timer
        // on ACTION_DOWN and fires the click immediately on a short ACTION_UP.
        val select = {
            selectedPresetId = preset.id
            onPresetClick(preset)
        }
        if (isExpanded) {
            holder.itemView.addPressEffect { select() }
        } else {
            holder.itemView.addPressEffectWithLongClick(
                // "None" is the absence of a style -- there is nothing to look at closer.
                onLongClick = if (isNone) null else ({ onPreviewRequested(preset) }),
                onClick = { select() }
            )
        }

        // Drawn only when the panel is open: collapsed, the tile is small enough that the
        // button covers the thumbnail it is meant to let you look at, which is why the
        // collapsed state uses long-press instead. "None" never carries one -- it is the
        // absence of a style, and there is nothing to look at closer.
        holder.previewEye.apply {
            isVisible = isExpanded && !isNone
            addPressEffect { onPreviewRequested(preset) }
        }
    }

    override fun getItemCount(): Int = presets.size

    /** Swaps the visible presets, e.g. when the header's search filters them. */
    fun submitPresets(newPresets: List<TextStylePreset>) {
        presets = newPresets
        notifyDataSetChanged()
    }

    class PresetViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val previewEye: ImageView = view.findViewById(R.id.previewEye)
        val previewImg: ImageView = view.findViewById(R.id.presetPreviewImage)
        val titleTxt: TextView = view.findViewById(R.id.presetTitleText)

        fun updateSize(attachedRecyclerView: RecyclerView?) {
            val cardRoot = itemView as? MaterialCardView ?: return
            val context = cardRoot.context
            val density = context.resources.displayMetrics.density
            val recyclerView = (cardRoot.parent as? RecyclerView) ?: attachedRecyclerView

            val marginEndPx = (6 * density).toInt()
            val marginBottomPx = (6 * density).toInt()

            val lm = recyclerView?.layoutManager as? GridLayoutManager
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
                recyclerView?.doOnNextLayout { updateSize(attachedRecyclerView) }
                return
            }

            val computedCollapsedHeight =
                ((availHeight - (spanCountCollapsed * marginBottomPx)) / spanCountCollapsed).coerceAtLeast((24 * density).toInt())

            val finalSize = computedCollapsedHeight.coerceAtLeast(1)

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
}
