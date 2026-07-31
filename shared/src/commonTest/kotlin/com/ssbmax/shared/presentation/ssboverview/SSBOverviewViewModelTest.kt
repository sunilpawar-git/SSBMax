@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.ssboverview

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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Characterization test for [SSBOverviewViewModel], written retroactively
 * (13-VM gap-closing pass, see the KMP-convergence plan's Phase 1). Pins the
 * static-content load + card-expansion-toggle behaviour (content is
 * educational/static, loaded from [com.ssbmax.shared.domain.util.SSBContentProvider],
 * see the ViewModel's own doc comment for why no repository is injected).
 */
class SSBOverviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = SSBOverviewViewModel(logger = NoOpLogger())

    @Test
    fun `loads SSB info cards on init`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
        assertTrue(state.infoCards.isNotEmpty())
    }

    @Test
    fun `toggleCardExpansion expands a collapsed card`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val cardId = viewModel.uiState.value.infoCards.first().id

        viewModel.toggleCardExpansion(cardId)

        assertTrue(viewModel.uiState.value.expandedCardIds.contains(cardId))
    }

    @Test
    fun `toggleCardExpansion collapses an already-expanded card`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        val cardId = viewModel.uiState.value.infoCards.first().id
        viewModel.toggleCardExpansion(cardId)

        viewModel.toggleCardExpansion(cardId)

        assertFalse(viewModel.uiState.value.expandedCardIds.contains(cardId))
    }

    @Test
    fun `refresh reloads the info cards`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertTrue(state.infoCards.isNotEmpty())
    }
}
