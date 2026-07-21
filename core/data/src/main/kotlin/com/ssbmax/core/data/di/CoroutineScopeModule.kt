package com.ssbmax.core.data.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Provides the application-scoped coroutine scope for repositories.
 *
 * This scope is used for:
 * - Long-lived reactive streams (auth state, user profile)
 * - Background operations that outlive individual ViewModels
 * - StateFlow with SharingStarted.WhileSubscribed()
 *
 * Uses SupervisorJob so one child failure doesn't cancel others. Hilt's
 * `@ApplicationScope` qualifier existed only to disambiguate this
 * [CoroutineScope] from other bindings — there are no other [CoroutineScope]
 * bindings in this graph, so no Koin qualifier is needed.
 */
val coroutineScopeModule = module {
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
}
