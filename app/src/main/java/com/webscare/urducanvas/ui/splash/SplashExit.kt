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
 * unfolds [landingHeaderContent] — in the order given — as the ground settles into the
 * header. Every one of them may be null or empty once the fragment's view is gone.
 */
interface SplashLanding {
    fun landingHeader(): View?
    fun landingTitle(): TextView?
    fun landingWatermark(): ImageView?
    fun landingContent(): List<View>
    fun landingHeaderContent(): List<View>
}

/** A run of text exactly as the splash laid it out, in window coordinates. */
class SplashText(
    val text: String,
    val paint: TextPaint,
    val x: Float,
    val baseline: Float
)

/** A drawable exactly where the splash drew it, in window coordinates. */
class SplashImage(
    val drawable: Drawable,
    val bounds: RectF,
    val alpha: Float
)

/**
 * The whole settled splash in window coordinates, so [SplashExitOverlay] can repaint it
 * above the navigation host without a single pixel moving at the hand-off — ground and
 * art, the calligraphy, the mark, the wordmark, the tagline and the loading line.
 *
 * Nothing is faded out before the hand-off on purpose: the overlay has to hold this
 * picture for as long as Home takes to lay itself out, and a half-dismantled splash is
 * a bad thing to stare at. The pieces leave during the collapse, while everything is
 * already moving.
 */
class SplashExitSpec(
    val wordmark: SplashText,
    val tagline: SplashText?,
    val publisher: SplashText?,
    val mark: SplashImage?,
    val watermark: SplashImage?,
    val loaderTrack: RectF?,
    val loaderTrackColor: Int,
    val loaderColor: Int,
    val loaderCorner: Float
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

/**
 * Where an image view actually paints its drawable, in window coordinates, with its
 * scale type, scale and translation applied — Home's calligraphy is scaled up and
 * pushed off the corner, and the splash's mark is fit inside a box, so neither view's
 * bounds say where the ink is.
 */
internal fun ImageView.drawnBoundsInWindow(): RectF? {
    val drawable = drawable ?: return null
    val parent = parent as? View ?: return null
    val rect = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
    imageMatrix?.mapRect(rect)
    rect.offset(paddingLeft.toFloat(), paddingTop.toFloat())
    matrix.mapRect(rect)
    val location = IntArray(2)
    parent.getLocationInWindow(location)
    rect.offset(location[0] + left.toFloat(), location[1] + top.toFloat())
    return rect
}
