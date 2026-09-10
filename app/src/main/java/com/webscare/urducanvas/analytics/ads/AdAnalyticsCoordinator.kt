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

    companion object {
        /**
         * How long after an ad is dismissed its outcome still counts as caused by the ad.
         *
         * One window for every outcome. Backgrounding used to use a shorter cutoff of its
         * own, so a user who put the phone down twenty seconds after a rewarded ad fell
         * between the two and produced no event at all — which quietly flattered the
         * post-ad completion rate, because only the users who continued were ever counted.
         */
        private const val OUTCOME_WINDOW_SECONDS = 30L
        private const val OUTCOME_WINDOW_MS = OUTCOME_WINDOW_SECONDS * 1000
    }

    /** True between requesting an ad and learning whether it actually rendered. */
    private var impressionPending = false

    fun onAdOpportunity(adUnitName: String, adFormat: String, triggerFeature: String, rewardTarget: String? = null) {
        impressionPending = true
        analyticsTracker.logAdOpportunity(adUnitName, adFormat, triggerFeature, rewardTarget)
    }

    /**
     * Records that the ad was actually put in front of the user.
     *
     * Callers may invoke this from more than one callback — a rewarded ad reports both
     * "reward earned" and "dismissed", and either is proof it rendered — so only the first
     * call after an opportunity emits anything.
     *
     * This used to be called *before* asking the SDK to show the ad, which meant an
     * impression was recorded even when the very next callback was onNotReady. Ads that
     * never appeared were counted as seen, so fill rate and every per-impression figure
     * derived from it read high.
     */
    fun onAdShown(adUnitName: String, adFormat: String, screenName: String, triggerFeature: String, rewardTarget: String? = null) {
        if (!impressionPending) return
        impressionPending = false
        activeAdUnitName = adUnitName
        analyticsTracker.logAdImpression(adUnitName, adFormat, screenName, triggerFeature, rewardTarget)
    }

    /**
     * Kept for the formats whose SDK entry point gives no way to tell a shown ad from a
     * skipped one — interstitial and app-open expose a single completion callback. Those
     * call sites still report optimistically; prefer [onAdShown] wherever the SDK
     * distinguishes the two.
     */
    fun onAdImpression(adUnitName: String, adFormat: String, screenName: String, triggerFeature: String, rewardTarget: String? = null) {
        impressionPending = false
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

        // Open the outcome window. Expiring without the user doing anything is itself the
        // answer — they watched the ad and then dropped the feature — so the timeout has to
        // report abandonment rather than just clearing the state, which is what it used to do.
        pendingOutcomeJob?.cancel()
        pendingOutcomeJob = scope.launch {
            delay(OUTCOME_WINDOW_MS)
            if (activeAdUnitName == adUnitName) {
                analyticsTracker.logAdPostBehavior(
                    adUnitName,
                    Values.AD_OUTCOME_ABANDONED,
                    OUTCOME_WINDOW_SECONDS
                )
                activeAdUnitName = null
                sessionStateManager.clearAdMonitoring()
            }
        }
    }

    fun onAdFailedToShow(adUnitName: String, adFormat: String, reason: String) {
        // Nothing rendered, so the pending impression must not be emitted later.
        impressionPending = false
        analyticsTracker.logAdFailedToShow(adUnitName, adFormat, reason)
    }

    /** Call when user continues using the feature unlocked by the ad (e.g., runs segmentation or exports) */
    fun onFeatureActionCompleted(featureName: String) {
        resolveOutcome(Values.AD_OUTCOME_CONTINUED)
    }

    /** Call when user backs out or cancels the feature after an ad */
    fun onFeatureAbandoned(featureName: String) {
        resolveOutcome(Values.AD_OUTCOME_ABANDONED)
    }

    /** Call when the entire app enters background */
    fun onAppBackgrounded() {
        resolveOutcome(Values.AD_OUTCOME_LEFT_APP)
    }

    /**
     * Closes the open outcome window with [outcome], if one is open and still in date.
     *
     * Past the window the ad is no longer a plausible cause of what the user did, so the
     * outcome goes unreported — but the window still closes, or the next ad would inherit
     * a stale dismissal timestamp.
     */
    private fun resolveOutcome(outcome: String) {
        val adUnit = activeAdUnitName ?: return
        if (adDismissedTimeElapsedMs <= 0) return

        val latency = ((SystemClock.elapsedRealtime() - adDismissedTimeElapsedMs) / 1000).coerceAtLeast(0)
        if (latency <= OUTCOME_WINDOW_SECONDS) {
            analyticsTracker.logAdPostBehavior(adUnit, outcome, latency)
        }
        pendingOutcomeJob?.cancel()
        activeAdUnitName = null
        sessionStateManager.clearAdMonitoring()
    }
}
