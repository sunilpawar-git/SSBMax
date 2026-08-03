package com.ssbmax.di

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
 * Verifies the application-scoped [CoroutineScope] binding in [databaseModule]
 * (moved here from the deleted `core:data` module's `CoroutineScopeModuleTest`
 * in the KMP-convergence plan's Phase 9f) still uses a SupervisorJob +
 * Dispatchers.Default — WorkManagerSubmissionAnalysisTrigger relies on one
 * child failure not cancelling siblings, and on background (not main-thread)
 * execution.
 *
 * Doesn't touch the module's Room `single`s — Koin singles are lazy, so
 * loading [databaseModule] and only resolving [CoroutineScope] never invokes
 * `Room.databaseBuilder(androidContext(), ...)`, which needs a real Android
 * `Context` this plain JVM test doesn't have (see [PlatformModuleCheckTest]
 * for the full-graph version that does supply one).
 */
class DatabaseModuleTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun coroutineScopeBinding_usesSupervisorJobAndDefaultDispatcher() {
        val koinApp = startKoin { modules(databaseModule) }

        val scope = koinApp.koin.get<CoroutineScope>()
        val job = scope.coroutineContext[Job]
        val dispatcher = scope.coroutineContext[CoroutineDispatcher]

        assertTrue(job != null && job::class.simpleName?.contains("Supervisor") == true)
        assertSame(Dispatchers.Default, dispatcher)
    }
}
