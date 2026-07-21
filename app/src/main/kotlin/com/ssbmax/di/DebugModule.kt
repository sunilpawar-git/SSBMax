package com.ssbmax.di

import com.ssbmax.BuildConfig
import com.ssbmax.core.data.debug.DebugConfig
import org.koin.dsl.module

/**
 * Provides debug-specific configurations.
 * This module bridges app-level BuildConfig to data layer.
 */
val debugModule = module {
    single<DebugConfig> {
        object : DebugConfig {
            override val bypassSubscriptionLimits: Boolean
                get() = BuildConfig.DEBUG && BuildConfig.BYPASS_SUBSCRIPTION_LIMITS
        }
    }
}
