package com.webscare.urducanvas

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.webscare.urducanvas.di.BillingManager
import com.webscare.ads.WebsCareAds
import javax.inject.Inject
import dagger.hilt.android.HiltAndroidApp
import com.webscare.urducanvas.analytics.AnalyticsTracker
import com.webscare.urducanvas.analytics.ads.AdAnalyticsCoordinator
import com.webscare.urducanvas.analytics.session.SessionStateManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var sessionStateManager: SessionStateManager

    @Inject
    lateinit var adAnalyticsCoordinator: AdAnalyticsCoordinator

    @Inject
    lateinit var preferencesDataStore: com.webscare.urducanvas.common.datastore.PreferencesDataStoreHelper

    companion object {
        var defaultDensityDpi: Int = 0
    }

    override fun onCreate() {
        super.onCreate()
        
        // Hilt field injection completes before this, so billingManager is ready
        val adsEnabled = BuildConfig.ENABLE_ADS
        WebsCareAds.init(this) {
            testMode = BuildConfig.DEBUG
            premiumCheck = { billingManager.isSubscribed.value }
            
            // Connect remote config / local overrides
            homeNativeEnabled = adsEnabled
            categoriesNativeEnabled = adsEnabled
            templatesNativeEnabled = adsEnabled
            emptyStateNativeEnabled = adsEnabled
            exportInterstitialEnabled = adsEnabled
            exportSuccessNativeEnabled = adsEnabled
            settingsBannerEnabled = adsEnabled
            rewardedBgRemovalEnabled = adsEnabled
            appOpenSplashEnabled = adsEnabled

            // The only honest impression signal the SDK offers. These two hooks are fed
            // straight from the real AdMob listeners — onAdImpression() for the display
            // formats — so unlike the elapsed-time heuristic the full-screen call sites
            // still have to use, this is proof the ad rendered rather than an inference.
            //
            // Declared here because AdConfig is global: there is no per-view callback on
            // WebsCareNativeView or WebsCareBannerView to hang it off. The coordinator
            // filters to the display formats, so the instrumented full-screen paths are
            // not double-counted.
            onAdImpression = { adType, adUnitId ->
                adAnalyticsCoordinator.onSdkAdImpression(adType, adUnitId)
            }
            onAdFailed = { adType, adUnitId, errorCode, errorMessage ->
                adAnalyticsCoordinator.onSdkAdLoadFailed(adType, adUnitId, errorCode, errorMessage)
            }
        }

        // Enable process-lifecycle warm-starts for App Open ads if ads enabled
        if (adsEnabled && BuildConfig.AD_APP_OPEN_SPLASH.isNotEmpty()) {
            WebsCareAds.enableAutoAppOpen(BuildConfig.AD_APP_OPEN_SPLASH, minBackgroundSeconds = 30)
            WebsCareAds.preloadAppOpen(this, BuildConfig.AD_APP_OPEN_SPLASH)
        }

        com.webscare.urducanvas.common.utils.SvgLoader.init(this)
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
        defaultDensityDpi = resources.displayMetrics.densityDpi
        suppressOemTouchBugs()
        setupLifecycleAnalytics()
    }

    private fun setupLifecycleAnalytics() {
        // Firebase persists the collection flag itself, so this is belt and braces: it
        // re-applies a stored refusal after a reinstall, where the SDK's own copy is gone
        // but the app's preference survived a backup restore.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val enabled = preferencesDataStore.getPreference(
                com.webscare.urducanvas.common.datastore.PreferenceDataStoreKeysConstants.KEY_ANALYTICS_ENABLED,
                true
            ).first()
            if (!enabled) analyticsTracker.setCollectionEnabled(false)
        }

        // Set once per process. Without it every GA4 audience that wants to separate
        // "users on the current build" from the long tail has nothing to filter on.
        analyticsTracker.setUserProperty(
            com.webscare.urducanvas.analytics.AnalyticsConstants.UserProperties.APP_VERSION,
            BuildConfig.VERSION_NAME
        )
        // The lifetime properties are counted in SharedPreferences, so they already have
        // values on launch. Pushing them here means a returning user who does nothing this
        // session still lands in the right audience.
        analyticsTracker.syncUserProfile()

        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                analyticsTracker.logAppForegrounded(sessionStateManager.currentScreen)
            }

            override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                val currentScreen = sessionStateManager.currentScreen
                val duration = sessionStateManager.getCurrentScreenDwellTimeSeconds()
                val lastAction = sessionStateManager.lastAction
                val workflow = sessionStateManager.activeWorkflowName

                if (duration > 0) {
                    analyticsTracker.logScreenLeave(
                        screenName = currentScreen,
                        durationSeconds = duration,
                        exitDirection = com.webscare.urducanvas.analytics.AnalyticsConstants.Values.EXIT_BACKGROUND,
                        lastAction = lastAction
                    )
                }

                analyticsTracker.logAppBackgrounded(
                    lastScreen = currentScreen,
                    durationSeconds = duration,
                    lastAction = lastAction,
                    workflow = workflow
                )

                adAnalyticsCoordinator.onAppBackgrounded()
            }
        })
    }

    private fun suppressOemTouchBugs() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (isOemTouchRecycleBug(throwable)) {
                FirebaseCrashlytics.getInstance().recordException(throwable)
                return@setDefaultUncaughtExceptionHandler
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Returns true if this is an OEM framework bug in touch target recycling —
     * identifiable by the fact that none of our own code appears in the stack.
     * Covers Xiaomi/MIUI, Huawei, Honor, OPPO/ColorOS, and any future OEM variant.
     */
    private fun isOemTouchRecycleBug(t: Throwable): Boolean {
        if (t !is IllegalStateException) return false
        if (t.message != "already recycled once") return false
        return t.stackTrace.none { frame ->
            frame.className.startsWith("com.webscare.urducanvas")
        }
    }
}