package com.ssbmax.ui.upgrade

import com.ssbmax.testing.BaseViewModelTest
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class UpgradeViewModelTest : BaseViewModelTest() {
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `init loads subscription plans`() = runTest {
        // When
        val viewModel = UpgradeViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state.plans.isNotEmpty())
    }
    
    @Test
    fun `has PRO and PREMIUM plans`() = runTest {
        // When
        val viewModel = UpgradeViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state.plans.any { it.name.contains("Pro", ignoreCase = true) })
        assertTrue(state.plans.any { it.name.contains("Premium", ignoreCase = true) })
    }
}

