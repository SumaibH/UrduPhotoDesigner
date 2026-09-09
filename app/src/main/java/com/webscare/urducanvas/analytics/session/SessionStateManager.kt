package com.webscare.urducanvas.analytics.session

import android.content.Context
import android.os.SystemClock
import com.webscare.urducanvas.analytics.AnalyticsConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val prefs = context.getSharedPreferences("analytics_session_state", Context.MODE_PRIVATE)

    var sessionId: String = UUID.randomUUID().toString()
        private set

    private val _currentScreen = AtomicReference("splash")
    val currentScreen: String get() = _currentScreen.get()

    private val _previousScreen = AtomicReference<String?>(null)
    val previousScreen: String? get() = _previousScreen.get()

    private var screenStartTimeElapsedMs: Long = SystemClock.elapsedRealtime()

    private val _lastAction = AtomicReference("app_launch")
    val lastAction: String get() = _lastAction.get()

    private var lastActionTimestampMs: Long = System.currentTimeMillis()

    var activeWorkflowName: String? = null
        private set

    var activeWorkflowStep: String? = null
        private set

    var activeTemplateId: Int? = null
        private set

    var activeTemplateName: String? = null
        private set

    var lastAdUnitShown: String? = null
        private set

    var lastAdDismissedTimeElapsedMs: Long? = null
        private set

    var lastAdTriggerFeature: String? = null
        private set

    var isSubscribed: Boolean = false

    data class ScreenTransition(
        val previousScreen: String?,
        val newScreen: String,
        val durationSeconds: Long,
        val lastAction: String
    )

    fun onScreenChanged(newScreenName: String): ScreenTransition {
        val now = SystemClock.elapsedRealtime()
        val prev = _currentScreen.get()
        val durationSec = if (screenStartTimeElapsedMs > 0) {
            ((now - screenStartTimeElapsedMs) / 1000).coerceAtLeast(0)
        } else 0L

        _previousScreen.set(prev)
        _currentScreen.set(newScreenName)
        screenStartTimeElapsedMs = now

        val transition = ScreenTransition(
            previousScreen = prev,
            newScreen = newScreenName,
            durationSeconds = durationSec,
            lastAction = _lastAction.get()
        )

        persistStateSnapshot()
        return transition
    }

    fun getCurrentScreenDwellTimeSeconds(): Long {
        val now = SystemClock.elapsedRealtime()
        return ((now - screenStartTimeElapsedMs) / 1000).coerceAtLeast(0)
    }

    fun recordAction(actionName: String) {
        val sanitized = actionName.take(AnalyticsConstants.MAX_STRING_LENGTH)
        _lastAction.set(sanitized)
        lastActionTimestampMs = System.currentTimeMillis()
        persistStateSnapshot()
    }

    /**
     * Committed editor actions so far this process.
     *
     * Monotonic and never reset: callers snapshot it and compare, which is how a tool panel
     * tells "the user applied three edits" from "the user opened me and left".
     */
    private val _toolActionCount = AtomicInteger(0)
    val toolActionCount: Int get() = _toolActionCount.get()

    fun recordToolAction() {
        _toolActionCount.incrementAndGet()
    }

    fun setActiveTemplate(templateId: Int?, templateName: String?) {
        this.activeTemplateId = templateId
        this.activeTemplateName = templateName?.take(AnalyticsConstants.MAX_STRING_LENGTH)
        templateOpenedAtElapsedMs = SystemClock.elapsedRealtime()
        toolActionCountAtTemplateOpen = toolActionCount
        exportedInTemplateSession = false
        persistStateSnapshot()
    }

    private var templateOpenedAtElapsedMs: Long = 0L
    private var toolActionCountAtTemplateOpen: Int = 0
    private var exportedInTemplateSession = false

    /** Call when an export completes, so the template session can close as a success. */
    fun recordTemplateExport() {
        exportedInTemplateSession = true
    }

    /** How long a template was open for, and how much was done to it. */
    data class TemplateSession(
        val templateId: Int,
        val durationSeconds: Long,
        val editCount: Int,
        val isModified: Boolean,
        val outcome: String
    )

    /**
     * Ends the open template session and returns what it amounted to, or null if no
     * template was open.
     *
     * Editing is counted from committed canvas actions rather than from time on the
     * screen, so a template someone opened and stared at does not read as engagement.
     */
    fun endTemplateSession(): TemplateSession? {
        val id = activeTemplateId ?: return null
        val edits = (toolActionCount - toolActionCountAtTemplateOpen).coerceAtLeast(0)
        val duration = if (templateOpenedAtElapsedMs > 0) {
            ((SystemClock.elapsedRealtime() - templateOpenedAtElapsedMs) / 1000).coerceAtLeast(0)
        } else 0L
        val outcome = if (exportedInTemplateSession) {
            AnalyticsConstants.Values.STATUS_COMPLETED
        } else {
            AnalyticsConstants.Values.STATUS_ABANDONED
        }

        activeTemplateId = null
        activeTemplateName = null
        templateOpenedAtElapsedMs = 0L
        exportedInTemplateSession = false
        persistStateSnapshot()

        return TemplateSession(id, duration, edits, edits > 0, outcome)
    }

    fun startWorkflow(workflowName: String, initialStep: String) {
        this.activeWorkflowName = workflowName
        this.activeWorkflowStep = initialStep
        recordAction("start_workflow_$workflowName")
    }

    fun updateWorkflowStep(step: String) {
        this.activeWorkflowStep = step
        recordAction("workflow_step_$step")
    }

    fun endWorkflow(status: String) {
        recordAction("end_workflow_${activeWorkflowName}_$status")
        this.activeWorkflowName = null
        this.activeWorkflowStep = null
        persistStateSnapshot()
    }

    fun recordAdShown(adUnitName: String, triggerFeature: String) {
        this.lastAdUnitShown = adUnitName
        this.lastAdTriggerFeature = triggerFeature
        recordAction("ad_shown_$adUnitName")
    }

    fun recordAdDismissed(adUnitName: String) {
        this.lastAdUnitShown = adUnitName
        this.lastAdDismissedTimeElapsedMs = SystemClock.elapsedRealtime()
        recordAction("ad_dismissed_$adUnitName")
    }

    fun clearAdMonitoring() {
        this.lastAdDismissedTimeElapsedMs = null
    }

    fun startNewSession() {
        sessionId = UUID.randomUUID().toString()
        screenStartTimeElapsedMs = SystemClock.elapsedRealtime()
        _lastAction.set("new_session")
        persistStateSnapshot()
    }

    private fun persistStateSnapshot() {
        prefs.edit()
            .putString("session_id", sessionId)
            .putString("last_screen", _currentScreen.get())
            .putString("previous_screen", _previousScreen.get())
            .putString("last_action", _lastAction.get())
            .putLong("last_action_timestamp", lastActionTimestampMs)
            .putString("workflow_name", activeWorkflowName)
            .putString("workflow_step", activeWorkflowStep)
            .putInt("active_template_id", activeTemplateId ?: -1)
            .apply()
    }
}
