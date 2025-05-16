package com.example.urduphotodesigner.ui.editor.panels.text.fonts

import android.graphics.Color
import android.graphics.drawable.PictureDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.urduphotodesigner.R
import com.example.urduphotodesigner.common.Constants
import com.example.urduphotodesigner.data.model.ColorItem
import com.example.urduphotodesigner.data.model.FontEntity
import com.example.urduphotodesigner.databinding.LayoutFontItemBinding

class FontsAdapter(
    private val onFontSelected: (FontEntity) -> Unit
) : RecyclerView.Adapter<FontsAdapter.FontViewHolder>() {

    private val fonts = mutableListOf<FontEntity>()

    fun submitList(newList: List<FontEntity>) {
        fonts.clear()
        fonts.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val binding =
            LayoutFontItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(fonts[position])
    }

    override fun getItemCount(): Int = fonts.size

    inner class FontViewHolder(private val binding: LayoutFontItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(font: FontEntity) {
            if (font.is_selected) {
                binding.root.strokeWidth = 4
                binding.root.setCardBackgroundColor(Color.WHITE)
                binding.root.strokeColor =
                    ContextCompat.getColor(binding.root.context, R.color.appColor)
            } else {
                binding.root.strokeWidth = 0
                binding.root.setCardBackgroundColor(Color.WHITE)
            }

            binding.root.setOnClickListener {
                fonts.forEach { it.is_selected = false }
                font.is_selected = true
                notifyDataSetChanged()
                onFontSelected.invoke(font)
            }

            val url = Constants.BASE_URL_GLIDE+font.image_url
            Glide.with(binding.root.context)
                .`as`(PictureDrawable::class.java)
                .load(url)
                .listener(object : RequestListener<PictureDrawable> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<PictureDrawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("GlideDebug", "Image load failed: ${e?.message}")
                        binding.progressBar.visibility = View.GONE
                        // Return false to let Glide handle error placeholder
                        return false
                    }

                    override fun onResourceReady(
                        resource: PictureDrawable,
                        model: Any,
                        target: Target<PictureDrawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("GlideDebug", "Image loaded successfully from: $url")
                        binding.progressBar.visibility = View.GONE
                        // Return false to let Glide set the image on ImageView
                        return false
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .skipMemoryCache(false)
                .into(binding.font)

        }
    }
}
