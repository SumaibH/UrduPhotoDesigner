package com.example.urduphotodesigner.ui.editor.panels.layers

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.model.CanvasElement
import com.example.urduphotodesigner.common.canvas.enums.ElementType
import com.example.urduphotodesigner.databinding.LayoutLayersItemBinding

class LayersAdapter(
    private val onLockToggle: (element: CanvasElement) -> Unit,
    private val onRemove: (element: CanvasElement) -> Unit,
    private val onItemClick: (CanvasElement) -> Unit
    ) : RecyclerView.Adapter<LayersAdapter.CanvasElementViewHolder>() {

        private var elements = emptyList<CanvasElement>()

        fun submitList(newElements: List<CanvasElement>) {
            elements = newElements
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CanvasElementViewHolder {
            val binding = LayoutLayersItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return CanvasElementViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CanvasElementViewHolder, position: Int) {
            holder.bind(elements[position])
        }

        override fun getItemCount(): Int = elements.size

        inner class CanvasElementViewHolder(
            private val binding: LayoutLayersItemBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(element: CanvasElement) {
                binding.apply {
                    // Set element name/type
                    title.text = when (element.type) {
                        ElementType.TEXT -> element.text
                        ElementType.IMAGE -> "Sticker"
                        else -> "Element"
                    }

                    image.setImageResource(
                        when (element.type) {
                            ElementType.TEXT -> R.drawable.ic_text_layer
                            ElementType.IMAGE -> R.drawable.ic_image_layer
                            else -> R.drawable.ic_objects
                        }
                    )

                    root.setBackgroundColor(if (element.isSelected) Color.LTGRAY else Color.TRANSPARENT)

                    // Set lock state
                    lock.setImageResource(
                        if (element.isLocked) {
                            R.drawable.ic_lock
                        } else {
                            R.drawable.ic_unlock
                        }
                    )

                    // Set up click listeners
                    lock.setOnClickListener {
                        element.isLocked = !element.isLocked
                        onLockToggle(element)
                    }

                    delete.setOnClickListener {
                        onRemove(element)
                    }

                    root.setOnClickListener {
                        onItemClick(element)
                    }

                    // Disable drag handle if element is locked
                    drag.visibility = if (element.isLocked) View.INVISIBLE else View.VISIBLE
                }
            }
        }
    }