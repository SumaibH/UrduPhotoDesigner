package com.example.urduphotodesigner.ui.editor.panels.background.backgrounds

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import com.example.urduphotodesigner.common.utils.Constants
import com.example.urduphotodesigner.data.model.ImageEntity
import com.example.urduphotodesigner.databinding.LayoutImagesItemBinding
import androidx.core.graphics.createBitmap

class ImagesAdapter(
    private val onImageSelected: (Bitmap) -> Unit
) : RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

    private val images = mutableListOf<ImageEntity>()

    fun submitList(newList: List<ImageEntity>) {
        images.clear()
        images.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding =
            LayoutImagesItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    inner class ImageViewHolder(private val binding: LayoutImagesItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentDrawable: Drawable? = null

        fun bind(image: ImageEntity) {
            if (image.is_selected) {
                binding.root.strokeWidth = 4
                binding.root.setCardBackgroundColor(Color.WHITE)
                binding.root.strokeColor =
                    ContextCompat.getColor(binding.root.context, R.color.appColor)
            } else {
                binding.root.strokeWidth = 0
                binding.root.setCardBackgroundColor(Color.WHITE)
            }

            binding.root.setOnClickListener {
                // Only proceed if image is loaded
                currentDrawable?.let { drawable ->
                    images.forEach { it.is_selected = false }
                    image.is_selected = true
                    notifyDataSetChanged()

                    val bitmap = when (drawable) {
                        is BitmapDrawable -> drawable.bitmap
                        else -> {
                            val width = drawable.intrinsicWidth.coerceAtLeast(1)
                            val height = drawable.intrinsicHeight.coerceAtLeast(1)
                            val bmp = createBitmap(width, height)
                            val canvas = Canvas(bmp)
                            drawable.setBounds(0, 0, width, height)
                            drawable.draw(canvas)
                            bmp
                        }
                    }
                    onImageSelected(bitmap)
                }
            }

            val url = Constants.BASE_URL_GLIDE + image.file_url
            Glide.with(binding.root.context)
                .load(url)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("GlideDebug", "Image load failed: ${e?.message}")
                        binding.progressBar.visibility = View.GONE
                        currentDrawable = null
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("GlideDebug", "Image loaded successfully from: $url")
                        binding.progressBar.visibility = View.GONE
                        currentDrawable = resource
                        return false
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .thumbnail(0.1f)
                .skipMemoryCache(false)
                .into(binding.image)
        }
    }
}
