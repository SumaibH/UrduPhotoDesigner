package com.webscare.urducanvas.analytics

object AnalyticsConstants {

    // ─── Max Length Constraints (GA4 Rules) ───
    const val MAX_STRING_LENGTH = 100
    const val MAX_PARAM_KEY_LENGTH = 40
    const val MAX_EVENT_NAME_LENGTH = 40

    // ─── Event Names (Max 40 chars) ───
    object Events {
        // Navigation & Screen Lifecycle
        const val SCREEN_VIEW_CUSTOM = "screen_view_custom"
        const val SCREEN_LEAVE = "screen_leave"
        const val APP_BACKGROUNDED = "app_backgrounded"
        const val APP_FOREGROUNDED = "app_foregrounded"
        const val SESSION_TERMINATED_ABNORMALLY = "session_term_abnormal"

        // Template System
        const val TEMPLATE_IMPRESSION = "template_impression"
        const val TEMPLATE_CLICK = "template_click"
        const val TEMPLATE_DOWNLOAD = "template_download"
        const val TEMPLATE_OPENED = "template_opened"
        const val TEMPLATE_SESSION_END = "template_session_end"

        // Editor Tools & Features
        const val TOOL_PANEL_OPENED = "tool_panel_opened"
        const val TOOL_ACTION_PERFORMED = "tool_action_performed"
        const val TOOL_PANEL_CLOSED = "tool_panel_closed"
        const val FEATURE_COMPLETED = "feature_completed"
        const val FEATURE_ERROR = "feature_error"

        // Ads & Monetization
        const val AD_OPPORTUNITY = "ad_opportunity"
        const val AD_IMPRESSION_CUSTOM = "ad_impression_custom"
        const val AD_REWARD_EARNED = "ad_reward_earned"
        const val AD_DISMISSED = "ad_dismissed"
        const val AD_FAILED_TO_SHOW = "ad_failed_to_show"
        const val AD_POST_BEHAVIOR = "ad_post_behavior"

        // Export & Sharing
        const val EXPORT_INITIATED = "export_initiated"
        const val EXPORT_OPTIONS_SELECTED = "export_options_selected"
        const val EXPORT_COMPLETED = "export_completed"
        const val SHARE_INITIATED = "share_initiated"

        // Subscriptions & Paywall
        const val PAYWALL_VIEWED = "paywall_viewed"
        const val SUBSCRIPTION_ACTION = "subscription_action"

        // Workflow / Funnel
        const val WORKFLOW_STEP = "workflow_step"
    }

    // ─── Parameter Keys (Max 40 chars, consolidated into 28 keys) ───
    object Params {
        const val SCREEN_NAME = "screen_name"
        const val PREVIOUS_SCREEN = "previous_screen"
        const val ENTRY_POINT = "entry_point"
        const val EXIT_DIRECTION = "exit_direction"
        const val LAST_ACTION = "last_action"
        const val DURATION_SECONDS = "duration_seconds"
        const val TEMPLATE_ID = "template_id"
        const val TEMPLATE_NAME = "template_name"
        const val CATEGORY = "category"
        const val SUBCATEGORY = "subcategory"
        const val IS_PREMIUM = "is_premium"
        const val IS_MODIFIED = "is_modified"
        const val EDIT_COUNT = "edit_count"
        const val TOOL_NAME = "tool_name"
        const val SUB_FEATURE = "sub_feature"
        const val WAS_APPLIED = "was_applied"
        const val FEATURE_NAME = "feature_name"
        const val ERROR_TYPE = "error_type"
        const val ERROR_MESSAGE = "error_message"
        const val AD_UNIT_NAME = "ad_unit_name"
        const val AD_FORMAT = "ad_format"
        const val TRIGGER_FEATURE = "trigger_feature"
        const val REWARD_TARGET = "reward_target"
        const val AD_OUTCOME = "ad_outcome"
        const val EXPORT_FORMAT = "export_format"
        const val EXPORT_RESOLUTION = "export_resolution"
        const val SHARE_CHANNEL = "share_channel"
        const val WORKFLOW_NAME = "workflow_name"
        const val WORKFLOW_STEP = "workflow_step_name"
        const val WORKFLOW_STATUS = "workflow_status"
        const val PLAN_ID = "plan_id"
        const val USER_TIER = "user_tier"
        const val PLACEMENT = "placement"
        const val LATENCY_SECONDS = "latency_seconds"
        const val FILE_SIZE_MB = "file_size_mb"
        const val REWARD_EARNED = "reward_earned"
    }

    // ─── User Property Keys (Max 24 chars) ───
    object UserProperties {
        const val SUBSCRIPTION_STATUS = "sub_status"
        const val DESIGNS_EXPORTED_BUCKET = "designs_exported"
        const val FAVORITE_CATEGORY = "fav_category"
        const val PREFERRED_EXPORT_FORMAT = "pref_format"
        const val ADS_WATCHED_BUCKET = "ads_watched"
        const val APP_VERSION = "app_version"
    }

    // ─── Standardized Values ───
    object Values {
        const val TIER_FREE = "free"
        const val TIER_SUBSCRIBED = "subscribed"

        const val EXIT_BACK = "back"
        const val EXIT_FORWARD = "forward"
        const val EXIT_BACKGROUND = "background"

        const val AD_OUTCOME_CONTINUED = "continued_feature"
        const val AD_OUTCOME_ABANDONED = "abandoned_feature"
        const val AD_OUTCOME_LEFT_APP = "left_app"

        const val STATUS_STARTED = "started"
        const val STATUS_SUCCESS = "success"
        const val STATUS_FAILED = "failed"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_ABANDONED = "abandoned"
    }
}
