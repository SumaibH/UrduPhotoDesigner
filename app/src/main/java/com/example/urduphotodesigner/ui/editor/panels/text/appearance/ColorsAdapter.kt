package com.example.urduphotodesigner.ui.editor.panels.text.appearance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.model.ColorItem
import com.example.urduphotodesigner.databinding.LayoutColorItemBinding
import com.example.urduphotodesigner.databinding.LayoutColorPickerItemBinding // Assuming you create this layout

class ColorsAdapter(
    private val colorList: List<ColorItem>,
    private val onColorSelected: (ColorItem) -> Unit,
    private val onNoneSelected: () -> Unit,
    private val onColorPickerClicked: () -> Unit // New callback for color picker
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() { // Change base class to RecyclerView.ViewHolder

    // Define view types
    private val VIEW_TYPE_COLOR_PICKER = 0
    private val VIEW_TYPE_COLOR_ITEM = 1
    private val VIEW_TYPE_NONE = 2

    var selectedColor: Int = Color.BLACK
        set(value) {
            if (field != value) {
                val oldSelectedPosition = colorList.indexOfFirst { it.colorCode.toColorInt() == field }
                val newSelectedPosition = colorList.indexOfFirst { it.colorCode.toColorInt() == value }

                field = value

                if (oldSelectedPosition != -1) {
                    notifyItemChanged(oldSelectedPosition + 1) // +1 because of color picker at pos 0
                }
                if (newSelectedPosition != -1 && newSelectedPosition != oldSelectedPosition) {
                    notifyItemChanged(newSelectedPosition + 1) // +1 because of color picker at pos 0
                }
            }
        }

    inner class ColorViewHolder(val binding: LayoutColorItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(colorItem: ColorItem) {
            binding.colorView.setBackgroundColor(colorItem.colorCode.toColorInt())

            val isCurrentItemSelected = colorItem.colorCode.toColorInt() == selectedColor

            if (isCurrentItemSelected) {
                binding.root.strokeWidth = 4
                binding.root.setCardBackgroundColor(Color.WHITE)
                binding.root.strokeColor =
                    ContextCompat.getColor(binding.root.context, R.color.appColor)
            } else {
                binding.root.strokeWidth = 0
                binding.root.setCardBackgroundColor(Color.WHITE)
            }

            binding.root.setOnClickListener {
                onColorSelected.invoke(colorItem)
            }
        }
    }

    inner class ColorPickerViewHolder(val binding: LayoutColorPickerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onColorPickerClicked.invoke()
            }
        }
    }

    inner class NoneViewHolder(val binding: LayoutColorPickerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.colorView.setImageResource(R.drawable.ic_none)
            binding.root.setOnClickListener {
                onNoneSelected.invoke()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_NONE else if (position==1) VIEW_TYPE_COLOR_PICKER else VIEW_TYPE_COLOR_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_COLOR_PICKER) {
            val binding = LayoutColorPickerItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            ColorPickerViewHolder(binding)
        } else if (viewType == VIEW_TYPE_NONE){
            val binding =
                LayoutColorPickerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            NoneViewHolder(binding)
        }else {
            val binding =
                LayoutColorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ColorViewHolder(binding)
        }
    }

    override fun getItemCount() = colorList.size + 1 // +1 for the color picker

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == VIEW_TYPE_COLOR_ITEM) {
            (holder as ColorViewHolder).bind(colorList[position - 1]) // -1 because of color picker at pos 0
        }
        // No binding needed for the color picker as it's static
    }
}