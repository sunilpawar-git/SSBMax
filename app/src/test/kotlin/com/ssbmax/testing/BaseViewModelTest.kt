package com.ssbmax.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Rule

/**
 * Base class for ViewModel tests
 * Provides coroutine test dispatcher setup
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
     * Provides access to the test dispatcher for advanced use cases
     */
    protected val testDispatcher: TestDispatcher
        get() = StandardTestDispatcher()
}

