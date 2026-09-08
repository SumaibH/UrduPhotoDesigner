package com.webscare.urducanvas.ui.editor.panels.text.symbols

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
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

        fun bind(item: SymbolItem) {
            // Typeface first: the carrier choice below asks this very paint
            // whether it can draw the dotted circle.
            previewTypeface?.let { binding.tvSymbolGlyph.typeface = it }

            binding.tvSymbolGlyph.text = displayGlyphFor(item)

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
    }

    private companion object {
        const val TATWEEL = "ـ"
        const val DOTTED_CIRCLE = "◌"
    }
}
