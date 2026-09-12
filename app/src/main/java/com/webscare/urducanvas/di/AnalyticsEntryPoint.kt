package com.webscare.urducanvas.di

import com.webscare.urducanvas.analytics.AnalyticsTracker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Reaches the singleton tracker from helpers Hilt cannot inject into — plain classes and
 * custom views rather than fragments.
 *
 * Editor code used to get there through `CanvasViewModel.logToolAction`, which works only
 * where that view model already exists. The asset preview now also opens from the
 * navigation screens, where building the editor's view model just to log one event would
 * construct the whole canvas graph on Home. The tracker itself is `@Singleton`, so asking
 * for it directly costs nothing either way.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AnalyticsEntryPoint {
    fun analyticsTracker(): AnalyticsTracker
}
