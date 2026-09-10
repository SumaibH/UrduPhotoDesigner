package com.webscare.urducanvas.ui.editor.panels.text.symbols

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.util.TypedValue
import android.widget.Toast
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.databinding.ItemSymbolCardBinding

class SymbolGridAdapter(
    private val onSymbolTapped: (SymbolItem) -> Unit
) : RecyclerView.Adapter<SymbolGridAdapter.SymbolViewHolder>() {

    private val items = mutableListOf<SymbolItem>()

    /**
     * Typeface of the text element currently being edited. Previews are drawn in
     * it so a mark looks here exactly like it will look on the canvas — the
     * default app font shapes several of these very differently.
     */
    private var previewTypeface: Typeface? = null

    /**
     * Scratch paint for glyph-coverage checks. Kept off the TextView's own paint
     * so asking a question never leaves the view in a half-configured state.
     */
    private val probe = Paint()

    fun submitList(newItems: List<SymbolItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setPreviewTypeface(typeface: Typeface?) {
        if (previewTypeface == typeface) return
        previewTypeface = typeface
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolViewHolder {
        val binding = ItemSymbolCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SymbolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SymbolViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SymbolViewHolder(private val binding: ItemSymbolCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /** The tile's own font, kept as the fallback for glyphs the element's font lacks. */
        private val defaultTypeface: Typeface = binding.tvSymbolGlyph.typeface ?: Typeface.DEFAULT

        fun bind(item: SymbolItem) {
            // Typeface first: the carrier choice below asks this very paint
            // whether it can draw the dotted circle.
            binding.tvSymbolGlyph.typeface = previewTypefaceFor(item)

            binding.tvSymbolGlyph.text = displayGlyphFor(item)
            fitGlyphToTile(displayGlyphFor(item))

            // The tile carries no caption any more, so the name lives on long
            // press — the same affordance the rail categories use.
            binding.symbolCardRoot.contentDescription = item.name
            binding.symbolCardRoot.setOnLongClickListener {
                Toast.makeText(binding.root.context, item.name, Toast.LENGTH_SHORT).show()
                true
            }

            binding.symbolCardRoot.addPressEffect {
                onSymbolTapped(item)
            }
        }

        /**
         * The element's font, but only when it can actually draw the symbol.
         *
         * Most display Urdu fonts cover the aeraab and nothing else, so the
         * Quranic stop signs, the honorific ligatures and the ornaments all
         * rendered as blank tiles — the grid looked broken while Above and Below
         * looked fine. Falling back to the default typeface for those draws the
         * glyph from the system font instead of drawing nothing.
         */
        private fun previewTypefaceFor(item: SymbolItem): Typeface {
            val preview = previewTypeface ?: return defaultTypeface
            probe.typeface = preview
            return if (probe.hasGlyph(item.glyph)) preview else defaultTypeface
        }

        /**
         * A combining mark has no width of its own, so it needs a carrier to be
         * visible at all.
         *
         * U+25CC DOTTED CIRCLE is the convention for showing a mark in
         * isolation and puts it where it actually sits on a letter. Tatweel is
         * the fallback: it is a low baseline dash, so a mark drawn on it floats
         * in empty space and reads as a speck.
         */
        private fun displayGlyphFor(item: SymbolItem): String {
            if (!item.isDiacritic) return item.glyph
            val paint = binding.tvSymbolGlyph.paint
            val carrier = if (paint.hasGlyph(DOTTED_CIRCLE)) DOTTED_CIRCLE else TATWEEL
            return carrier + item.glyph
        }

        /**
         * Sizes the glyph so its *ink* fits the tile.
         *
         * The tile used to rely on TextView autosize, which measures the advance width
         * the font reports. That is the right measure for a run of text and the wrong
         * one for these: the honorific ligatures — Bismillah and Muhammad worst of all —
         * report a modest advance and then draw far outside it, so autosize saw
         * something that fitted while the tile clipped both ends of it. Lowering the
         * autosize floor changed nothing for exactly that reason.
         *
         * getTextBounds returns the inked rectangle, so measuring with that and stepping
         * down until it fits is the measure that matches what the user sees.
         */
        private fun fitGlyphToTile(glyph: String) {
            val tv = binding.tvSymbolGlyph
            // Autosize and an explicit text size are mutually exclusive; setTextSize is
            // silently ignored while autosize owns the view.
            TextViewCompat.setAutoSizeTextTypeWithDefaults(
                tv, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
            )

            val density = tv.resources.displayMetrics.density
            val availW = (TILE_DP - 2 * TILE_INSET_DP) * density
            val availH = availW

            probe.typeface = tv.typeface
            var sizePx = MAX_GLYPH_SP * density
            val minPx = MIN_GLYPH_SP * density
            val ink = Rect()
            while (sizePx > minPx) {
                probe.textSize = sizePx
                probe.getTextBounds(glyph, 0, glyph.length, ink)
                if (ink.width() <= availW && ink.height() <= availH) break
                sizePx -= density   // 1dp per step
            }
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx)
        }
    }

    private companion object {
        const val TATWEEL = "ـ"
        const val DOTTED_CIRCLE = "◌"

        /** Matches the card in item_symbol_card.xml, and its horizontal padding. */
        const val TILE_DP = 52f
        const val TILE_INSET_DP = 2f

        const val MAX_GLYPH_SP = 28f
        const val MIN_GLYPH_SP = 6f
    }
}
