package com.webscare.urducanvas.ui.navigation.tutorials

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.webscare.urducanvas.R
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.common.utils.isDarkModeEnabled
import com.webscare.urducanvas.common.utils.startShimmerSoft
import com.webscare.urducanvas.data.model.TutorialVideo
import com.webscare.urducanvas.databinding.LayoutTutorialItemBinding
import java.util.Locale
import java.util.concurrent.TimeUnit

class TutorialsAdapter(
    private val onVideoClicked: (TutorialVideo, Int) -> Unit
) : ListAdapter<TutorialVideo, TutorialsAdapter.VideoViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = LayoutTutorialItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        holder.clear()
    }

    inner class VideoViewHolder(
        private val binding: LayoutTutorialItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(video: TutorialVideo) {
            val context = binding.root.context

            binding.videoTitle.text = video.title
            binding.meta.text = context.getString(
                R.string.tutorial_meta,
                formatViews(video.views),
                formatAge(context, video.publishedAtMillis)
            )

            // The badge only appears once there is an image under it, so a still-loading
            // row is a clean shimmer rather than a logo floating on a grey box.
            binding.playBadge.visibility = View.GONE
            binding.duration.visibility = View.GONE

            binding.shimmerLayout.startShimmerSoft(context.isDarkModeEnabled())
            Glide.with(context)
                .load(video.thumbnailUrl)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?,
                        target: Target<Drawable>, isFirstResource: Boolean
                    ): Boolean {
                        binding.shimmerLayout.hideShimmer()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable, model: Any,
                        target: Target<Drawable>?, dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.shimmerLayout.hideShimmer()
                        binding.playBadge.visibility = View.VISIBLE
                        return false
                    }
                })
                .into(binding.thumbnail)

            binding.root.addPressEffect { onVideoClicked(video, bindingAdapterPosition) }
        }

        /**
         * Glide keeps loading into a recycled view otherwise, and the shimmer would be
         * left switched off on a row that has just been rebound to a different video.
         */
        fun clear() {
            Glide.with(binding.root.context).clear(binding.thumbnail)
            binding.thumbnail.setImageDrawable(null)
            binding.playBadge.visibility = View.GONE
        }
    }

    // Locale.US on purpose: the ".0" trim below matches a dot, and on an Urdu or Arabic
    // locale the default format would produce a different separator — and Eastern Arabic
    // digits — leaving "١٫٢K" with the trim silently doing nothing.
    private fun formatViews(views: Long): String = when {
        views >= 1_000_000 -> String.format(Locale.US, "%.1fM", views / 1_000_000f).replace(".0", "")
        views >= 1_000 -> String.format(Locale.US, "%.1fK", views / 1_000f).replace(".0", "")
        else -> views.toString()
    }

    private fun formatAge(context: android.content.Context, publishedAtMillis: Long): String {
        if (publishedAtMillis <= 0L) return ""
        val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - publishedAtMillis)
        val res = context.resources
        return when {
            days < 1 -> context.getString(R.string.age_today)
            days < 7 -> res.getQuantityString(R.plurals.age_days, days.toInt(), days.toInt())
            days < 30 -> {
                val weeks = (days / 7).toInt()
                res.getQuantityString(R.plurals.age_weeks, weeks, weeks)
            }

            days < 365 -> {
                val months = (days / 30).toInt()
                res.getQuantityString(R.plurals.age_months, months, months)
            }

            else -> {
                val years = (days / 365).toInt()
                res.getQuantityString(R.plurals.age_years, years, years)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<TutorialVideo>() {
            override fun areItemsTheSame(old: TutorialVideo, new: TutorialVideo) = old.id == new.id
            override fun areContentsTheSame(old: TutorialVideo, new: TutorialVideo) = old == new
        }
    }
}
