package com.example.urduphotodesigner.common.glide

import android.content.Context
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.caverock.androidsvg.SVG
import java.io.InputStream

@GlideModule
class SvgDecoderModule : AppGlideModule() {
    override fun registerComponents(
        context: Context,
        glide: Glide,
        registry: Registry
    ) {
        registry
            .register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder())
            .append(InputStream::class.java, SVG::class.java, SvgDecoder())
            .append(
                GlideUrl::class.java,
                InputStream::class.java,
                OkHttpUrlLoader.Factory()
            )
    }

    override fun isManifestParsingEnabled(): Boolean = false
}
