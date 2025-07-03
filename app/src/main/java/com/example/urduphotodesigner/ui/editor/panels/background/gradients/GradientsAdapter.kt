package com.example.urduphotodesigner.ui.editor.panels.background.gradients

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.model.GradientItem
import com.example.urduphotodesigner.databinding.LayoutColorItemBinding
import com.example.urduphotodesigner.databinding.LayoutColorPickerItemBinding

class GradientsAdapter(
    private var gradientList: List<GradientItem>,
    private val onGradientSelected: (Bitmap, GradientItem) -> Unit,
    private val onGradientEditSelected: (Bitmap, GradientItem) -> Unit,
    private val onNoneSelected: () -> Unit,
    private val onGradientPickerClicked: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION

    var selectedItem: GradientItem? = null
        set(value) {
            field = value
            gradientList.forEach { it.isSelected = (it == value) }
            notifyDataSetChanged()
        }

    companion object {
        private const val VIEW_TYPE_NONE = 0
        private const val VIEW_TYPE_PICKER = 1
        private const val VIEW_TYPE_GRADIENT = 2
    }

    inner class GradientViewHolder(val binding: LayoutColorItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: GradientItem, isSelected: Boolean) {
//            binding.colorView.doOnLayout {
//                val w = it.width
//                val h = it.height
//                val drawable = item.createGradientPreviewDrawable(
//                    item,
//                    width = w,
//                    height = h
//                )
//                it.background = drawable
//            }
            binding.colorView.background = GradientDrawable(
                GradientDrawable.Orientation.RIGHT_LEFT,
                item.colors.toIntArray()
            )
            val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, item.colors.toIntArray())

            if (isSelected) {
                binding.root.strokeWidth = 4
                binding.root.setCardBackgroundColor(Color.WHITE)
                binding.edit.visibility = View.VISIBLE
                binding.root.strokeColor = ContextCompat.getColor(binding.root.context, R.color.appColor)
            } else {
                binding.root.strokeWidth = 0
                binding.edit.visibility = View.GONE
                binding.root.setCardBackgroundColor(Color.WHITE)
            }

            binding.root.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = adapterPosition

                val width = if (gradientDrawable.intrinsicWidth > 0) gradientDrawable.intrinsicWidth else 100
                val height = if (gradientDrawable.intrinsicHeight > 0) gradientDrawable.intrinsicHeight else 100
                val drawable = item.createGradientPreviewDrawable(
                    item,
                    width = width,
                    height = height
                )
                val bmp = createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)

                if (binding.edit.isVisible){
                    onGradientEditSelected(bmp, item)
                    selectedItem = item
                }else{
                    onGradientSelected(bmp, item)
                    selectedItem = item
                }
                if (previousSelected != RecyclerView.NO_POSITION && previousSelected != selectedPosition) {
                    notifyItemChanged(previousSelected)
                }
                notifyItemChanged(selectedPosition)
            }
        }
    }

    inner class NoneViewHolder(val binding: LayoutColorPickerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.colorView.setImageResource(R.drawable.ic_none) // your "none" icon
            binding.root.setOnClickListener {
                val prevSelected = selectedPosition
                selectedPosition = RecyclerView.NO_POSITION
                if (prevSelected != RecyclerView.NO_POSITION) notifyItemChanged(prevSelected)
                notifyItemChanged(adapterPosition)
                onNoneSelected()
            }
        }
    }

    inner class PickerViewHolder(val binding: LayoutColorPickerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.colorView.setImageResource(R.drawable.ic_add)
            binding.root.setOnClickListener {
                onGradientPickerClicked()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> VIEW_TYPE_NONE
            1 -> VIEW_TYPE_PICKER
            else -> VIEW_TYPE_GRADIENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_NONE -> {
                val binding = LayoutColorPickerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                NoneViewHolder(binding)
            }
            VIEW_TYPE_PICKER -> {
                val binding = LayoutColorPickerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                PickerViewHolder(binding)
            }
            else -> {
                val binding = LayoutColorItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                GradientViewHolder(binding)
            }
        }
    }

    override fun getItemCount() = gradientList.size + 2 // +2 for "none" and "picker"

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is GradientViewHolder && position >= 2) {
            val gradientItem = gradientList[position - 2]
            holder.bind(gradientItem, position == selectedPosition)
        }
    }

    fun updateList(new:List<GradientItem>){
        gradientList = new
        notifyDataSetChanged()
    }
}