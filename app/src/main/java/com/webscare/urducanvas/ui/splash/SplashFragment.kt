package com.webscare.urducanvas.ui.splash

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.MessageQueue
import android.os.SystemClock
import android.text.TextPaint
import android.util.Log
import android.view.Choreographer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.core.view.OneShotPreDrawListener
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.webscare.ads.WebsCareAds
import com.webscare.urducanvas.BuildConfig
import com.webscare.urducanvas.MainActivity
import com.webscare.urducanvas.R
import com.webscare.urducanvas.analytics.ads.AdAnalyticsCoordinator
import com.webscare.urducanvas.databinding.FragmentSplashBinding
import com.webscare.urducanvas.di.BillingManager
import com.webscare.urducanvas.ui.splash.SplashArt.Companion.EMPHASIZED
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.hypot
import kotlin.math.max

/**
 * The launch splash, picking up exactly where Android's own leaves off.
 *
 * Android 12+ (and the compat library on older versions) paints the launcher icon on
 * the window background before the app has drawn anything. Once the first frame is up,
 * MainActivity is handed that splash view and passes it here through [SplashHandoff].
 * The choreography then runs from the icon Android actually drew, wherever it drew it:
 *
 *  1. The brand ground blooms out of the icon's circle to the corners of the screen,
 *     starting in the icon's own greens and deepening into the brand gradient, while
 *     the system icon dissolves into the app mark. The mark rests on the very spot the
 *     icon occupied, so nothing drifts; it only breathes out a little.
 *  2. The wordmark and tagline rise in beneath it; the khatam lattice, the calligraphy
 *     watermark and the footer fade up behind and beneath them.
 *  3. The screen holds — [MIN_HOLD_MS] at the least, longer while the app-open ad is
 *     still loading, never past [MAX_AD_WAIT_MS] — then, through the ad, leaves for
 *     Home by handing its last frame to [SplashExitController], which collapses the
 *     green into Home's header while Home lays itself out underneath.
 *
 * Two things keep the bloom smooth and continuous rather than merely correct. The
 * choreography does not start until the main thread has drained its start-up work (or
 * [IDLE_WAIT_MAX_MS] has passed): animations started into a busy thread skip every
 * frame and the icon simply cuts to a half-done splash. And the fragment's own copy of
 * the icon sits under Android's from the first frame, so if no system splash ever
 * arrives — timed from the first draw, since that is what triggers the handover — the
 * bloom plays from that replica instead of from a blank screen.
 *
 * The status bar stays on screen throughout: the ground paints behind it and its icons
 * flip from dark to light the moment the green reaches the top edge.
 */
@AndroidEntryPoint
class SplashFragment : Fragment() {

    @Inject
    lateinit var adAnalyticsCoordinator: AdAnalyticsCoordinator

    @Inject
    lateinit var billingManager: BillingManager

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    /** Android's splash view, held until its icon has dissolved into ours. */
    private var systemSplash: SplashScreenViewProvider? = null
    private var started = false
    private var startedAt = 0L
    private var viewCreatedAt = 0L
    private var navigated = false
    private var reveal: Animator? = null
    private var markFade: Animator? = null
    private var latticeFade: Animator? = null
    private var tintFade: Animator? = null
    private var idleHandler: MessageQueue.IdleHandler? = null
    private var idleTimeout: Runnable? = null

    /** No handover arrived (nothing for Android to hand over): play from the replica icon. */
    private val fallback = Runnable { begin(null) }

    /** Once the green has reached the top edge the status bar icons go light. */
    private val statusBarFlip = Runnable { (activity as? MainActivity)?.setStatusBarIconsDark(false) }

    /**
     * AdMob keeps the FullScreenContentCallback it was handed alive well past the ad being
     * dismissed, so a dismissal lambda that captures `this` keeps the whole SplashFragment
     * reachable — LeakCanary caught it three separate launches.
     *
     * The relay is a plain nested (non-inner) class, so the ads SDK holding it holds
     * nothing else; the lambda that does capture the fragment lives in [Relay.action],
     * which [onDestroy] clears.
     */
    private class Relay {
        var action: (() -> Unit)? = null
        fun fire() {
            action?.invoke()
        }
    }

    private val appOpenDismissRelay = Relay()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val main = activity as? MainActivity
        main?.let {
            it.isSplashCompleted = false
            it.updateChromeVisibility()
        }

        // The lattice fades up on its own clock; it must not be there before that.
        binding.ground.latticeAlpha = 0f

        // The first beat is the window background, so the icons have to suit that,
        // not the green that arrives a moment later.
        val windowGround = ContextCompat.getColor(requireContext(), R.color.screen_bg)
        main?.setStatusBarIconsDark(ColorUtils.calculateLuminance(windowGround) > 0.5)

        viewCreatedAt = SystemClock.uptimeMillis()
        main?.splashHandoff?.consume { provider -> begin(provider) }

        // Android hands its view over once the first frame is drawn, so the wait for it is
        // timed from there — not from here, which on a slow cold start can be a second or
        // more earlier while the activity is still setting itself up.
        OneShotPreDrawListener.add(view) {
            view.postDelayed(fallback, HANDOFF_TIMEOUT_MS)
        }
    }

    private fun begin(provider: SplashScreenViewProvider?) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "begin: handoff=${provider != null} sinceViewCreated=${SystemClock.uptimeMillis() - viewCreatedAt}ms")
        }
        if (started) {
            // The fallback has already played; a late system view can only fade off the top of it.
            provider?.view?.animate()
                ?.alpha(0f)
                ?.setDuration(CROSSFADE_MS)
                ?.withEndAction { provider.remove() }
                ?.start()
            return
        }
        val root = _binding?.root
        if (root == null) {
            provider?.remove()
            return
        }
        started = true
        root.removeCallbacks(fallback)
        systemSplash = provider
        if (provider != null) {
            // Android's own icon is on top; the replica underneath has done its job.
            binding.iconReplica.visibility = View.GONE
        }

        if (root.isLaidOut) {
            playWhenIdle(provider)
        } else {
            root.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View, left: Int, top: Int, right: Int, bottom: Int,
                    oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
                ) {
                    v.removeOnLayoutChangeListener(this)
                    playWhenIdle(provider)
                }
            })
        }
    }

    /**
     * Starts the choreography once the main thread has nothing queued, or after
     * [IDLE_WAIT_MAX_MS] at the latest. Until then the screen shows the icon, exactly as
     * before, so the wait costs nothing visible; starting sooner would cost the bloom.
     */
    private fun playWhenIdle(provider: SplashScreenViewProvider?) {
        val root = _binding?.root ?: return
        val handler = MessageQueue.IdleHandler {
            clearIdleWait()
            play(provider)
            false
        }
        val timeout = Runnable {
            clearIdleWait()
            play(provider)
        }
        idleHandler = handler
        idleTimeout = timeout
        Looper.myQueue().addIdleHandler(handler)
        root.postDelayed(timeout, IDLE_WAIT_MAX_MS)
    }

    private fun clearIdleWait() {
        idleHandler?.let { Looper.myQueue().removeIdleHandler(it) }
        idleTimeout?.let { _binding?.root?.removeCallbacks(it) }
        idleHandler = null
        idleTimeout = null
    }

    private fun play(provider: SplashScreenViewProvider?) {
        val b = _binding ?: return
        val root = b.root
        val width = root.width.toFloat()
        val height = root.height.toFloat()
        if (width == 0f || height == 0f) return
        val density = resources.displayMetrics.density
        val rootInWindow = IntArray(2).also { root.getLocationInWindow(it) }
        startedAt = SystemClock.uptimeMillis()

        // Android painted its splash with the system's day/night setting, but MainActivity
        // forces the app's own preference before this view exists, so the two grounds can
        // differ (a dark phone running the app in light mode, or the reverse). Ours takes
        // the colour Android actually showed, so nothing flashes before the green arrives.
        val systemGround = (provider?.view?.background as? ColorDrawable)?.color
        if (systemGround != null) {
            root.setBackgroundColor(systemGround)
            (activity as? MainActivity)
                ?.setStatusBarIconsDark(ColorUtils.calculateLuminance(systemGround) > 0.5)
        }

        // The icon the bloom grows out of: Android's when it was handed over, otherwise our
        // replica. The platform view's icon is nullable underneath the compat wrapper, so a
        // missing one counts as no handover. Its circle, glyph and greens are measured off
        // the view itself rather than assumed: Android 12+ fills the icon view with the
        // circle, the compat library draws a two-thirds one, and the glyph sits wherever
        // the art puts it.
        val systemIcon = provider?.let { runCatching { it.iconView }.getOrNull() }
        val icon: View = systemIcon?.takeIf { it.width > 0 && it.height > 0 } ?: b.iconReplica
        val geometry = measureIcon(icon) ?: assumedGeometry(isSystemIcon = icon === systemIcon)
        val iconInWindow = IntArray(2).also { icon.getLocationInWindow(it) }
        val iconSize = icon.width.toFloat()
        val cx = iconInWindow[0] - rootInWindow[0] + icon.width / 2f + iconSize * geometry.circleDx
        val cy = iconInWindow[1] - rootInWindow[1] + icon.height / 2f + iconSize * geometry.circleDy
        val iconDiameter = iconSize * geometry.circle
        val glyphWidth = iconSize * geometry.glyph
        val glyphCx = cx + iconSize * geometry.glyphDx
        val glyphCy = cy + iconSize * geometry.glyphDy
        if (BuildConfig.DEBUG) {
            // The numbers to look at if the bloom's first circle ever mismatches the icon.
            Log.d(
                TAG, "play: ${if (icon === systemIcon) "system" else "replica"} icon " +
                        "${icon.width}x${icon.height}px, circle ${iconDiameter.toInt()}px at " +
                        "(${cx.toInt()}, ${cy.toInt()}), glyph ${glyphWidth.toInt()}px at " +
                        "(${glyphCx.toInt()}, ${glyphCy.toInt()}) ${if (geometry.measured) "measured" else "assumed"}, " +
                        "screen=${width.toInt()}x${height.toInt()}px, density=$density, " +
                        "sinceViewCreated=${startedAt - viewCreatedAt}ms"
            )
            Choreographer.getInstance().postFrameCallback {
                Log.d(TAG, "play: first frame ${SystemClock.uptimeMillis() - startedAt}ms in")
            }
        }

        layoutWatermark(b.calligraphy, width, density)

        // Timers that pace the animators have to stretch with the system animator scale,
        // or a slowed-down choreography would be cut off by a real-time exit.
        val scale = animatorScale()

        // 1. The ground blooms from the icon's circle until it covers the farthest corner,
        //    in the icon's own greens at first, deepening into the brand gradient.
        val endRadius = hypot(max(cx, width - cx), max(cy, height - cy))
        if (geometry.light != null && geometry.dark != null) {
            b.ground.setIconTint(geometry.light, geometry.dark)
            b.ground.iconTintAlpha = 1f
            val ground = b.ground
            tintFade = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = TINT_MS
                addUpdateListener { ground.iconTintAlpha = it.animatedValue as Float }
                start()
            }
        }
        b.groundGroup.visibility = View.VISIBLE
        reveal = ViewAnimationUtils.createCircularReveal(
            b.groundGroup, cx.toInt(), cy.toInt(), iconDiameter / 2f, endRadius
        ).apply {
            duration = REVEAL_MS
            interpolator = EMPHASIZED
            start()
        }
        root.postDelayed(statusBarFlip, (STATUS_BAR_FLIP_MS * scale).toLong())

        // 2. The mark breathes out of the icon's glyph into its resting size, on the spot.
        val mark = b.mark
        val markInWindow = IntArray(2).also { mark.getLocationInWindow(it) }
        val markCx = markInWindow[0] - rootInWindow[0] + mark.width / 2f
        val markCy = markInWindow[1] - rootInWindow[1] + mark.height / 2f
        val startScale = glyphWidth / mark.width
        mark.pivotX = mark.width / 2f
        mark.pivotY = mark.height / 2f
        mark.scaleX = startScale
        mark.scaleY = startScale
        mark.translationX = glyphCx - markCx
        mark.translationY = glyphCy - markCy
        markFade = ObjectAnimator.ofFloat(mark, View.ALPHA, 0f, 1f).apply {
            duration = CROSSFADE_MS
            start()
        }
        mark.animate()
            .scaleX(1f).scaleY(1f)
            .translationX(0f).translationY(0f)
            .setDuration(MARK_MS)
            .setInterpolator(EMPHASIZED)
            .start()

        // 3. The words rise in; lattice, watermark and footer fade up around them.
        riseIn(b.wordmark, WORDMARK_DELAY_MS, density)
        riseIn(b.tagline, TAGLINE_DELAY_MS, density)
        // A plain value animator rather than a property name: nothing for R8 to rename away.
        val ground = b.ground
        latticeFade = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = LATTICE_MS
            startDelay = LATTICE_DELAY_MS
            addUpdateListener { ground.latticeAlpha = it.animatedValue as Float }
            start()
        }
        b.calligraphy.animate()
            .alpha(WATERMARK_ALPHA)
            .setDuration(WATERMARK_MS)
            .setStartDelay(WATERMARK_DELAY_MS)
            .start()
        b.footer.animate()
            .alpha(1f)
            .setDuration(FOOTER_MS)
            .setStartDelay(FOOTER_DELAY_MS)
            .start()

        // 4. The icon dissolves into our mark. Android's view goes once its icon has faded,
        //    its white dropped at once so the bloom shows through; the replica just fades.
        if (provider != null) {
            provider.view.background = null
            if (systemIcon != null) {
                systemIcon.animate()
                    .alpha(0f)
                    .setDuration(CROSSFADE_MS)
                    .withEndAction { releaseSystemSplash() }
                    .start()
            } else {
                root.postDelayed({ releaseSystemSplash() }, (CROSSFADE_MS * scale).toLong())
            }
        } else {
            b.iconReplica.animate()
                .alpha(0f)
                .setDuration(CROSSFADE_MS)
                .start()
        }

        root.postDelayed(exitCheck, (MIN_HOLD_MS * scale).toLong())
    }

    /**
     * The icon's circle, glyph and greens as fractions of the icon view's width: how wide
     * the circle is and where its centre sits relative to the view's, how wide the white
     * glyph is and where its centre sits relative to the circle's, and the lighter and
     * darker green either side of the icon's diagonal.
     */
    private class IconGeometry(
        val circle: Float,
        val circleDx: Float,
        val circleDy: Float,
        val glyph: Float,
        val glyphDx: Float,
        val glyphDy: Float,
        val light: Int?,
        val dark: Int?,
        val measured: Boolean
    )

    /**
     * Draws the icon view small and reads the geometry off the pixels: anything painted is
     * the circle, anything near white is the glyph, and the rest, split along the
     * diagonal, gives the two greens. Null if the view will not draw or shows no glyph, in
     * which case [assumedGeometry] stands in.
     */
    private fun measureIcon(icon: View): IconGeometry? = runCatching {
        if (icon.width <= 0 || icon.height <= 0) return@runCatching null
        val size = MEASURE_PX
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            scale(size / icon.width.toFloat(), size / icon.height.toFloat())
            icon.draw(this)
        }
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        bitmap.recycle()

        var left = size; var right = -1; var top = size; var bottom = -1
        var glyphLeft = size; var glyphRight = -1; var glyphTop = size; var glyphBottom = -1
        var lightR = 0L; var lightG = 0L; var lightB = 0L; var lightN = 0
        var darkR = 0L; var darkG = 0L; var darkB = 0L; var darkN = 0
        val band = (size * TINT_BAND).toInt()
        for (i in pixels.indices) {
            val c = pixels[i]
            if (Color.alpha(c) < 128) continue
            val x = i % size
            val y = i / size
            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
            val r = Color.red(c)
            val g = Color.green(c)
            val bl = Color.blue(c)
            if (r > 225 && g > 225 && bl > 225) {
                if (x < glyphLeft) glyphLeft = x
                if (x > glyphRight) glyphRight = x
                if (y < glyphTop) glyphTop = y
                if (y > glyphBottom) glyphBottom = y
            } else {
                // Which side of the icon's diagonal the pixel is on: the gradient runs
                // top-left to bottom-right, so the two ends are the two greens.
                val d = x + y - size
                if (d < -band) { lightR += r; lightG += g; lightB += bl; lightN++ }
                else if (d > band) { darkR += r; darkG += g; darkB += bl; darkN++ }
            }
        }
        if (right < 0 || glyphRight < 0) return@runCatching null
        val circleCx = (left + right + 1) / 2f
        val circleCy = (top + bottom + 1) / 2f
        IconGeometry(
            circle = (right - left + 1f) / size,
            circleDx = (circleCx - size / 2f) / size,
            circleDy = (circleCy - size / 2f) / size,
            glyph = (glyphRight - glyphLeft + 1f) / size,
            glyphDx = ((glyphLeft + glyphRight + 1) / 2f - circleCx) / size,
            glyphDy = ((glyphTop + glyphBottom + 1) / 2f - circleCy) / size,
            light = if (lightN > 0) Color.rgb((lightR / lightN).toInt(), (lightG / lightN).toInt(), (lightB / lightN).toInt()) else null,
            dark = if (darkN > 0) Color.rgb((darkR / darkN).toInt(), (darkG / darkN).toInt(), (darkB / darkN).toInt()) else null,
            measured = true
        )
    }.getOrNull()

    /**
     * What the icon looks like when it cannot be measured: Android 12+ fills its icon view
     * with the circle, the compat library on older versions masks a two-thirds circle, and
     * the mark in the launcher art spans 69% of the circle, centred 3% above its middle.
     */
    private fun assumedGeometry(isSystemIcon: Boolean): IconGeometry {
        val circle = if (isSystemIcon && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) 2f / 3f else 1f
        return IconGeometry(
            circle = circle,
            circleDx = 0f,
            circleDy = 0f,
            glyph = ASSUMED_GLYPH_FRACTION * circle,
            glyphDx = 0f,
            glyphDy = -ASSUMED_GLYPH_RISE_FRACTION * circle,
            light = null,
            dark = null,
            measured = false
        )
    }

    /** The developer-options animator scale (1 normally, 0 with animations off). */
    private fun animatorScale(): Float =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ValueAnimator.getDurationScale() else 1f

    private fun riseIn(view: View, delay: Long, density: Float) {
        view.translationY = RISE_DP * density
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(RISE_MS)
            .setStartDelay(delay)
            .setInterpolator(EMPHASIZED)
            .start()
    }

    /**
     * The کینوس watermark, as on the Home header: a shade wider than the screen and
     * bleeding off the top right, so only its sweep and dot sit inside the frame.
     */
    private fun layoutWatermark(view: View, width: Float, density: Float) {
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.width = (width * WATERMARK_WIDTH_FRACTION).toInt()
        params.topMargin = (WATERMARK_TOP_DP * density).toInt()
        params.gravity = Gravity.TOP or Gravity.END
        view.layoutParams = params
        view.translationX = width * WATERMARK_BLEED_FRACTION
    }

    private fun releaseSystemSplash() {
        systemSplash?.remove()
        systemSplash = null
    }

    /**
     * Leaves for Home once the splash has held long enough — and, when an app-open ad is
     * on its way, once it has loaded, so the ad plays over the splash rather than over
     * a Home that has only just appeared.
     */
    private val exitCheck = object : Runnable {
        override fun run() {
            val root = _binding?.root ?: return
            val elapsed = SystemClock.uptimeMillis() - startedAt
            val adStillLoading = waitsForAppOpen() &&
                    elapsed < MAX_AD_WAIT_MS &&
                    !WebsCareAds.isAdLoaded(BuildConfig.AD_APP_OPEN_SPLASH)
            if (adStillLoading) {
                root.postDelayed(this, AD_POLL_MS)
            } else {
                navigateToHome()
            }
        }
    }

    /** Only worth waiting for an ad that can actually show: ads on, a unit set, not premium. */
    private fun waitsForAppOpen(): Boolean =
        BuildConfig.ENABLE_ADS &&
                BuildConfig.AD_APP_OPEN_SPLASH.isNotEmpty() &&
                !billingManager.isSubscribed.value

    private fun navigateToHome() {
        if (navigated || !isAdded || view == null) return
        navigated = true

        val performNavigation = {
            view?.post { leaveForHome() }
        }

        val activity = activity
        if (activity != null) {
            adAnalyticsCoordinator.onAdOpportunity("app_open_splash", "app_open", "splash_open")
            appOpenDismissRelay.action = {
                // showAppOpen runs its completion lambda whether an ad played or there
                // was no fill, so whether this counts as an impression is decided here
                // from how long the round trip took, not optimistically up front.
                val shown = adAnalyticsCoordinator.onAdCompletionCallback(
                    "app_open_splash", "app_open", "splash", "splash_open"
                )
                if (shown) {
                    adAnalyticsCoordinator.onAdDismissed("app_open_splash", "app_open", rewardEarned = false)
                }
                performNavigation()
            }
            // Bound to a local on purpose: a lambda that mentions `appOpenDismissRelay`
            // directly would capture `this` and defeat the whole point of the relay.
            val relay = appOpenDismissRelay
            WebsCareAds.showAppOpen(activity, BuildConfig.AD_APP_OPEN_SPLASH) { relay.fire() }
        } else {
            performNavigation()
        }
    }

    /**
     * The exit. The splash's content — mark, tagline, footer — fades out first, leaving
     * the bare green sheet with the wordmark and the calligraphy; that is handed to the
     * activity's exit overlay, painted identically above the navigation host, and only
     * then is Home navigated to. The overlay morphs into Home's header once Home has laid
     * itself out.
     */
    private fun leaveForHome() {
        val b = _binding
        val main = activity as? MainActivity
        if (b == null || main == null || !isAdded) {
            navigate()
            return
        }
        main.isSplashCompleted = true
        b.mark.animate()
            .alpha(0f)
            .scaleX(EXIT_MARK_SCALE).scaleY(EXIT_MARK_SCALE)
            .setDuration(EXIT_PREFADE_MS)
            .setInterpolator(EMPHASIZED)
            .start()
        b.tagline.animate().alpha(0f).setDuration(EXIT_PREFADE_MS).start()
        b.footer.animate()
            .alpha(0f)
            .setDuration(EXIT_PREFADE_MS)
            .withEndAction {
                buildExitSpec()?.let { main.splashExit.begin(it) }
                navigate()
            }
            .start()
    }

    private fun navigate() {
        if (!isAdded) return
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.splashFragment, true)
            .build()
        findNavController().navigate(R.id.homeFragment, null, navOptions)
    }

    /** The bare splash, in window coordinates, for [SplashExitOverlay] to paint. */
    private fun buildExitSpec(): SplashExitSpec? {
        val b = _binding ?: return null
        if (b.root.width == 0 || b.wordmark.width == 0) return null
        val watermark = ContextCompat.getDrawable(requireContext(), R.drawable.header_calligraphy)?.mutate()
        val location = IntArray(2)
        b.wordmark.getLocationInWindow(location)
        val textX = location[0] + b.wordmark.compoundPaddingLeft.toFloat()
        val textBaseline = location[1] + b.wordmark.baseline.toFloat()
        return SplashExitSpec(
            text = b.wordmark.text.toString(),
            textPaint = TextPaint(b.wordmark.paint),
            textX = textX,
            textBaseline = textBaseline,
            watermark = watermark,
            watermarkBounds = windowBounds(b.calligraphy, location),
            watermarkAlpha = b.calligraphy.alpha
        )
    }

    private fun windowBounds(view: View, location: IntArray): RectF {
        view.getLocationInWindow(location)
        return RectF(
            location[0].toFloat(), location[1].toFloat(),
            location[0] + view.width.toFloat(), location[1] + view.height.toFloat()
        )
    }

    override fun onDestroyView() {
        clearIdleWait()
        _binding?.let { b ->
            b.root.removeCallbacks(fallback)
            b.root.removeCallbacks(statusBarFlip)
            b.root.removeCallbacks(exitCheck)
            listOf(b.mark, b.wordmark, b.tagline, b.calligraphy, b.footer, b.iconReplica)
                .forEach { it.animate().cancel() }
        }
        reveal?.cancel()
        markFade?.cancel()
        latticeFade?.cancel()
        tintFade?.cancel()
        reveal = null
        markFade = null
        latticeFade = null
        tintFade = null
        releaseSystemSplash()
        (activity as? MainActivity)?.splashHandoff?.clear()
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleared here rather than in onDestroyView so a configuration change while the
        // app-open ad is on screen still navigates when it is dismissed.
        appOpenDismissRelay.action = null
    }

    private companion object {
        const val TAG = "SplashFragment"

        /** Side of the small render the icon's geometry is read from. */
        const val MEASURE_PX = 96

        /** How far from the icon's diagonal a pixel must be to count as one of its two greens. */
        const val TINT_BAND = 0.2f

        /**
         * The mark inside the launcher icon spans about 69% of the circle and sits a touch
         * above its middle — measured off a Pixel 9 Pro's splash. Only used when the icon
         * on screen cannot be measured directly.
         */
        const val ASSUMED_GLYPH_FRACTION = 0.69f
        const val ASSUMED_GLYPH_RISE_FRACTION = 0.03f

        /**
         * How long after the first draw to give Android to hand its splash over before the
         * replica icon plays the bloom on its own. The replica is on screen the whole
         * time, so this costs nothing visible; it only has to outlast the platform.
         */
        const val HANDOFF_TIMEOUT_MS = 3000L

        /** The most the choreography waits for the main thread to go quiet before starting. */
        const val IDLE_WAIT_MAX_MS = 800L

        const val CROSSFADE_MS = 180L
        const val REVEAL_MS = 450L
        const val TINT_MS = 350L
        const val STATUS_BAR_FLIP_MS = 160L
        const val MARK_MS = 640L
        const val RISE_MS = 520L
        const val RISE_DP = 12f
        const val WORDMARK_DELAY_MS = 260L
        const val TAGLINE_DELAY_MS = 400L
        const val LATTICE_MS = 900L
        const val LATTICE_DELAY_MS = 200L
        const val WATERMARK_MS = 1100L
        const val WATERMARK_DELAY_MS = 350L
        const val WATERMARK_ALPHA = 0.10f
        const val WATERMARK_WIDTH_FRACTION = 420f / 390f
        const val WATERMARK_BLEED_FRACTION = 150f / 390f
        const val WATERMARK_TOP_DP = 60f
        const val FOOTER_MS = 600L
        const val FOOTER_DELAY_MS = 700L

        /** How long the settled splash is on screen before Home, at the very least. */
        const val MIN_HOLD_MS = 1600L

        /** How long it will keep waiting beyond that for the app-open ad to load. */
        const val MAX_AD_WAIT_MS = 3000L
        const val AD_POLL_MS = 150L

        /** The mark, tagline and footer fade before the exit overlay takes over the frame. */
        const val EXIT_PREFADE_MS = 220L
        const val EXIT_MARK_SCALE = 0.92f
    }
}
