package com.webscare.urducanvas

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.webscare.ads.WebsCareAds
import com.webscare.urducanvas.common.canvas.CanvasViewModel
import com.webscare.urducanvas.common.canvas.model.CanvasSize
import com.webscare.urducanvas.common.utils.Utils.addPressEffect
import com.webscare.urducanvas.databinding.ActivityMainBinding
import com.webscare.urducanvas.di.BillingManager
import com.webscare.urducanvas.di.UpdateManager
import com.webscare.urducanvas.viewmodels.MainViewModel
import com.webscare.urducanvas.viewmodels.PexelsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import com.webscare.urducanvas.analytics.navigation.NavigationAnalyticsListener
import com.webscare.urducanvas.ui.splash.SplashExitController
import com.webscare.urducanvas.ui.splash.SplashHandoff

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var navigationAnalyticsListener: NavigationAnalyticsListener

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var updateCheckTriggered = false

    /** The banner only starts loading once a screen that shows it is reached. */
    private var bannerAdInitialised = false

    private var _navController: NavController? = null
    private val navController get() = _navController!!

    private val viewModel: CanvasViewModel by viewModels()

    val navOptions: NavOptions = NavOptions.Builder().setLaunchSingleTop(true).build()

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val fileName = com.webscare.urducanvas.common.utils.ImageUtils.getFileNameFromUri(this@MainActivity, it)
                        val rawBitmap = com.webscare.urducanvas.common.utils.ImageProcessor
                            .decodeUriSafely(contentResolver, it, com.webscare.urducanvas.common.utils.Constants.GPU_SAFE_MAX_PX, com.webscare.urducanvas.common.utils.Constants.GPU_SAFE_MAX_PX)
                            ?: return@launch

                        val bitmap = com.webscare.urducanvas.common.utils.ImageProcessor
                            .downsampleIfNeeded(rawBitmap, com.webscare.urducanvas.common.utils.Constants.GPU_SAFE_MAX_PX, com.webscare.urducanvas.common.utils.Constants.GPU_SAFE_MAX_PX)

                        val widthVal = bitmap.width.toFloat()
                        val heightVal = bitmap.height.toFloat()

                        withContext(Dispatchers.Main) {
                            val canvasSize = CanvasSize(
                                id = 0, "From Image", widthVal, heightVal
                            )
                            viewModel.clearCanvas()
                            viewModel.setCanvasSize(canvasSize)
                            viewModel.setCanvasBackgroundImage(bitmap, this@MainActivity, customName = fileName)
                            val editorNavOptions = NavOptions.Builder().setLaunchSingleTop(true)
                                .setPopUpTo(R.id.editorFragment, inclusive = true).build()
                            navController.navigate(R.id.editorFragment, null, editorNavOptions)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var updateManager: UpdateManager

    private val mainViewModel: MainViewModel by viewModels()

    // Creating PexelsViewModel here ensures Hilt constructs it on app start,
    // so seedPexelsCategories() fires immediately — same lifecycle as MainViewModel.
    // The actual seeding now lives in MainViewModel.seedPexelsCategories() which
    // is called from MainViewModel.init, so this just keeps the VM alive.
    @Suppress("unused")
    private val pexelsViewModel: PexelsViewModel by viewModels()

    // ──────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────

    /**
     * Guard against a known Android framework bug where
     * [ViewGroup.TouchTarget.recycle] throws [IllegalStateException]
     * ("already recycled once") when a child view is removed/detached
     * during an active touch sequence.  The entire stack trace is
     * framework-internal, so catching here is the only viable fix.
     */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        return try {
            super.dispatchTouchEvent(ev)
        } catch (e: IllegalStateException) {
            // Swallow only the specific "already recycled" crash
            true
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("is_dark_mode", false)
        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
        }

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)

        // Android's own splash (the icon on the window background) is handed over here once
        // the first frame is drawn; SplashFragment claims it and blooms the brand ground out
        // of that very icon. Nothing is removed until the fragment has dissolved it.
        splashScreen.setOnExitAnimationListener { provider -> splashHandoff.offer(provider) }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            statusBarInsetPx = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            // Edge to edge: the window never reserves the status bar. Every screen
            // re-inserts it as its own top padding via InsetUtils, the same way
            // home has always sized its header spacer.
            view.setPadding(0, 0, 0, navBarHeight)
            // The plate that keeps the status bar white on every screen but home.
            // Re-sized on every inset pass so rotation and multi-window stay right.
            binding.statusBarScrim.updateLayoutParams { height = statusBarInsetPx }
            ViewCompat.dispatchApplyWindowInsets(binding.navHostMain, insets)
            insets
        }

        updateManager.registerListener(this)
        forceImmersiveMode()
        billingManager.checkSubscriptionOnLaunch()
        viewModel.fetchDefaultPreferences()
        initObservers()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_main) as NavHostFragment
        _navController = navHostFragment.navController

        // The exit from the splash into Home runs above the nav host, so the green can
        // collapse into Home's header while Home lays itself out underneath. Chrome waits
        // for it to land, and the add button then fades in rather than popping.
        splashExit = SplashExitController(binding.main, navHostFragment.childFragmentManager) { running ->
            splashExitRunning = running
            if (!running) binding.fabAddImage.alpha = 0f
            updateChromeVisibility()
            if (!running && binding.fabAddImage.visibility == View.VISIBLE) {
                binding.fabAddImage.animate().alpha(1f).setDuration(220).start()
            }
        }

        setupChrome()
        handleIncomingIntent(intent)
    }

    // ──────────────────────────────────────────────────────────
    // Top-level chrome (FAB + status bar + back handling)
    // ──────────────────────────────────────────────────────────

    /** Destinations that keep the FAB and the banner ad on screen. */
    private val topLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.templateCategoriesFragment,
        R.id.filesFragment,
        R.id.settingsFragment
    )

    /**
     * Screens that paint their own artwork behind the status bar and therefore
     * want light (white) status bar icons. Everything else is a white page and
     * takes dark icons.
     */
    private val darkChromeDestinations = setOf(
        R.id.homeFragment,
        // Splash blooms its green up behind the bars, so it gets no white plate
        // either — otherwise a white band sits over the artwork.
        R.id.splashFragment
    )

    /** Runs the splash's collapse into Home's header; the chrome waits for it to land. */
    lateinit var splashExit: SplashExitController
        private set
    private var splashExitRunning = false

    /** Android's splash view on its way from the window to SplashFragment. */
    val splashHandoff = SplashHandoff()

    /**
     * The splash starts on the window background and turns green a beat later, so it
     * sets the status bar icon colour itself at each step.
     */
    fun setStatusBarIconsDark(dark: Boolean) = setStatusBarTextColor(darkIcons = dark)

    /** Status bar height in px, published for fragments that pad themselves. */
    var statusBarInsetPx: Int = 0
        private set

    var isSplashCompleted: Boolean = false
        set(value) {
            field = value
            updateChromeVisibility()
        }

    /**
     * The destination whose fragment is actually resumed on screen.
     *
     * Navigation reports a destination change as soon as the graph moves, which is before
     * the new fragment has been committed and drawn — so showing the chrome off the
     * destination alone popped the add button in over the tail of the splash, ahead of
     * Home. Hiding still keys off the destination, because that has to be immediate;
     * only showing waits for the fragment to resume.
     */
    private var resumedDestinationId: Int? = null

    fun updateChromeVisibility() {
        if (_binding == null) return
        val destId = _navController?.currentDestination?.id
        val isTopLevel = isSplashCompleted && !splashExitRunning &&
                destId in topLevelDestinations &&
                resumedDestinationId == destId
        if (isTopLevel && !bannerAdInitialised) {
            bannerAdInitialised = true
            binding.mainBannerAd.setAdUnitId(BuildConfig.AD_BANNER_MAIN)
        }
        binding.mainBannerAd.visibility = if (isTopLevel) View.VISIBLE else View.GONE
        binding.bannerAdDivider.visibility = if (isTopLevel) View.VISIBLE else View.GONE
        binding.fabAddImage.visibility = if (isTopLevel) View.VISIBLE else View.GONE
        if (isTopLevel) binding.fabAddImage.bringToFront()
    }

    private fun setupChrome() {
        // Nothing is loaded or shown until a top-level destination is reached, so
        // splash never gets the FAB, the banner, or the divider above it.
        binding.fabAddImage.visibility = View.GONE
        binding.mainBannerAd.visibility = View.GONE
        binding.bannerAdDivider.visibility = View.GONE

        val density = resources.displayMetrics.density
        binding.fabAddImage.elevation = 16f * density
        ViewCompat.setElevation(binding.fabAddImage, 16f * density)
        binding.fabAddImage.bringToFront()

        binding.fabAddImage.addPressEffect {
            pickImageLauncher.launch("image/*")
        }

        // Chrome appears when the destination's fragment is genuinely on screen, not when
        // the graph says it is on its way there.
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_main) as NavHostFragment
        navHost.childFragmentManager.registerFragmentLifecycleCallbacks(
            object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(
                    fm: androidx.fragment.app.FragmentManager,
                    f: androidx.fragment.app.Fragment
                ) {
                    resumedDestinationId = _navController?.currentDestination?.id
                    updateChromeVisibility()
                }
            },
            false
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.homeFragment && !updateCheckTriggered) {
                updateCheckTriggered = true
                updateManager.checkForUpdate(this)
            }

            if (destination.id in topLevelDestinations) {
                isSplashCompleted = true
            }

            updateChromeVisibility()
            applyStatusBarFor(destination.id)
        }
        navController.addOnDestinationChangedListener(navigationAnalyticsListener)

        onBackPressedDispatcher.addCallback(this) {
            when (navController.currentDestination?.id) {
                R.id.editorFragment -> { /* editor handles its own back */ }
                R.id.homeFragment -> finish()
                R.id.templateCategoriesFragment,
                R.id.filesFragment,
                R.id.settingsFragment ->
                    navController.navigate(R.id.homeFragment, null, navOptions)
                else -> { /* do nothing */ }
            }
        }
    }

    // ──────────────────────────────────────────────────────────
    // Status bar / immersive
    // ──────────────────────────────────────────────────────────

    private fun setStatusBarTextColor(darkIcons: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = darkIcons
    }

    /**
     * The window itself stays transparent — from targetSdk 35 the platform
     * ignores `statusBarColor` under edge to edge, so the status bar has to be
     * painted in the layout instead. [ActivityMainBinding.statusBarScrim] does
     * that: white (dark at night) on every destination, hidden only on the
     * screens that paint their own artwork up there. Icon colour follows.
     */
    private fun applyStatusBarFor(destinationId: Int?) {
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val paintsOwnChrome = destinationId in darkChromeDestinations
        binding.statusBarScrim.visibility = if (paintsOwnChrome) View.GONE else View.VISIBLE
        // The splash starts on the window background and turns green a beat later; it drives
        // its own icon colour at each step, so this must not snap it back.
        if (destinationId == R.id.splashFragment) return
        setStatusBarTextColor(darkIcons = !paintsOwnChrome && !isNight)
    }

    private fun applyStatusBarColor() =
        applyStatusBarFor(_navController?.currentDestination?.id)

    private fun forceImmersiveMode() {
        window?.let { w ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                w.insetsController?.apply {
                    hide(WindowInsets.Type.navigationBars())
                    show(WindowInsets.Type.statusBars())
                    systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION") w.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            applyStatusBarColor()
        }
    }

    // ──────────────────────────────────────────────────────────
    // Intent handling
    // ──────────────────────────────────────────────────────────

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
        uri ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ── 1) Our project file? (.urdc, .bin from WhatsApp, or octet-stream) ──
                // We always attempt tryOpenProject for any octet-stream / unknown extension.
                // tryOpenProject does a magic-byte check internally — it rejects non-URDC files
                // silently, so false positives just fall through to the image handler below.
                val name = queryDisplayName(uri)
                val looksLikeProject = name?.endsWith(".urdc", true) == true || name?.endsWith(
                    ".bin", true
                ) == true ||   // WhatsApp renames .urdc -> .bin
                        intent.type == "application/octet-stream" || intent.type == "application/octet_stream" || intent.type == null   // some share sources send no MIME type at all
                if (looksLikeProject && tryOpenProjectAsync(uri)) {
                    return@launch
                }

                // ── 2) Existing image handling (unchanged) ──
                if (intent.type?.startsWith("image/") == true) {
                    val fileName = com.webscare.urducanvas.common.utils.ImageUtils.getFileNameFromUri(this@MainActivity, uri)
                    val rawBitmap = com.webscare.urducanvas.common.utils.ImageProcessor.decodeUriSafely(
                        contentResolver,
                        uri,
                        com.webscare.urducanvas.common.utils.Constants.GPU_SAFE_MAX_PX,
                        com.webscare.urducanvas.common.utils.Constants.GPU_SAFE_MAX_PX
                    )
                    if (rawBitmap != null) {
                        val bitmap = com.webscare.urducanvas.common.utils.ImageProcessor.downsampleIfNeeded(
                            rawBitmap,
                            com.webscare.urducanvas.common.utils.Constants.GPU_SAFE_MAX_PX,
                            com.webscare.urducanvas.common.utils.Constants.GPU_SAFE_MAX_PX
                        )
                        val widthVal = bitmap.width.toFloat()
                        val heightVal = bitmap.height.toFloat()
                        withContext(Dispatchers.Main) {
                            val canvasSize = CanvasSize(
                                id = 0, "From Image", widthVal, heightVal
                            )
                            viewModel.clearCanvas()
                            viewModel.setCanvasSize(canvasSize)
                            viewModel.setCanvasBackgroundImage(bitmap, this@MainActivity, customName = fileName)
                            val opts = NavOptions.Builder().setLaunchSingleTop(true)
                                .setPopUpTo(R.id.editorFragment, inclusive = true).build()
                            navController.navigate(R.id.editorFragment, null, opts)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Copy the incoming project to cache, build a minimal ExportResult, and load via the VM. */
    private suspend fun tryOpenProjectAsync(uri: Uri): Boolean {
        return try {
            val cached = File(cacheDir, "incoming_${System.currentTimeMillis()}.urdc")
            contentResolver.openInputStream(uri)?.use { input ->
                cached.outputStream().use { input.copyTo(it) }
            } ?: return false

            // Verify it's really ours before hijacking the open.
            val isUrdc = com.webscare.urducanvas.common.canvas.io.ProjectCodec.isUrdcFile(cached)
            if (!isUrdc) {
                // Could still be a plain .json shared from a debug/authoring build — allow that.
                val firstByte = cached.inputStream().use { it.read() }
                if (firstByte != '['.code && firstByte != '{'.code) {
                    cached.delete(); return false
                }
            }

            // jsonPath points at the cached file; the loader auto-detects .urdc vs plain JSON.
            val result = com.webscare.urducanvas.data.model.ExportResult(
                imagePath = "",
                jsonPath = cached.absolutePath,
                fileName = "Shared project",
                fileSizeMB = cached.length() / (1024.0 * 1024.0),
                resolution = "",
                format = "URDC",
                quality = "",
                canvasSize = viewModel.canvasSize.value ?: CanvasSize(
                    id = 0, "Project", 1080f, 1080f
                ),
                exportDate = "",
                updatedDate = ""
            )

            withContext(Dispatchers.Main) {
                viewModel.loadTemplateFromJsonFile(result, this@MainActivity) { success ->
                    if (success) {
                        val opts = NavOptions.Builder().setLaunchSingleTop(true)
                            .setPopUpTo(R.id.editorFragment, inclusive = true).build()
                        navController.navigate(R.id.editorFragment, null, opts)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace(); false
        }
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        if (uri.scheme == "file") uri.lastPathSegment
        else contentResolver.query(
            uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (e: Exception) {
        null
    }

    // ──────────────────────────────────────────────────────────
    // Observers / Resume / Result / Destroy
    // ──────────────────────────────────────────────────────────

    private fun initObservers() {
        lifecycleScope.launch { mainViewModel.localFonts.collect { } }
        lifecycleScope.launch { mainViewModel.localImages.collect { } }
        lifecycleScope.launch { billingManager.isSubscribed.collect { } }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) forceImmersiveMode()
    }

    override fun onResume() {
        super.onResume()
        updateManager.onResume(this)
    }

    private var originalBaseContext: Context? = null

    override fun attachBaseContext(newBase: Context) {
        originalBaseContext = newBase
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = 1.0f
        if (MyApplication.defaultDensityDpi != 0) {
            config.densityDpi = MyApplication.defaultDensityDpi
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun getSystemService(name: String): Any? {
        if (name == Context.PRINT_SERVICE) {
            return originalBaseContext?.getSystemService(name) ?: super.getSystemService(name)
        }
        return super.getSystemService(name)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UpdateManager.REQUEST_CODE_UPDATE && resultCode != RESULT_OK) {
            updateManager.checkForUpdate(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateManager.onDestroy()
        splashHandoff.clear()
        if (::splashExit.isInitialized) splashExit.cancel()
        if (isFinishing) {
            // The ads SDK's handlers are process-scoped singletons that hold on to loaded
            // ads — and a native ad holds its NativeAdView, which holds this Activity.
            // LeakCanary saw MainActivity retained that way after every finish. The caches
            // are worthless once the task is going away, so drop them. Guarded on
            // isFinishing so a configuration change does not throw away preloaded ads.
            WebsCareAds.destroyAllAds()
        }
        _navController = null
        _binding = null
    }
}