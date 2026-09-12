package com.webscare.urducanvas.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.webscare.urducanvas.BuildConfig
import com.webscare.urducanvas.analytics.AnalyticsConstants.Events
import com.webscare.urducanvas.analytics.AnalyticsConstants.MAX_PARAM_KEY_LENGTH
import com.webscare.urducanvas.analytics.AnalyticsConstants.MAX_STRING_LENGTH
import com.webscare.urducanvas.analytics.AnalyticsConstants.Params
import com.webscare.urducanvas.analytics.AnalyticsConstants.UserProperties
import com.webscare.urducanvas.analytics.AnalyticsConstants.Values
import com.webscare.urducanvas.analytics.session.SessionStateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStateManager: SessionStateManager
) {

    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    init {
        // Default parameters ride on every event the SDK sends, including the ones it
        // collects by itself — screen_view, session_start, first_open, user_engagement.
        //
        // Stamping the two bundles this class builds covers only what the app logs, so a
        // dev build's auto-collected events were still arriving in the production stream
        // with no `traffic_type` on them at all, where the internal-traffic filter could
        // never match and exclude them. Verified on a device: the app's own events carried
        // the parameter and `screen_view(_vs)`, which the SDK logs itself, did not.
        //
        // Same condition as [stampTrafficType], so a prod release build still sends
        // nothing: that build must never mark real users' traffic as internal.
        if (!BuildConfig.IS_PROD_LOGIC || BuildConfig.DEBUG) {
            firebaseAnalytics.setDefaultEventParameters(
                Bundle().apply { putString(Params.TRAFFIC_TYPE, Values.TRAFFIC_INTERNAL) }
            )
        }
    }

    companion object {
        private const val TAG = "AnalyticsTracker"

        /** Milliseconds in a day. */
        private const val DAY_MS = 86_400_000L

        /**
         * How long a saved project sat before its owner came back to it, in days, for the
         * `days_since_edit` parameter of `project_opened` and `project_deleted`.
         *
         * One implementation, because there were two: the editor's loader and the Files list
         * each had their own copy and neither read every timestamp the app writes. Both
         * parsed with `"yyyy-MM-dd"`, which happens to work on the `"yyyy-MM-dd_HH-mm-ss"`
         * that exports, imports and templates store only because SimpleDateFormat ignores
         * whatever trails the pattern — and which fails outright on the raw millis string a
         * duplicated project gets, so every duplicate reported -1.
         *
         * Both shapes are read deliberately here rather than by accident: an all-digit value
         * is epoch millis, anything else is parsed from its leading `yyyy-MM-dd`. -1 for a
         * value that is neither, which is the documented "unknown" — a wrong number is worse
         * than a missing one in a retention metric, and 0 would read as "edited today".
         */
        fun daysSinceEdit(dateText: String?): Int {
            val raw = dateText?.trim()
            if (raw.isNullOrEmpty()) return -1
            val editedAt = if (raw.all { it.isDigit() }) {
                raw.toLongOrNull() ?: return -1
            } else {
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .parse(raw.take(10))?.time ?: return -1
                } catch (e: Exception) {
                    return -1
                }
            }
            if (editedAt <= 0L) return -1
            return ((System.currentTimeMillis() - editedAt) / DAY_MS).toInt().coerceAtLeast(0)
        }
    }

    fun logRawEvent(eventName: String, params: Map<String, Any?> = emptyMap()) {
        try {
            val bundle = Bundle()
            params.forEach { (key, value) ->
                val safeKey = key.take(MAX_PARAM_KEY_LENGTH)
                when (value) {
                    null -> {}
                    is String -> bundle.putString(safeKey, value.take(MAX_STRING_LENGTH))
                    is Int -> bundle.putInt(safeKey, value)
                    is Long -> bundle.putLong(safeKey, value)
                    is Double -> bundle.putDouble(safeKey, value)
                    is Float -> bundle.putDouble(safeKey, value.toDouble())
                    is Boolean -> bundle.putBoolean(safeKey, value)
                    else -> bundle.putString(safeKey, value.toString().take(MAX_STRING_LENGTH))
                }
            }

            // Always attach user tier
            val userTier = if (sessionStateManager.isSubscribed) Values.TIER_SUBSCRIBED else Values.TIER_FREE
            bundle.putString(Params.USER_TIER, userTier)
            stampTrafficType(bundle)

            firebaseAnalytics.logEvent(eventName.take(AnalyticsConstants.MAX_EVENT_NAME_LENGTH), bundle)
            // Debug builds only: the parameter map carries template names and file paths,
            // which have no business in a shipped device's logcat.
            if (BuildConfig.DEBUG) Log.d(TAG, "Logged event [$eventName]: $params")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event $eventName", e)
        }
    }

    /**
     * Marks events from a non-production build as internal traffic.
     *
     * All three flavours share one `applicationId` — google-services.json has a single
     * client, and an `applicationIdSuffix` breaks the build, AdMob and Play billing — so
     * every dev and debug run was reporting into the production GA4 stream alongside real
     * users. `traffic_type` is GA4's own built-in parameter: the internal-traffic data
     * filter in the console matches on this key and value, which is why it needs no
     * custom-dimension registration and why the value must stay exactly "internal".
     *
     * Stamped here and in [logStandardScreenView] because those are the only two places
     * that hand a bundle to Firebase, and the standard `screen_view` path deliberately
     * bypasses [logRawEvent].
     */
    private fun stampTrafficType(bundle: Bundle) {
        if (!BuildConfig.IS_PROD_LOGIC || BuildConfig.DEBUG) {
            bundle.putString(Params.TRAFFIC_TYPE, Values.TRAFFIC_INTERNAL)
        }
    }

    // ─── Screen & Lifecycle ───

    fun logScreenView(screenName: String, previousScreen: String? = null, entryPoint: String? = null) {
        sessionStateManager.recordAction("view_$screenName")
        logRawEvent(Events.SCREEN_VIEW_CUSTOM, mapOf(
            Params.SCREEN_NAME to screenName,
            Params.PREVIOUS_SCREEN to previousScreen,
            Params.ENTRY_POINT to entryPoint
        ))
        logStandardScreenView(screenName)
    }

    /**
     * Also reports the screen under GA4's own event name.
     *
     * This is a single-Activity app and nothing was telling Firebase which screen was on
     * show, so every event went up stamped `ga_screen_class=MainActivity` and the whole
     * built-in *Engagement → Pages and screens* surface reported one row. Screens per
     * session, per-screen engagement time, entrances and exits — all unusable.
     *
     * [Events.SCREEN_VIEW_CUSTOM] stays, because it carries the previous screen and the
     * entry point, and [logScreenLeave] carries dwell time and exit direction. GA4 cannot
     * derive any of those. This one exists so the free reports work too.
     */
    private fun logStandardScreenView(screenName: String) {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName.take(MAX_STRING_LENGTH))
                // The class name is what GA4 groups by when a screen name is missing.
                // Ours is always present, so this just keeps the two columns consistent.
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName.take(MAX_STRING_LENGTH))
            }
            stampTrafficType(bundle)
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log standard screen_view for $screenName", e)
        }
    }

    fun logScreenLeave(screenName: String, durationSeconds: Long, exitDirection: String, lastAction: String) {
        logRawEvent(Events.SCREEN_LEAVE, mapOf(
            Params.SCREEN_NAME to screenName,
            Params.DURATION_SECONDS to durationSeconds,
            Params.EXIT_DIRECTION to exitDirection,
            Params.LAST_ACTION to lastAction
        ))
    }

    fun logAppBackgrounded(lastScreen: String, durationSeconds: Long, lastAction: String, workflow: String?) {
        logRawEvent(Events.APP_BACKGROUNDED, mapOf(
            Params.SCREEN_NAME to lastScreen,
            Params.DURATION_SECONDS to durationSeconds,
            Params.LAST_ACTION to lastAction,
            Params.WORKFLOW_NAME to workflow
        ))
    }

    fun logAppForegrounded(screenName: String) {
        logRawEvent(Events.APP_FOREGROUNDED, mapOf(
            Params.SCREEN_NAME to screenName
        ))
    }

    // ─── Template System ───

    fun logTemplateImpression(templateId: Int, name: String, category: String?, isPremium: Boolean, placement: String) {
        logRawEvent(Events.TEMPLATE_IMPRESSION, mapOf(
            Params.TEMPLATE_ID to templateId,
            Params.TEMPLATE_NAME to name,
            Params.CATEGORY to category,
            Params.IS_PREMIUM to isPremium,
            Params.PLACEMENT to placement
        ))
    }

    fun logTemplateClick(templateId: Int, name: String, category: String?, isDownloaded: Boolean, isPremium: Boolean) {
        sessionStateManager.recordAction("click_template_$templateId")
        logRawEvent(Events.TEMPLATE_CLICK, mapOf(
            Params.TEMPLATE_ID to templateId,
            Params.TEMPLATE_NAME to name,
            Params.CATEGORY to category,
            Params.IS_PREMIUM to isPremium,
            "is_downloaded" to isDownloaded
        ))
    }

    fun logTemplateDownload(templateId: Int, status: String, durationMs: Long = 0, error: String? = null) {
        logRawEvent(Events.TEMPLATE_DOWNLOAD, mapOf(
            Params.TEMPLATE_ID to templateId,
            Params.WORKFLOW_STATUS to status,
            "duration_ms" to durationMs,
            Params.ERROR_MESSAGE to error
        ))
    }

    fun logTemplateOpened(templateId: Int, name: String, category: String?, isPremium: Boolean, elementCount: Int) {
        sessionStateManager.setActiveTemplate(templateId, name)
        syncUserProfile(sessionStateManager.recordTemplateCategory(category))
        sessionStateManager.recordAction("open_template_$templateId")
        startDesignWorkflow(Values.SOURCE_TEMPLATE)
        logRawEvent(Events.TEMPLATE_OPENED, mapOf(
            Params.TEMPLATE_ID to templateId,
            Params.TEMPLATE_NAME to name,
            Params.CATEGORY to category,
            Params.IS_PREMIUM to isPremium,
            "element_count" to elementCount
        ))
    }

    fun logTemplateSessionEnd(templateId: Int, isModified: Boolean, editCount: Int, durationSeconds: Long, outcome: String) {
        logRawEvent(Events.TEMPLATE_SESSION_END, mapOf(
            Params.TEMPLATE_ID to templateId,
            Params.IS_MODIFIED to isModified,
            Params.EDIT_COUNT to editCount,
            Params.DURATION_SECONDS to durationSeconds,
            "outcome" to outcome
        ))
    }

    // ─── Editor Tools & Features ───

    fun logToolPanelOpened(toolName: String, entrySource: String? = null) {
        sessionStateManager.recordAction("open_tool_$toolName")
        logRawEvent(Events.TOOL_PANEL_OPENED, mapOf(
            Params.TOOL_NAME to toolName,
            Params.ENTRY_POINT to entrySource
        ))
    }

    fun logToolActionPerformed(toolName: String, subFeature: String, actionDetail: String? = null) {
        sessionStateManager.recordAction("tool_act_${toolName}_$subFeature")
        sessionStateManager.recordToolAction()
        logRawEvent(Events.TOOL_ACTION_PERFORMED, mapOf(
            Params.TOOL_NAME to toolName,
            Params.SUB_FEATURE to subFeature,
            "action_detail" to actionDetail
        ))
    }

    fun logToolPanelClosed(toolName: String, durationSeconds: Long, actionsCount: Int, wasApplied: Boolean) {
        logRawEvent(Events.TOOL_PANEL_CLOSED, mapOf(
            Params.TOOL_NAME to toolName,
            Params.DURATION_SECONDS to durationSeconds,
            "actions_count" to actionsCount,
            Params.WAS_APPLIED to wasApplied
        ))
    }

    fun logFeatureCompleted(featureName: String, durationSeconds: Long = 0, detail: String? = null) {
        sessionStateManager.recordAction("feature_completed_$featureName")
        logRawEvent(Events.FEATURE_COMPLETED, mapOf(
            Params.FEATURE_NAME to featureName,
            Params.DURATION_SECONDS to durationSeconds,
            "detail" to detail
        ))
    }

    fun logFeatureError(featureName: String, errorType: String, errorMessage: String) {
        logRawEvent(Events.FEATURE_ERROR, mapOf(
            Params.FEATURE_NAME to featureName,
            Params.ERROR_TYPE to errorType,
            Params.ERROR_MESSAGE to errorMessage
        ))
    }

    // ─── Ads & Monetization ───

    fun logAdOpportunity(adUnitName: String, adFormat: String, triggerFeature: String, rewardTarget: String? = null) {
        sessionStateManager.recordAction("ad_opportunity_$adUnitName")
        logRawEvent(Events.AD_OPPORTUNITY, mapOf(
            Params.AD_UNIT_NAME to adUnitName,
            Params.AD_FORMAT to adFormat,
            Params.TRIGGER_FEATURE to triggerFeature,
            Params.REWARD_TARGET to rewardTarget
        ))
    }

    /**
     * [countsAsWatched] is false for the passive display formats — an in-layout native or
     * a banner the SDK fills by itself. Those are ads that appeared next to something the
     * user came for, not ads the user sat through, and the lifetime `ads_watched` user
     * property is built to band people by the latter. A Home banner firing on every visit
     * would push most of the userbase into the top bucket within a week and the audiences
     * that filter on it would stop separating anyone. The event still goes out; only the
     * lifetime counter and the post-ad outcome window are left alone.
     */
    fun logAdImpression(
        adUnitName: String,
        adFormat: String,
        screenName: String,
        triggerFeature: String,
        rewardTarget: String? = null,
        countsAsWatched: Boolean = true
    ) {
        if (countsAsWatched) {
            sessionStateManager.recordAdShown(adUnitName, triggerFeature)
            syncUserProfile(sessionStateManager.recordAdWatched())
        }
        logRawEvent(Events.AD_IMPRESSION_CUSTOM, mapOf(
            Params.AD_UNIT_NAME to adUnitName,
            Params.AD_FORMAT to adFormat,
            Params.SCREEN_NAME to screenName,
            Params.TRIGGER_FEATURE to triggerFeature,
            Params.REWARD_TARGET to rewardTarget
        ))
    }

    fun logAdRewardEarned(adUnitName: String, rewardTarget: String) {
        sessionStateManager.recordAction("reward_earned_$adUnitName")
        logRawEvent(Events.AD_REWARD_EARNED, mapOf(
            Params.AD_UNIT_NAME to adUnitName,
            Params.REWARD_TARGET to rewardTarget
        ))
    }

    fun logAdDismissed(adUnitName: String, adFormat: String, rewardEarned: Boolean) {
        sessionStateManager.recordAdDismissed(adUnitName)
        logRawEvent(Events.AD_DISMISSED, mapOf(
            Params.AD_UNIT_NAME to adUnitName,
            Params.AD_FORMAT to adFormat,
            Params.REWARD_EARNED to rewardEarned
        ))
    }

    fun logAdFailedToShow(adUnitName: String, adFormat: String, reason: String) {
        logRawEvent(Events.AD_FAILED_TO_SHOW, mapOf(
            Params.AD_UNIT_NAME to adUnitName,
            Params.AD_FORMAT to adFormat,
            Params.ERROR_MESSAGE to reason
        ))
    }

    fun logAdPostBehavior(adUnitName: String, outcome: String, latencySeconds: Long) {
        logRawEvent(Events.AD_POST_BEHAVIOR, mapOf(
            Params.AD_UNIT_NAME to adUnitName,
            Params.AD_OUTCOME to outcome,
            Params.LATENCY_SECONDS to latencySeconds
        ))
    }

    // ─── Export & Sharing ───

    fun logExportInitiated(sourceType: String, templateId: Int?, hasPremiumAssets: Boolean, elementCount: Int) {
        sessionStateManager.recordAction("export_initiated")
        logRawEvent(Events.EXPORT_INITIATED, mapOf(
            "source_type" to sourceType,
            Params.TEMPLATE_ID to templateId,
            "has_premium_assets" to hasPremiumAssets,
            "element_count" to elementCount
        ))
    }

    fun logExportOptionsSelected(format: String, resolution: String, quality: String) {
        logRawEvent(Events.EXPORT_OPTIONS_SELECTED, mapOf(
            Params.EXPORT_FORMAT to format,
            Params.EXPORT_RESOLUTION to resolution,
            "quality" to quality
        ))
    }

    fun logExportCompleted(format: String, fileSizeMb: Double, durationSeconds: Long, templateId: Int?) {
        sessionStateManager.recordAction("export_completed")
        sessionStateManager.recordTemplateExport()
        syncUserProfile(sessionStateManager.recordExport(format))
        // Decides how endDesignWorkflow() closes out — an export is what makes the
        // attempt a success, and it happens before the flow is left.
        workflowExported = true
        logRawEvent(Events.EXPORT_COMPLETED, mapOf(
            Params.EXPORT_FORMAT to format,
            Params.FILE_SIZE_MB to fileSizeMb,
            Params.DURATION_SECONDS to durationSeconds,
            Params.TEMPLATE_ID to templateId
        ))
    }

    fun logShareInitiated(channel: String, format: String, templateId: Int?) {
        sessionStateManager.recordAction("share_$channel")
        logRawEvent(Events.SHARE_INITIATED, mapOf(
            Params.SHARE_CHANNEL to channel,
            Params.EXPORT_FORMAT to format,
            Params.TEMPLATE_ID to templateId
        ))
    }

    // ─── Subscriptions & Paywall ───

    fun logPaywallViewed(source: String) {
        sessionStateManager.recordAction("view_paywall_$source")
        logRawEvent(Events.PAYWALL_VIEWED, mapOf(
            Params.ENTRY_POINT to source
        ))
    }

    /**
     * [priceMicros] and [currency] are attached only on a successful purchase, and only
     * when Play actually gave us a price. GA4 reads `value` and `currency` by name on any
     * event, so this is what puts a subscription into the monetisation reports instead of
     * leaving it as a custom event nobody's revenue report can see.
     *
     * This is a *client-side* signal and does not survive a refund. The Play-to-Firebase
     * console link remains the source of truth for real revenue; this is here so the
     * paywall funnel can be cut by plan price without joining against it.
     */
    fun logSubscriptionAction(
        planId: String,
        status: String,
        errorMessage: String? = null,
        priceMicros: Long? = null,
        currency: String? = null
    ) {
        sessionStateManager.recordAction("sub_${planId}_$status")
        val params = mutableMapOf<String, Any?>(
            Params.PLAN_ID to planId,
            Params.WORKFLOW_STATUS to status,
            Params.ERROR_MESSAGE to errorMessage
        )
        if (status == Values.STATUS_SUCCESS && priceMicros != null && priceMicros > 0) {
            params[Params.VALUE] = priceMicros / 1_000_000.0
            params[Params.CURRENCY] = currency
        }
        logRawEvent(Events.SUBSCRIPTION_ACTION, params)
    }

    // ─── Saved Projects ───

    fun logProjectSaved(elementCount: Int, canvasSize: String, sourceType: String) {
        sessionStateManager.recordAction("save_project")
        logRawEvent(Events.PROJECT_SAVED, mapOf(
            Params.ELEMENT_COUNT to elementCount,
            Params.CANVAS_SIZE to canvasSize,
            Params.SOURCE_TYPE to sourceType
        ))
    }

    /**
     * [daysSinceEdit] is the whole point of this event: how long a design sat before its
     * owner came back to it. -1 when the file has no usable timestamp.
     */
    fun logProjectOpened(elementCount: Int, canvasSize: String, daysSinceEdit: Int) {
        sessionStateManager.recordAction("open_project")
        startDesignWorkflow(Values.SOURCE_PROJECT)
        logRawEvent(Events.PROJECT_OPENED, mapOf(
            Params.ELEMENT_COUNT to elementCount,
            Params.CANVAS_SIZE to canvasSize,
            Params.DAYS_SINCE_EDIT to daysSinceEdit
        ))
    }

    fun logProjectDeleted(daysSinceEdit: Int) {
        sessionStateManager.recordAction("delete_project")
        logRawEvent(Events.PROJECT_DELETED, mapOf(
            Params.DAYS_SINCE_EDIT to daysSinceEdit
        ))
    }

    // ─── Fonts ───

    fun logFontDownload(
        fontId: String,
        fontName: String,
        language: String?,
        status: String,
        durationMs: Long = 0,
        error: String? = null
    ) {
        logRawEvent(Events.FONT_DOWNLOAD, mapOf(
            Params.FONT_ID to fontId,
            Params.FONT_NAME to fontName,
            Params.LANGUAGE to language,
            Params.WORKFLOW_STATUS to status,
            "duration_ms" to durationMs,
            Params.ERROR_MESSAGE to error
        ))
    }

    /** [justDownloaded] separates "tried the font they came for" from "reused one". */
    fun logFontApplied(fontId: String, fontName: String, language: String?, justDownloaded: Boolean) {
        sessionStateManager.recordAction("apply_font_$fontId")
        logRawEvent(Events.FONT_APPLIED, mapOf(
            Params.FONT_ID to fontId,
            Params.FONT_NAME to fontName,
            Params.LANGUAGE to language,
            "just_downloaded" to justDownloaded
        ))
    }

    // ─── Canvas Creation ───

    fun logCanvasCreated(presetName: String, canvasSize: String, isCustom: Boolean, sourceType: String) {
        sessionStateManager.recordAction("create_canvas")
        startDesignWorkflow(sourceType)
        logRawEvent(Events.CANVAS_CREATED, mapOf(
            Params.PRESET_NAME to presetName,
            Params.CANVAS_SIZE to canvasSize,
            "is_custom" to isCustom,
            Params.SOURCE_TYPE to sourceType
        ))
    }

    // ─── Search ───

    /**
     * [term] has already been sanitised by the caller. A zero [resultCount] is the
     * interesting case: those searches are a content roadmap written by users.
     */
    fun logSearch(term: String, resultCount: Int, placement: String) {
        sessionStateManager.recordAction("search")
        logRawEvent(Events.SEARCH, mapOf(
            Params.SEARCH_TERM to term,
            Params.RESULT_COUNT to resultCount,
            Params.PLACEMENT to placement,
            "is_zero_result" to (resultCount == 0)
        ))
    }

    // ─── Tutorials & Review ───

    fun logTutorialOpened(videoId: String, title: String, position: Int) {
        sessionStateManager.recordAction("open_tutorial")
        logRawEvent(Events.TUTORIAL_OPENED, mapOf(
            Params.VIDEO_ID to videoId,
            Params.TEMPLATE_NAME to title,
            Params.LIST_POSITION to position
        ))
    }

    /**
     * [status] is one of: `skipped` (the eligibility rule said no), `requested`,
     * `shown`, `failed`. Play never tells an app whether the user actually rated
     * anything, so `shown` means the flow completed, not that a review was left.
     */
    fun logReviewPrompt(status: String, trigger: String, exportCount: Int) {
        logRawEvent(Events.REVIEW_PROMPT, mapOf(
            Params.WORKFLOW_STATUS to status,
            Params.TRIGGER_FEATURE to trigger,
            "export_count" to exportCount
        ))
    }

    // ─── Workflow Funnel ───

    /**
     * A workflow is one attempt at making a design, named by where it started, and it
     * has three steps: [Values.STEP_OPENED] → [Values.STEP_COMPOSED] → [Values.STEP_EXPORTED].
     *
     * The gap between opened and composed is the reason this exists: people who reached
     * a canvas and never put anything on it are invisible to every other event, because
     * nothing they did was worth an event of its own.
     *
     * The whole funnel is driven from inside this class off events that already fire, so
     * there is one place that decides what a step is rather than a dozen call sites each
     * having an opinion.
     */
    private var workflowSource: String? = null
    private var workflowComposed = false
    private var workflowExported = false

    private fun startDesignWorkflow(sourceType: String) {
        // Restarting is correct: opening a second design abandons the first attempt, and
        // NavigationAnalyticsListener has already closed it out by the time we get here.
        workflowSource = sourceType
        workflowComposed = false
        workflowExported = false
        sessionStateManager.startWorkflow(Values.WORKFLOW_DESIGN, Values.STEP_OPENED)
        logWorkflowStep(Values.STEP_OPENED, Values.STATUS_STARTED, sourceType)
    }

    /**
     * Called when an element lands on the canvas. Only the first one in a workflow is
     * reported — the step is "this became a design", not "the user did a thing".
     */
    fun notifyCanvasComposed() {
        val source = workflowSource ?: return
        if (workflowComposed) return
        workflowComposed = true
        sessionStateManager.updateWorkflowStep(Values.STEP_COMPOSED)
        logWorkflowStep(Values.STEP_COMPOSED, Values.STATUS_SUCCESS, source)
    }

    /**
     * Closes an open workflow. Called by NavigationAnalyticsListener when the user leaves
     * the editing flow — the editor fragment's own lifecycle cannot be used, because it is
     * removed on the way *into* export.
     */
    fun endDesignWorkflow() {
        val source = workflowSource ?: return
        val status = if (workflowExported) Values.STATUS_COMPLETED else Values.STATUS_ABANDONED
        workflowSource = null
        workflowComposed = false
        workflowExported = false
        sessionStateManager.endWorkflow(status)
        logWorkflowStep(Values.STEP_EXPORTED, status, source)
    }

    private fun logWorkflowStep(step: String, status: String, sourceType: String?) {
        logRawEvent(Events.WORKFLOW_STEP, mapOf(
            Params.WORKFLOW_NAME to Values.WORKFLOW_DESIGN,
            Params.WORKFLOW_STEP to step,
            Params.WORKFLOW_STATUS to status,
            Params.SOURCE_TYPE to sourceType
        ))
    }

    // ─── User Properties ───

    /**
     * Pushes the lifetime profile to GA4.
     *
     * Called after anything that changes it, and once at startup so a returning user who
     * does nothing this session still carries the properties the audiences filter on.
     */
    fun syncUserProfile(profile: SessionStateManager.UserProfile = sessionStateManager.currentProfile()) {
        setUserProperty(UserProperties.DESIGNS_EXPORTED_BUCKET, profile.exportedBucket)
        setUserProperty(UserProperties.ADS_WATCHED_BUCKET, profile.adsWatchedBucket)
        profile.preferredFormat?.let { setUserProperty(UserProperties.PREFERRED_EXPORT_FORMAT, it) }
        profile.favouriteCategory?.let { setUserProperty(UserProperties.FAVORITE_CATEGORY, it) }
    }

    /**
     * Turns collection on or off for this device.
     *
     * Firebase stops collecting locally rather than collecting and discarding server
     * side, and the setting persists across launches on its own — so this only needs
     * calling when the user changes it, plus once at startup to re-apply a stored "off"
     * in case the app was reinstalled over a previous refusal.
     */
    fun setCollectionEnabled(enabled: Boolean) {
        try {
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set analytics collection to $enabled", e)
        }
    }

    fun setUserProperty(name: String, value: String?) {
        try {
            firebaseAnalytics.setUserProperty(name.take(24), value?.take(36))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user property $name", e)
        }
    }
}
