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
    private var opportunityElapsedMs: Long = 0L

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

        /**
         * Shortest plausible round trip for an ad that was actually presented, from
         * asking the SDK to show it to the completion callback. Anything faster is the
         * SDK saying it had nothing to show. Generous on purpose: counting a real
         * impression as a no-fill costs more than the reverse, because it understates
         * a number the whole ad funnel divides by.
         */
        private const val MIN_RENDER_MS = 700L

        /** The `adType` strings WebsCareAds passes to its analytics callbacks. */
        private const val FORMAT_NATIVE = "native"
        private const val FORMAT_BANNER = "banner"

        /**
         * Trigger for a slot the user did not ask for. A passive native or banner is filled
         * because a screen was opened, not because a feature needed unlocking.
         */
        private const val TRIGGER_SCREEN_LOAD = "screen_load"
    }

    /** True between requesting an ad and learning whether it actually rendered. */
    private var impressionPending = false

    init {
        sessionStateManager.setActionListener { action ->
            // "view_x" is arriving somewhere, not doing something — and the very first
            // one after a splash app-open ad is the navigation the dismissal itself
            // triggered. Counting those would make every app-open ad look like a success
            // as reliably as the old timeout made them all look like failures.
            if (!action.startsWith("view_")) onUserActivityAfterAd()
        }
    }

    fun onAdOpportunity(adUnitName: String, adFormat: String, triggerFeature: String, rewardTarget: String? = null) {
        impressionPending = true
        opportunityElapsedMs = SystemClock.elapsedRealtime()
        analyticsTracker.logAdOpportunity(adUnitName, adFormat, triggerFeature, rewardTarget)
    }

    /**
     * Friendly placement name for each configured ad unit id.
     *
     * The SDK's impression callback identifies the ad by its *resolved* unit id, and the
     * only thing the app can map that back to is the id it configured. Registered from the
     * attach points rather than hard-coded so a new placement cannot be added without also
     * naming itself here.
     *
     * Two known holes, both harmless and both reported honestly rather than guessed:
     * a debug build has `testMode` on and the SDK substitutes Google's test unit ids, so the
     * lookup misses and the format is used as the name; and the non-production flavours
     * point several placements at the same test id, so in those builds one name wins for
     * all of them. Release builds have eleven distinct ids and map cleanly.
     */
    private val placementNamesByUnitId = mutableMapOf<String, String>()

    /**
     * An ad slot handed to the SDK to fill whenever it likes — an in-layout native, an
     * in-feed native, a banner. There is no "show" call to hang an opportunity off, so the
     * attach *is* the opportunity.
     *
     * Deliberately does not set `impressionPending`. That flag belongs to the full-screen
     * heuristic in [onAdCompletionCallback], which infers an impression from how long a
     * completion callback took; a passive slot attaching while a full-screen ad was in
     * flight would hand that heuristic an opportunity timestamp belonging to something else
     * and invent an impression from it.
     */
    fun onAdSlotAttached(
        adUnitName: String,
        adUnitId: String,
        adFormat: String,
        triggerFeature: String
    ) {
        // A blank unit id is the no-ads flavour: the slot does not exist, so there was no
        // opportunity. Reporting one would put phantom ad inventory into the funnel.
        if (adUnitId.isBlank()) return
        placementNamesByUnitId[adUnitId] = adUnitName
        analyticsTracker.logAdOpportunity(adUnitName, adFormat, triggerFeature, null)
    }

    /**
     * A real AdMob impression, forwarded from `AdConfig.onAdImpression`.
     *
     * This is the callback the rest of this class has been working around: it fires from
     * the SDK's own `onAdImpression()` / `onAdShowedFullScreenContent()`, so it is proof the
     * ad rendered rather than an inference from elapsed time.
     *
     * Only the passive display formats are reported from here. The full-screen ones are
     * already instrumented at their call sites, which know the feature that triggered them
     * and the reward that was on offer — neither of which this callback carries — so
     * handling them here as well would double-count every interstitial and rewarded ad.
     *
     * Not every passive placement can reach this. `WebsCareAds.wrapWithNativeAds` builds
     * its own AdLoader inside the SDK and never invokes these config callbacks, so the six
     * in-feed placements emit `ad_opportunity` at attach and nothing further. That is the
     * same honest limitation the interstitial path has, and closing it needs a WebsCareAds
     * release, not an app change.
     */
    fun onSdkAdImpression(adType: String, resolvedAdUnitId: String) {
        if (adType != FORMAT_NATIVE && adType != FORMAT_BANNER) return
        analyticsTracker.logAdImpression(
            adUnitName = placementNamesByUnitId[resolvedAdUnitId] ?: "${adType}_unmapped",
            adFormat = adType,
            screenName = sessionStateManager.currentScreen,
            triggerFeature = TRIGGER_SCREEN_LOAD,
            rewardTarget = null,
            // A banner the user scrolled past is not an ad they watched — see the KDoc on
            // AnalyticsTracker.logAdImpression for why the lifetime bucket must not see it.
            countsAsWatched = false
        )
    }

    /**
     * A load failure for a passive slot, forwarded from `AdConfig.onAdFailed`.
     *
     * Same format filter and for the same reason: the full-screen call sites report their
     * own failures with the trigger context attached.
     */
    fun onSdkAdLoadFailed(adType: String, resolvedAdUnitId: String, errorCode: Int, errorMessage: String) {
        if (adType != FORMAT_NATIVE && adType != FORMAT_BANNER) return
        analyticsTracker.logAdFailedToShow(
            adUnitName = placementNamesByUnitId[resolvedAdUnitId] ?: "${adType}_unmapped",
            adFormat = adType,
            reason = "load_failed_$errorCode: $errorMessage"
        )
    }

    /**
     * Records an impression for a format whose SDK cannot tell a shown ad from a skipped
     * one, using how long the completion callback took to come back.
     *
     * `WebsCareAds.showInterstitial` and `showAppOpen` expose a single completion lambda
     * that fires whether the ad played or there was nothing to play, so every one of
     * those call sites reported an impression optimistically — a no-fill counted as an ad
     * the user saw, and fill rate plus everything derived from it read high.
     *
     * A real full-screen ad cannot be presented, watched and dismissed inside
     * [MIN_RENDER_MS]; a no-fill returns almost immediately. So the elapsed time is the
     * signal. This is a heuristic and is labelled as one: the clean fix is a WebsCareAds
     * release that reports shown and skipped separately, at which point these call sites
     * move to [onAdShown] like the rewarded ones already did.
     */
    fun onAdCompletionCallback(
        adUnitName: String,
        adFormat: String,
        screenName: String,
        triggerFeature: String
    ): Boolean {
        if (!impressionPending) return false
        impressionPending = false
        val elapsed = SystemClock.elapsedRealtime() - opportunityElapsedMs
        if (elapsed < MIN_RENDER_MS) {
            analyticsTracker.logAdFailedToShow(adUnitName, adFormat, "no_fill_or_skipped")
            return false
        }
        activeAdUnitName = adUnitName
        analyticsTracker.logAdImpression(adUnitName, adFormat, screenName, triggerFeature, null)
        return true
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

    /**
     * Any sign that the user is still using the app after an ad was dismissed.
     *
     * An app-open ad gates the app rather than a feature, so nothing ever called
     * [onFeatureActionCompleted] for it and its window could only ever expire — every
     * single app-open ad reported `abandoned_feature` at exactly 30 seconds, which
     * dragged the whole outcome distribution with it. For that placement "did the ad
     * cost us the session" is the real question, and a screen view or a tool action
     * inside the window is the answer.
     *
     * Deliberately weaker than [onFeatureActionCompleted]: it only resolves a window
     * that is still open, so an ad shown for a feature is still judged on that feature.
     */
    fun onUserActivityAfterAd() {
        if (activeAdUnitName == null) return
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
