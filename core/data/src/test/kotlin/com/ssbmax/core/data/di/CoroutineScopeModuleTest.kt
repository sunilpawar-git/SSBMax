package com.ssbmax.core.data.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CoroutineScopeModuleTest {

    @Test
    fun provideApplicationScope_usesSupervisorJobAndDefaultDispatcher() {
        val scope = CoroutineScopeModule.provideApplicationScope()

        val job = scope.coroutineContext[Job]
        val dispatcher = scope.coroutineContext[CoroutineDispatcher]

        assertTrue(job != null && job::class.simpleName?.contains("Supervisor") == true)
        assertSame(Dispatchers.Default, dispatcher)
    }
}
