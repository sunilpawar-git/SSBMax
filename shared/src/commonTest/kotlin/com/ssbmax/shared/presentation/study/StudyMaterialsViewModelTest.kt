@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.study

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

/**
 * Characterization test for [StudyMaterialsViewModel], written retroactively
 * (13-VM gap-closing pass, see the KMP-convergence plan's Phase 1). Pins the
 * current hardcoded 9-category list + total-article-count computation (no
 * Firestore-based dynamic content yet, see the ViewModel's own doc comment).
 */
class StudyMaterialsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = StudyMaterialsViewModel(logger = NoOpLogger())

    @Test
    fun `loads the 9 hardcoded study categories with the total article count`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
        assertEquals(9, state.categories.size)
        assertEquals(360, state.totalArticles)
    }

    @Test
    fun `OIR prep category is present and not premium`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val oirCategory = viewModel.uiState.value.categories.first { it.type == StudyCategory.OIR_PREP }
        assertEquals(24, oirCategory.articleCount)
        assertEquals(false, oirCategory.isPremium)
    }
}
