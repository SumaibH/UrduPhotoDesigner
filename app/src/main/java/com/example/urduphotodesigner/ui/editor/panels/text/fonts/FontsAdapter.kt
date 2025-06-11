package com.example.urduphotodesigner.ui.editor.panels.text.fonts

import android.graphics.drawable.PictureDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.utils.Constants
import com.example.urduphotodesigner.data.model.FontEntity
import com.example.urduphotodesigner.databinding.LayoutFontItemBinding

class FontsAdapter(
    private val onFontSelected: (FontEntity, Boolean) -> Unit
) : ListAdapter<FontEntity, FontsAdapter.FontViewHolder>(DiffCallback()) {

    var selectedFontId: String? = null
        set(value) {
            val old = field
            field = value
            if (old != value) {
                old?.let { oldId ->
                    val oldPos = currentList.indexOfFirst { it.id.toString() == oldId }
                    if (oldPos != -1) notifyItemChanged(oldPos)
                }
                value?.let { newId ->
                    val newPos = currentList.indexOfFirst { it.id.toString() == newId }
                    if (newPos != -1) notifyItemChanged(newPos)
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val binding =
            LayoutFontItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FontViewHolder(private val binding: LayoutFontItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(font: FontEntity) {
            val isSelected = font.id.toString() == selectedFontId

            binding.root.strokeWidth = if (isSelected) 2 else 0
            binding.root.strokeColor = ContextCompat.getColor(
                binding.root.context,
                R.color.appColor
            )
//            binding.root.setCardBackgroundColor(
//                ContextCompat.getColor(
//                    binding.root.context,
//                    if (isSelected) R.color.selection else R.color.white
//                )
//            )

            // Download UI
            binding.download.visibility =
                if (font.is_downloaded || font.is_downloading) View.GONE else View.VISIBLE

            binding.progressBar.visibility =
                if (font.is_downloading) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                selectedFontId = font.id.toString()
                if (font.is_downloaded) {
                    onFontSelected(font, true)
                } else {
                    onFontSelected(font, false)
                }
            }

            // Load font preview
            val url = Constants.BASE_URL_GLIDE + font.image_url
            Glide.with(binding.root.context)
                .`as`(PictureDrawable::class.java)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .listener(object : RequestListener<PictureDrawable> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?, target: Target<PictureDrawable>,
                        isFirstResource: Boolean
                    ) = false.also { binding.progressBar.visibility = View.GONE }

                    override fun onResourceReady(
                        resource: PictureDrawable, model: Any, target: Target<PictureDrawable>?,
                        dataSource: DataSource, isFirstResource: Boolean
                    ) = false.also { binding.progressBar.visibility = View.GONE }
                })
                .into(binding.font)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FontEntity>() {
        override fun areItemsTheSame(oldItem: FontEntity, newItem: FontEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FontEntity, newItem: FontEntity) =
            oldItem == newItem
    }
}