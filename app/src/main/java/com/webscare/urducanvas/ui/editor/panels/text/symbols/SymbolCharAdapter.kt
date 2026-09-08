package com.webscare.urducanvas.ui.editor.panels.text.symbols

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
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

    fun submitList(newItems: List<CharChipModel>) {
        items.clear()
        items.addAll(newItems)
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

            if (item.isSelected) {
                binding.charChipRoot.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.appColor)
                )
                binding.tvChar.setTextColor(Color.WHITE)
                binding.selectedIndicator.visibility = View.VISIBLE
            } else {
                binding.charChipRoot.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.contrast)
                )
                binding.tvChar.setTextColor(
                    ContextCompat.getColor(ctx, R.color.black)
                )
                binding.selectedIndicator.visibility = View.GONE
            }

            binding.charChipRoot.addPressEffect {
                onCharSelected(item.index)
            }
        }
    }
}
