package com.example.urduphotodesigner.ui.editor.panels.background.gradients

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.data.model.GradientItem
import com.example.urduphotodesigner.databinding.LayoutColorItemBinding

class GradientsAdapter(
    private val gradientList: List<GradientItem>,
    private val onGradientSelected: (GradientItem) -> Unit
) : RecyclerView.Adapter<GradientsAdapter.GradientViewHolder>() {

    inner class GradientViewHolder(val binding: LayoutColorItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GradientItem) {
            val gradientDrawable = GradientDrawable(item.orientation, item.colors.toIntArray())
            binding.colorView.background = gradientDrawable

            if (item.isSelected) {
                binding.root.strokeWidth = 4
                binding.root.setCardBackgroundColor(Color.WHITE)
                binding.root.strokeColor =
                    ContextCompat.getColor(binding.root.context, R.color.appColor)
            } else {
                binding.root.strokeWidth = 0
                binding.root.setCardBackgroundColor(Color.WHITE)
            }

            binding.root.setOnClickListener {
                gradientList.forEach { it.isSelected = false }
                item.isSelected = true
                notifyDataSetChanged()
                onGradientSelected(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GradientViewHolder {
        val binding =
            LayoutColorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GradientViewHolder(binding)
    }

    override fun getItemCount() = gradientList.size

    override fun onBindViewHolder(holder: GradientViewHolder, position: Int) {
        holder.bind(gradientList[position])
    }
}
