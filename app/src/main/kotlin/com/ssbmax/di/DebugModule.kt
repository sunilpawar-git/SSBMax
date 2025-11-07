package com.ssbmax.di

import com.ssbmax.BuildConfig
import com.ssbmax.core.data.debug.DebugConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides debug-specific configurations
 * This module bridges app-level BuildConfig to data layer
 */
@Module
@InstallIn(SingletonComponent::class)
object DebugModule {
    
    @Provides
    @Singleton
    fun provideDebugConfig(): DebugConfig {
        return object : DebugConfig {
            override val bypassSubscriptionLimits: Boolean
                get() = BuildConfig.DEBUG && BuildConfig.BYPASS_SUBSCRIPTION_LIMITS
        }
    }
}

