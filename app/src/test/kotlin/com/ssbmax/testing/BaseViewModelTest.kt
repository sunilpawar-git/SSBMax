package com.ssbmax.testing

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Before
import org.junit.Rule

/**
 * Base class for ViewModel tests
 * Provides coroutine test dispatcher setup and mocks Android Log
 * 
 * Usage:
 * ```
 * class MyViewModelTest : BaseViewModelTest() {
 *     @Test
 *     fun `test something`() = runTest {
 *         // Test implementation
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseViewModelTest {
    
    @get:Rule
    val dispatcherRule = TestDispatcherRule()
    
    /**
     * Mock Android Log for unit tests
     * This prevents "Method d in android.util.Log not mocked" errors
     */
    @Before
    fun mockAndroidLog() {
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
    }
    
    /**
     * Provides access to the test dispatcher for advanced use cases
     */
    protected val testDispatcher: TestDispatcher
        get() = StandardTestDispatcher()
}

