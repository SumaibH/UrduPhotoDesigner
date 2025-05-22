package com.example.urduphotodesigner.ui.editor.panels.background.gradients

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.recyclerview.widget.RecyclerView
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.GradientItem
import com.example.urduphotodesigner.databinding.LayoutColorItemBinding

class GradientsAdapter(
    private val gradientList: List<GradientItem>,
    private val onGradientSelected: (Bitmap) -> Unit
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
                val bitmap = if (gradientDrawable is BitmapDrawable) {
                    gradientDrawable.bitmap
                } else {
                    val width = if (gradientDrawable.intrinsicWidth > 0) gradientDrawable.intrinsicWidth else 100
                    val height = if (gradientDrawable.intrinsicHeight > 0) gradientDrawable.intrinsicHeight else 100
                    val config = Bitmap.Config.ARGB_8888
                    val bmp = createBitmap(width, height, config)
                    val canvas = Canvas(bmp)
                    gradientDrawable.setBounds(0, 0, canvas.width, canvas.height)
                    gradientDrawable.draw(canvas)
                    bmp
                }
                gradientList.forEach { it.isSelected = false }
                item.isSelected = true
                notifyDataSetChanged()
                onGradientSelected(bitmap)
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
