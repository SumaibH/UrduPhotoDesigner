package com.example.urduphotodesigner.ui.editor.panels.text.colors

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.ColorItem
import com.example.urduphotodesigner.databinding.LayoutColorItemBinding

class ColorsAdapter(
    private val colorList: List<ColorItem>,
    private val onColorSelected: (ColorItem) -> Unit
) : RecyclerView.Adapter<ColorsAdapter.ColorViewHolder>() {

    // Keep track of the currently selected color
    var selectedColor: Int = Color.BLACK
        set(value) {
            // Only update if the color actually changed
            if (field != value) {
                val oldSelectedPosition = colorList.indexOfFirst { it.colorCode.toColorInt() == field }
                val newSelectedPosition = colorList.indexOfFirst { it.colorCode.toColorInt() == value }

                field = value // Update the backing field

                // Notify only the items whose selection state has changed
                if (oldSelectedPosition != -1) {
                    notifyItemChanged(oldSelectedPosition)
                }
                if (newSelectedPosition != -1 && newSelectedPosition != oldSelectedPosition) {
                    notifyItemChanged(newSelectedPosition)
                }
                // If the new color isn't in the list, and an old one was, just update the old one to deselect.
                // This handles cases where selection is cleared or set to a non-list color.
            }
        }

    inner class ColorViewHolder(val binding: LayoutColorItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(colorItem: ColorItem) {
            binding.colorView.setBackgroundColor(colorItem.colorCode.toColorInt())

            // Determine if the current item should be selected based on the adapter's selectedColor
            val isCurrentItemSelected = colorItem.colorCode.toColorInt() == selectedColor

            if (isCurrentItemSelected) {
                binding.root.strokeWidth = 4
                binding.root.setCardBackgroundColor(Color.WHITE)
                binding.root.strokeColor =
                    ContextCompat.getColor(binding.root.context, R.color.white)
            } else {
                binding.root.strokeWidth = 0
                binding.root.setCardBackgroundColor(Color.WHITE)
            }

            binding.root.setOnClickListener {
                // Invoke the callback to the ViewModel, which will update the selected color.
                // The ViewModel's LiveData will then trigger the observer in ColorsListFragment,
                // which in turn updates `selectedColor` in this adapter, leading to efficient
                // `notifyItemChanged` calls.
                onColorSelected.invoke(colorItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding =
            LayoutColorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ColorViewHolder(binding)
    }

    override fun getItemCount() = colorList.size

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colorList[position])
    }
}