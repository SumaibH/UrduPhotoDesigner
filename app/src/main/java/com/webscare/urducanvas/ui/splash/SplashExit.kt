package com.webscare.urducanvas.ui.splash

import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.view.View
import android.widget.ImageView
import android.widget.TextView

/**
 * A screen the splash can land on: Home. The exit collapses the splash ground into
 * [landingHeader], carries the wordmark onto [landingTitle] and the calligraphy onto
 * [landingWatermark], rises [landingContent] in underneath while the ground lifts, and
 * blooms [landingHeaderContent] in once the ground has settled into the header. Every
 * one of them may be null or empty once the fragment's view is gone.
 */
interface SplashLanding {
    fun landingHeader(): View?
    fun landingTitle(): TextView?
    fun landingWatermark(): ImageView?
    fun landingContent(): List<View>
    fun landingHeaderContent(): List<View>
}

/**
 * Everything the exit overlay needs to paint the bare splash exactly as the fragment
 * left it, in window coordinates: the ground and its art, the calligraphy and the
 * wordmark. The mark, tagline and footer are not carried: the fragment fades them
 * out before handing over.
 */
class SplashExitSpec(
    val text: String,
    val textPaint: TextPaint,
    val textX: Float,
    val textBaseline: Float,
    val watermark: Drawable?,
    val watermarkBounds: RectF,
    val watermarkAlpha: Float
)

/** Where the overlay lands, in window coordinates, read off Home once it is laid out. */
class SplashLandingTarget(
    val headerBottom: Float,
    val cornerRadius: Float,
    val titleX: Float,
    val titleBaseline: Float,
    val titlePaint: TextPaint,
    val watermarkBounds: RectF?
)
