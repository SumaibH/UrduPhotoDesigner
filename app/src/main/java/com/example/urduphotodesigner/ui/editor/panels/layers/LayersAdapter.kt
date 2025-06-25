package com.example.urduphotodesigner.ui.editor.panels.layers

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.canvas.model.CanvasElement
import com.example.urduphotodesigner.common.canvas.enums.ElementType
import com.example.urduphotodesigner.databinding.LayoutLayersItemBinding

/**
 * Adapter for the layers list. Shows each CanvasElement with icon, title, lock badge, selection highlight,
 * and a "more options" overflow icon.
 *
 * Callbacks:
 *  - onLockToggle(element): when lock icon tapped.
 *  - onMoreOptions(element, anchorView): when overflow (more) icon tapped.
 *  - onItemClick(element): single-tap on row.
 *  - onItemLongClick(element): long-press on row (to enter multi-select).
 */
class LayersAdapter(
    private val onLockToggle: (element: CanvasElement) -> Unit,
    private val onMoreOptions: (element: CanvasElement, anchorView: View) -> Unit,
    private val onItemClick: (element: CanvasElement) -> Unit,
    private val onItemLongClick: (element: CanvasElement) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<LayersAdapter.CanvasElementViewHolder>() {

    private var elements = emptyList<CanvasElement>()
    private var inSelectionMode = false

    fun setSelectionMode(enabled: Boolean) {
        if (inSelectionMode != enabled) {
            inSelectionMode = enabled
            notifyDataSetChanged()
        }
    }

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

        @SuppressLint("ClickableViewAccessibility")
        fun bind(element: CanvasElement) {
            binding.apply {
                // Title: for TEXT use element.text, for IMAGE show "Sticker" or another label
                title.text = when (element.type) {
                    ElementType.TEXT -> element.text ?: "Text"
                    ElementType.IMAGE -> "Sticker"
                    else -> "Element"
                }

                // Icon based on type
                image.setImageResource(
                    when (element.type) {
                        ElementType.TEXT -> R.drawable.ic_text_layer
                        ElementType.IMAGE -> R.drawable.ic_image_layer
                        else -> R.drawable.ic_objects
                    }
                )

                if (element.isSelected) {
                    binding.root.strokeWidth = 2
                    binding.root.strokeColor =
                        ContextCompat.getColor(binding.root.context, R.color.appColor)
                } else {
                    binding.root.strokeWidth = 0
                }

                if (inSelectionMode) {
                    // hide per-item icons in multi-select
                    lock.visibility = View.GONE
                    options.visibility = View.GONE
                    // drag handle only on selected items
                    drag.visibility = if (element.isSelected) View.VISIBLE else View.GONE
                } else {
                    // normal mode: show icons based on locked state
                    lock.visibility = View.VISIBLE
                    options.visibility = View.VISIBLE
                    drag.visibility = if (element.isLocked) View.INVISIBLE else View.VISIBLE
                }


                // Lock icon
                lock.setImageResource(
                    if (element.isLocked) R.drawable.ic_lock else R.drawable.ic_unlock
                )
                // Drag handle visibility
                drag.visibility = if (element.isLocked) View.INVISIBLE else View.VISIBLE

                // Click listeners:
                lock.setOnClickListener {
                    element.isLocked = !element.isLocked
                    onLockToggle(element)
                }
                options.setOnClickListener { v ->
                    onMoreOptions(element, v)
                }
                drag.setOnTouchListener { v, event ->
                    // You could check event.action == MotionEvent.ACTION_DOWN
                    onStartDrag(this@CanvasElementViewHolder)
                    false
                }
                root.setOnClickListener {
                    onItemClick(element)
                }
                root.setOnLongClickListener {
                    onItemLongClick(element)
                    true
                }
            }
        }
    }
}