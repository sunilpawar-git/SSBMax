package com.ssbmax.core.data.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * Verifies the application-scoped [CoroutineScope] Koin binding (converted
 * from Hilt's `CoroutineScopeModule.provideApplicationScope()`) still uses a
 * SupervisorJob + Dispatchers.Default — repositories like AuthRepositoryImpl
 * and OIRQuestionCacheManager rely on one child failure not cancelling
 * siblings, and on background (not main-thread) execution.
 */
class CoroutineScopeModuleTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun coroutineScopeModule_bindsSupervisorJobAndDefaultDispatcher() {
        val koinApp = startKoin { modules(coroutineScopeModule) }

        val scope = koinApp.koin.get<CoroutineScope>()
        val job = scope.coroutineContext[Job]
        val dispatcher = scope.coroutineContext[CoroutineDispatcher]

        assertTrue(job != null && job::class.simpleName?.contains("Supervisor") == true)
        assertSame(Dispatchers.Default, dispatcher)
    }
}
