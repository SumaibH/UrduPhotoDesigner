package com.webscare.urducanvas.analytics.di

import android.content.Context
import com.webscare.urducanvas.analytics.AnalyticsTracker
import com.webscare.urducanvas.analytics.session.SessionStateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideSessionStateManager(
        @ApplicationContext context: Context
    ): SessionStateManager {
        return SessionStateManager(context)
    }

    @Provides
    @Singleton
    fun provideAnalyticsTracker(
        @ApplicationContext context: Context,
        sessionStateManager: SessionStateManager
    ): AnalyticsTracker {
        return AnalyticsTracker(context, sessionStateManager)
    }
}
