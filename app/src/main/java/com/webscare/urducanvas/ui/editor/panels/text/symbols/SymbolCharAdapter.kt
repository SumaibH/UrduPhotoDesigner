package com.webscare.urducanvas.ui.editor.panels.text.symbols

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.databinding.ItemSymbolCharChipBinding

data class CharChipModel(
    val index: Int,
    val displayText: String,
    val rawChar: String,
    val isSelected: Boolean
)

class SymbolCharAdapter(
    private val onCharSelected: (Int) -> Unit
) : RecyclerView.Adapter<SymbolCharAdapter.CharViewHolder>() {

    private val items = mutableListOf<CharChipModel>()

    /** Font of the element being edited, so chips read like the canvas does. */
    private var previewTypeface: Typeface? = null

    fun submitList(newItems: List<CharChipModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setPreviewTypeface(typeface: Typeface?) {
        if (previewTypeface == typeface) return
        previewTypeface = typeface
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharViewHolder {
        val binding = ItemSymbolCharChipBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CharViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CharViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CharViewHolder(private val binding: ItemSymbolCharChipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CharChipModel) {
            val ctx = binding.root.context
            binding.tvChar.text = item.displayText
            previewTypeface?.let { binding.tvChar.typeface = it }

            // Stroke-based state, like the filter tiles: the glyph keeps its own
            // colour so the letter stays readable when selected.
            val density = ctx.resources.displayMetrics.density
            binding.charCard.strokeWidth =
                if (item.isSelected) (2f * density).toInt() else 0
            binding.charCard.setCardBackgroundColor(
                ContextCompat.getColor(
                    ctx, if (item.isSelected) R.color.white else R.color.contrast
                )
            )
            binding.tvChar.setTextColor(ContextCompat.getColor(ctx, R.color.black))

            binding.charChipRoot.addPressEffect {
                onCharSelected(item.index)
            }
        }
    }
}
