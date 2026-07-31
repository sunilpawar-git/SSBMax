@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.marketplace

import com.ssbmax.shared.domain.util.NoOpLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Characterization test for [MarketplaceViewModel], written retroactively
 * (13-VM gap-closing pass, see the KMP-convergence plan's Phase 1). Pins the
 * current mock-data-backed search/filter behaviour (no `MarketplaceRepository`
 * exists yet, see the ViewModel's own doc comment).
 */
class MarketplaceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = MarketplaceViewModel(logger = NoOpLogger())

    @Test
    fun `loads all mock institutes on init`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(10, state.institutes.size)
        assertEquals(null, state.error)
    }

    @Test
    fun `search query filters institutes by name`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("Kalshi")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Kalshi", state.searchQuery)
        assertEquals(1, state.institutes.size)
        assertEquals("major_kalshi", state.institutes.first().id)
    }

    @Test
    fun `city filter narrows the institute list`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onFilterCityChange("Chandigarh")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.institutes.isNotEmpty())
        assertTrue(state.institutes.all { it.city.equals("Chandigarh", ignoreCase = true) })
    }

    @Test
    fun `clearFilters resets search and filters and reloads all institutes`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onSearchQueryChange("Kalshi")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearFilters()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(null, state.filterType)
        assertEquals(null, state.filterCity)
        assertEquals(10, state.institutes.size)
    }

    @Test
    fun `a search query matching nothing yields an empty result without an error`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("no-such-institute-xyz")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.institutes.isEmpty())
        assertEquals(null, state.error)
    }
}
