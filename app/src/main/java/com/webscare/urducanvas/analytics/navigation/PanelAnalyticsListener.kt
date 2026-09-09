package com.webscare.urducanvas.analytics.navigation

import android.os.Bundle
import android.os.SystemClock
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.webscare.urducanvas.R
import com.webscare.urducanvas.analytics.AnalyticsTracker
import com.webscare.urducanvas.analytics.session.SessionStateManager
import javax.inject.Inject

/**
 * Tool telemetry for the editor's nested panel nav graph.
 *
 * Panels are tools, not screens: the user does not leave the editor to open the text
 * panel, they reach for it and come back. Reporting them as screen views cost us both
 * signals at once — the editor's dwell time was cut off at the first panel tap, and the
 * questions the panels actually answer ("which tools get opened and abandoned?") had no
 * events at all. So this reports `tool_panel_opened` / `tool_panel_closed` and leaves the
 * editor's own screen timer running underneath.
 *
 * Deliberately unscoped: one instance per [com.webscare.urducanvas.ui.editor.EditorFragment],
 * so a new editor cannot inherit a half-open panel from the last one.
 */
class PanelAnalyticsListener @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val sessionStateManager: SessionStateManager
) : NavController.OnDestinationChangedListener {

    private var openPanel: String? = null
    private var openedAtElapsedMs: Long = 0L

    /** Tool actions recorded when the panel opened, so the close can report the delta. */
    private var actionCountAtOpen: Int = 0

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        val panel = toolNameForDestination(destination.id)
        if (panel == openPanel) return

        closeOpenPanel()

        openPanel = panel
        openedAtElapsedMs = SystemClock.elapsedRealtime()
        actionCountAtOpen = sessionStateManager.toolActionCount
        analyticsTracker.logToolPanelOpened(panel, arguments?.getString("ENTRY_POINT"))
    }

    /**
     * Closes the open panel, if any.
     *
     * Call when the editor goes away — a panel left open at that point is still a panel the
     * user finished with, and without this its dwell time would never be reported.
     */
    fun onEditorClosed() {
        closeOpenPanel()
    }

    private fun closeOpenPanel() {
        val panel = openPanel ?: return
        val durationSeconds =
            ((SystemClock.elapsedRealtime() - openedAtElapsedMs) / 1000).coerceAtLeast(0)
        val actions = (sessionStateManager.toolActionCount - actionCountAtOpen).coerceAtLeast(0)

        analyticsTracker.logToolPanelClosed(
            toolName = panel,
            durationSeconds = durationSeconds,
            actionsCount = actions,
            // A panel the user opened and left without committing an edit is the
            // abandonment signal; anything that reached the undo stack counts as applied.
            wasApplied = actions > 0
        )
        openPanel = null
    }

    private fun toolNameForDestination(destinationId: Int): String {
        return when (destinationId) {
            R.id.textFragment -> "text"
            R.id.imagesFragment -> "images"
            R.id.objectsFragment -> "stickers"
            R.id.layersFragment -> "layers"
            R.id.filtersFragment -> "filters"
            R.id.colorPickerFragment -> "color_picker"
            R.id.adjustmentsFragment -> "adjustments"
            R.id.adjustmentsParentFragment -> "adjustments_parent"
            R.id.drawFragment -> "draw"
            R.id.shapesParentFragment -> "shapes"
            R.id.shapeFragment -> "shape_edit"
            R.id.textAdjustmentsFragment -> "text_adjustments"
            R.id.tableAdjustmentsFragment -> "table_adjustments"
            R.id.tablesParentFragment -> "tables"
            R.id.universalEraserFragment -> "universal_eraser"
            else -> "panel_$destinationId"
        }
    }
}
