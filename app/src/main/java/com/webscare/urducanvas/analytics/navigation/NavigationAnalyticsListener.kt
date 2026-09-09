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

@Singleton
class NavigationAnalyticsListener @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val sessionStateManager: SessionStateManager
) : NavController.OnDestinationChangedListener {

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        val screenName = getScreenNameForDestination(destination.id)
        val transition = sessionStateManager.onScreenChanged(screenName)

        // Log exit for previous screen if it existed
        transition.previousScreen?.let { prev ->
            if (transition.durationSeconds > 0) {
                analyticsTracker.logScreenLeave(
                    screenName = prev,
                    durationSeconds = transition.durationSeconds,
                    exitDirection = Values.EXIT_FORWARD,
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

            // Panel Nav Graph Destinations
            R.id.textFragment -> "panel_text"
            R.id.imagesFragment -> "panel_images"
            R.id.objectsFragment -> "panel_stickers"
            R.id.layersFragment -> "panel_layers"
            R.id.filtersFragment -> "panel_filters"
            R.id.colorPickerFragment -> "panel_color_picker"
            R.id.adjustmentsFragment -> "panel_adjustments"
            R.id.adjustmentsParentFragment -> "panel_adjustments_parent"
            R.id.drawFragment -> "panel_draw"
            R.id.shapesParentFragment -> "panel_shapes"
            R.id.shapeFragment -> "panel_shape_edit"
            R.id.textAdjustmentsFragment -> "panel_text_adjustments"
            R.id.tableAdjustmentsFragment -> "panel_table_adjustments"
            R.id.tablesParentFragment -> "panel_tables"
            R.id.universalEraserFragment -> "panel_universal_eraser"

            else -> "destination_$destinationId"
        }
    }
}
