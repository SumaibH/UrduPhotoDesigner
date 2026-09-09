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

    companion object {
        private const val TAG = "AnalyticsTracker"
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

            firebaseAnalytics.logEvent(eventName.take(AnalyticsConstants.MAX_EVENT_NAME_LENGTH), bundle)
            // Debug builds only: the parameter map carries template names and file paths,
            // which have no business in a shipped device's logcat.
            if (BuildConfig.DEBUG) Log.d(TAG, "Logged event [$eventName]: $params")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log event $eventName", e)
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
        sessionStateManager.recordAction("open_template_$templateId")
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

    fun logAdImpression(adUnitName: String, adFormat: String, screenName: String, triggerFeature: String, rewardTarget: String? = null) {
        sessionStateManager.recordAdShown(adUnitName, triggerFeature)
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

    fun logSubscriptionAction(planId: String, status: String, errorMessage: String? = null) {
        sessionStateManager.recordAction("sub_${planId}_$status")
        logRawEvent(Events.SUBSCRIPTION_ACTION, mapOf(
            Params.PLAN_ID to planId,
            Params.WORKFLOW_STATUS to status,
            Params.ERROR_MESSAGE to errorMessage
        ))
    }

    // ─── User Properties ───

    fun setUserProperty(name: String, value: String?) {
        try {
            firebaseAnalytics.setUserProperty(name.take(24), value?.take(36))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set user property $name", e)
        }
    }
}
