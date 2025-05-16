package com.example.urduphotodesigner.ui.editor.panels.text.colors

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.data.model.ColorItem
import com.example.urduphotodesigner.databinding.LayoutColorItemBinding

class ColorsAdapter(private val colorList: List<ColorItem>,
                    private val onColorSelected: (ColorItem) -> Unit
) :
    RecyclerView.Adapter<ColorsAdapter.ColorViewHolder>() {

    inner class ColorViewHolder(val binding: LayoutColorItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(colorItem: ColorItem) {

            binding.colorView.setBackgroundColor(colorItem.colorCode.toColorInt())
            if (colorItem.isSelected) {
                binding.root.strokeWidth = 4
                binding.root.setCardBackgroundColor(Color.WHITE)
                binding.root.strokeColor =
                    ContextCompat.getColor(binding.root.context, R.color.appColor)
            } else {
                binding.root.strokeWidth = 0
                binding.root.setCardBackgroundColor(Color.WHITE)
            }

            binding.root.setOnClickListener {
                colorList.forEach { it.isSelected = false }
                colorItem.isSelected = true
                notifyDataSetChanged()
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
