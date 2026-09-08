package com.webscare.urducanvas.ui.editor.panels.text.symbols

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.databinding.ItemSymbolCardBinding

class SymbolGridAdapter(
    private val onSymbolTapped: (SymbolItem) -> Unit
) : RecyclerView.Adapter<SymbolGridAdapter.SymbolViewHolder>() {

    private val items = mutableListOf<SymbolItem>()

    fun submitList(newItems: List<SymbolItem>) {
        items.clear()
        items.addAll(newItems)
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
            // For combining diacritics, show with tatweel base (ـ) so it renders visibly
            val displayGlyph = if (item.isDiacritic && !item.glyph.startsWith("ـ")) {
                "ـ" + item.glyph
            } else {
                item.glyph
            }

            binding.tvSymbolGlyph.text = displayGlyph
            binding.tvSymbolName.text = item.name

            binding.symbolCardRoot.addPressEffect {
                onSymbolTapped(item)
            }
        }
    }
}
