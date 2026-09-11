package com.webscare.urducanvas.analytics

/**
 * Event, parameter and value taxonomy for GA4.
 *
 * **Declared but never emitted yet.** Nothing below fires unless something calls it, and
 * a name sitting in this file is not evidence that it does. One name is deliberately
 * unbuilt rather than overlooked — do not build a report on it until it is wired:
 *
 * - [Events.SESSION_TERMINATED_ABNORMALLY] — needs the persisted snapshot to be read back
 *   and reconciled on the next launch.
 *
 * Everything else here is emitted.
 *
 * [Events.WORKFLOW_STEP] and the `startWorkflow`/`updateWorkflowStep`/`endWorkflow` API on
 * SessionStateManager used to be listed above as unbuilt; they are not. The design funnel
 * is driven entirely from inside `AnalyticsTracker` — started by `logTemplateOpened`,
 * `logProjectOpened` and `logCanvasCreated`, advanced by `notifyCanvasComposed` on the
 * first committed canvas action, and closed by `NavigationAnalyticsListener` when the user
 * leaves the editing flow. The gap between `opened` and `composed` is the reason it exists.
 *
 * [Events.TEMPLATE_IMPRESSION] comes from `TemplateImpressionTracker` on real viewport
 * visibility rather than on bind, and all six [UserProperties] are set — the four lifetime
 * ones from counters `SessionStateManager` keeps in SharedPreferences so they survive
 * process death.
 *
 * Every custom parameter here is also invisible in GA4 reports until it is registered as
 * a custom dimension in the Firebase console. That is console work, not code.
 */
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

        // Saved projects. Reopening your own work is the earliest reliable signal that
        // the app has become a habit rather than a thing someone tried once.
        const val PROJECT_SAVED = "project_saved"
        const val PROJECT_OPENED = "project_opened"
        const val PROJECT_DELETED = "project_deleted"

        // Fonts — the reason a lot of people install an Urdu design app in the first place.
        const val FONT_DOWNLOAD = "font_download"
        const val FONT_APPLIED = "font_applied"

        // The blank-canvas half of Home's two entry points.
        const val CANVAS_CREATED = "canvas_created"

        /**
         * GA4's own recommended event name, not a custom one, so it shows up in the
         * standard search reports without an Exploration having to be built by hand.
         */
        const val SEARCH = "search"

        const val TUTORIAL_OPENED = "tutorial_opened"
        const val REVIEW_PROMPT = "review_prompt"
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

        /**
         * GA4's own built-in parameter, not a custom one — the internal-traffic data
         * filter in the console matches on exactly this key and value, so it needs no
         * custom-dimension registration and must not be renamed.
         *
         * Stamped on every event from a non-production build. All three flavours share
         * one `applicationId` (google-services.json has a single client, and a suffix
         * breaks AdMob and Play billing), so without this the dev and debug builds
         * report into the production stream and are indistinguishable from real users.
         */
        const val TRAFFIC_TYPE = "traffic_type"
        const val LATENCY_SECONDS = "latency_seconds"
        const val FILE_SIZE_MB = "file_size_mb"
        const val REWARD_EARNED = "reward_earned"

        const val ELEMENT_COUNT = "element_count"
        const val DAYS_SINCE_EDIT = "days_since_edit"
        const val SOURCE_TYPE = "source_type"
        const val FONT_ID = "font_id"
        const val FONT_NAME = "font_name"
        const val LANGUAGE = "language"
        const val CANVAS_SIZE = "canvas_size"
        const val PRESET_NAME = "preset_name"
        const val RESULT_COUNT = "result_count"
        const val VIDEO_ID = "video_id"
        const val LIST_POSITION = "list_position"

        /** GA4's recommended parameter name for [Events.SEARCH]. */
        const val SEARCH_TERM = "search_term"

        // Revenue. GA4 reads these two by name on any event, so a purchase reported
        // with them lands in the monetisation reports rather than only in Explorations.
        const val VALUE = "value"
        const val CURRENCY = "currency"
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

        /** The value GA4's built-in internal-traffic filter looks for. Do not translate. */
        const val TRAFFIC_INTERNAL = "internal"

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

        // ── Workflows ─────────────────────────────────────────────────────────
        //
        // A workflow is one attempt at making a design, named by where it started, and
        // it has three steps. The gap between "opened" and "composed" is the drop-off
        // no other event captures: people who reached a canvas and never put anything
        // on it.
        const val WORKFLOW_DESIGN = "design"
        const val STEP_OPENED = "opened"
        const val STEP_COMPOSED = "composed"
        const val STEP_EXPORTED = "exported"

        const val SOURCE_TEMPLATE = "template"
        const val SOURCE_BLANK = "blank"
        const val SOURCE_PHOTO = "photo"
        const val SOURCE_PROJECT = "project"
    }
}
