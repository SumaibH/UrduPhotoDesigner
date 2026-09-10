package com.webscare.urducanvas.analytics.navigation

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.webscare.urducanvas.R
import com.webscare.urducanvas.analytics.AnalyticsConstants.Values
import com.webscare.urducanvas.analytics.AnalyticsTracker
import com.webscare.urducanvas.analytics.session.SessionStateManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Screen telemetry for the **root** nav graph only.
 *
 * The editor's panels run on their own nested NavController. They used to share this
 * listener, and because screen state is a single slot on [SessionStateManager], opening
 * any panel ended the editor's dwell timer — the editor, the screen users spend nearly
 * all their time on, reported a few seconds and one view per visit. Panels are tools
 * rather than screens, so they now report through PanelAnalyticsListener and nest inside
 * the editor's dwell instead of replacing it.
 */
@Singleton
class NavigationAnalyticsListener @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val sessionStateManager: SessionStateManager
) : NavController.OnDestinationChangedListener {

    /**
     * Destination ids in the order we entered them.
     *
     * Navigation does not tell a listener whether it was pushed or popped to, and the
     * direction is the whole point of the exit signal — a user who backs out of export has
     * done something quite different from one who completes it. Landing on the destination
     * directly beneath the top of this stack is a pop; anything else is a push.
     */
    private val visited = ArrayDeque<Int>()

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        val screenName = getScreenNameForDestination(destination.id)
        val exitDirection = trackAndClassify(destination.id)
        val transition = sessionStateManager.onScreenChanged(screenName)

        // Log exit for previous screen if it existed
        transition.previousScreen?.let { prev ->
            if (transition.durationSeconds > 0) {
                analyticsTracker.logScreenLeave(
                    screenName = prev,
                    durationSeconds = transition.durationSeconds,
                    exitDirection = exitDirection,
                    lastAction = transition.lastAction
                )
            }
        }

        // Log entry for new screen
        analyticsTracker.logScreenView(
            screenName = screenName,
            previousScreen = transition.previousScreen,
            entryPoint = arguments?.getString("ENTRY_POINT")
        )

        if (screenName !in EDITING_FLOW) {
            endTemplateSession()
            // The design workflow ends on the same boundary and for the same reason: the
            // editor fragment is removed on the way *into* export, so its own lifecycle
            // cannot tell an abandoned attempt from one still in progress. Whether it
            // counts as completed is decided by the template session's own outcome,
            // which already knows whether an export happened.
            analyticsTracker.endDesignWorkflow()
        }
    }

    /**
     * Closes the open template session once the user leaves the editing flow entirely.
     *
     * This cannot hang off EditorFragment's lifecycle: navigating from the editor to export
     * removes that fragment, so ending the session there closed it *before* the export it
     * was on its way to. Every template session therefore reported "abandoned", the
     * completed outcome was unreachable, and the export events lost the template id they
     * were meant to carry. Leaving for home, files or templates is the real end.
     */
    private fun endTemplateSession() {
        sessionStateManager.endTemplateSession()?.let { session ->
            analyticsTracker.logTemplateSessionEnd(
                templateId = session.templateId,
                isModified = session.isModified,
                editCount = session.editCount,
                durationSeconds = session.durationSeconds,
                outcome = session.outcome
            )
        }
    }

    private companion object {
        /** Screens on which a template is still being worked on. */
        val EDITING_FLOW = setOf(
            "editor", "export", "finish_export", "preview_export",
            "bg_removal", "subscriptions", "manage_subscription"
        )
    }

    private fun trackAndClassify(destinationId: Int): String {
        val poppedBackTo = visited.size >= 2 && visited.elementAt(visited.size - 2) == destinationId
        return if (poppedBackTo) {
            visited.removeLast()
            Values.EXIT_BACK
        } else {
            if (visited.lastOrNull() != destinationId) visited.addLast(destinationId)
            Values.EXIT_FORWARD
        }
    }

    private fun getScreenNameForDestination(destinationId: Int): String {
        return when (destinationId) {
            R.id.splashFragment -> "splash"
            R.id.homeFragment -> "home"
            R.id.createFragment -> "create_canvas"
            R.id.filesFragment -> "files"
            R.id.templatesFragment -> "templates_category"
            R.id.templatesListFragment -> "templates_list"
            R.id.templateCategoriesFragment -> "templates_all_categories"
            R.id.editorFragment -> "editor"
            R.id.exportFragment -> "export"
            R.id.finishExportFragment -> "finish_export"
            R.id.previewExportFragment -> "preview_export"
            R.id.settingsFragment -> "settings"
            R.id.subscriptionsFragment -> "subscriptions"
            R.id.manageSubscriptionFragment -> "manage_subscription"
            R.id.preferencesFragment -> "preferences"
            R.id.popularFontsFragment -> "popular_fonts"
            R.id.bgRemovalFragment -> "bg_removal"
            R.id.searchFragment -> "search"
            R.id.tutorialsFragment -> "tutorials"

            // Panel destinations belong to PanelAnalyticsListener and never reach here.
            else -> "destination_$destinationId"
        }
    }
}
