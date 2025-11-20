package com.ssbmax.ui.marketplace

import com.ssbmax.testing.BaseViewModelTest
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceViewModelTest : BaseViewModelTest() {
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `init loads coaching institutes`() = runTest {
        // When
        val viewModel = MarketplaceViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.institutes.isNotEmpty())
    }
    
    @Test
    fun `loads institutes successfully`() = runTest {
        // Given
        val viewModel = MarketplaceViewModel()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
    }
}

