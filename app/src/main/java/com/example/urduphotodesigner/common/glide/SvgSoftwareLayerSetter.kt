package com.example.urduphotodesigner.common.glide

import android.graphics.drawable.PictureDrawable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class SvgSoftwareLayerSetter : RequestListener<PictureDrawable> {
    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<PictureDrawable>,
        isFirstResource: Boolean
    ): Boolean = false

    override fun onResourceReady(
        resource: PictureDrawable,
        model: Any,
        target: Target<PictureDrawable>?,
        dataSource: DataSource,
        isFirstResource: Boolean
    ): Boolean {
        (target as ImageView).setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        return false
    }
}
