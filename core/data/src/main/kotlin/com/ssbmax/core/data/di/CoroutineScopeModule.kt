package com.ssbmax.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier annotation for application-scoped coroutine scope.
 * Used for long-lived reactive streams in repositories.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Provides application-scoped coroutine scope for repositories.
 * 
 * This scope is used for:
 * - Long-lived reactive streams (auth state, user profile)
 * - Background operations that outlive individual ViewModels
 * - StateFlow with SharingStarted.WhileSubscribed()
 * 
 * Uses SupervisorJob to ensure one child failure doesn't cancel others.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {
    
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

