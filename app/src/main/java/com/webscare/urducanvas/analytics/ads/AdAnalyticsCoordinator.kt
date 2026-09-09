package com.webscare.urducanvas.analytics.ads

import android.os.SystemClock
import com.webscare.urducanvas.analytics.AnalyticsConstants.Values
import com.webscare.urducanvas.analytics.AnalyticsTracker
import com.webscare.urducanvas.analytics.session.SessionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdAnalyticsCoordinator @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val sessionStateManager: SessionStateManager
) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var pendingOutcomeJob: Job? = null
    private var activeAdUnitName: String? = null
    private var adDismissedTimeElapsedMs: Long = 0L

    fun onAdOpportunity(adUnitName: String, adFormat: String, triggerFeature: String, rewardTarget: String? = null) {
        analyticsTracker.logAdOpportunity(adUnitName, adFormat, triggerFeature, rewardTarget)
    }

    fun onAdImpression(adUnitName: String, adFormat: String, screenName: String, triggerFeature: String, rewardTarget: String? = null) {
        activeAdUnitName = adUnitName
        analyticsTracker.logAdImpression(adUnitName, adFormat, screenName, triggerFeature, rewardTarget)
    }

    fun onAdRewardEarned(adUnitName: String, rewardTarget: String) {
        analyticsTracker.logAdRewardEarned(adUnitName, rewardTarget)
    }

    fun onAdDismissed(adUnitName: String, adFormat: String, rewardEarned: Boolean) {
        analyticsTracker.logAdDismissed(adUnitName, adFormat, rewardEarned)
        adDismissedTimeElapsedMs = SystemClock.elapsedRealtime()
        activeAdUnitName = adUnitName

        // Start 30-second window to detect outcome if not explicitly resolved earlier
        pendingOutcomeJob?.cancel()
        pendingOutcomeJob = scope.launch {
            delay(30_000)
            // If 30 seconds elapse without explicit continuation or abandonment, check if still active
            if (activeAdUnitName == adUnitName) {
                activeAdUnitName = null
                sessionStateManager.clearAdMonitoring()
            }
        }
    }

    fun onAdFailedToShow(adUnitName: String, adFormat: String, reason: String) {
        analyticsTracker.logAdFailedToShow(adUnitName, adFormat, reason)
    }

    /** Call when user continues using the feature unlocked by the ad (e.g., runs segmentation or exports) */
    fun onFeatureActionCompleted(featureName: String) {
        val adUnit = activeAdUnitName
        if (adUnit != null && adDismissedTimeElapsedMs > 0) {
            val latency = ((SystemClock.elapsedRealtime() - adDismissedTimeElapsedMs) / 1000).coerceAtLeast(0)
            if (latency <= 30) {
                analyticsTracker.logAdPostBehavior(adUnit, Values.AD_OUTCOME_CONTINUED, latency)
                pendingOutcomeJob?.cancel()
                activeAdUnitName = null
                sessionStateManager.clearAdMonitoring()
            }
        }
    }

    /** Call when user backs out or cancels the feature after an ad */
    fun onFeatureAbandoned(featureName: String) {
        val adUnit = activeAdUnitName
        if (adUnit != null && adDismissedTimeElapsedMs > 0) {
            val latency = ((SystemClock.elapsedRealtime() - adDismissedTimeElapsedMs) / 1000).coerceAtLeast(0)
            if (latency <= 30) {
                analyticsTracker.logAdPostBehavior(adUnit, Values.AD_OUTCOME_ABANDONED, latency)
                pendingOutcomeJob?.cancel()
                activeAdUnitName = null
                sessionStateManager.clearAdMonitoring()
            }
        }
    }

    /** Call when the entire app enters background */
    fun onAppBackgrounded() {
        val adUnit = activeAdUnitName
        if (adUnit != null && adDismissedTimeElapsedMs > 0) {
            val latency = ((SystemClock.elapsedRealtime() - adDismissedTimeElapsedMs) / 1000).coerceAtLeast(0)
            if (latency <= 15) {
                // Left app shortly after ad dismissal
                analyticsTracker.logAdPostBehavior(adUnit, Values.AD_OUTCOME_LEFT_APP, latency)
            }
            pendingOutcomeJob?.cancel()
            activeAdUnitName = null
            sessionStateManager.clearAdMonitoring()
        }
    }
}
