@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.ssbmax.shared.presentation.analytics

import com.ssbmax.shared.domain.model.PerformanceOverview
import com.ssbmax.shared.domain.model.TestTypeStats
import com.ssbmax.shared.domain.util.NoOpLogger
import com.ssbmax.shared.presentation.testing.FakeAnalyticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Characterization test, written before converting [AnalyticsViewModel] to
 * `androidx.lifecycle.ViewModel` (Phase 1). Pins the `init { loadAnalytics() }`
 * auto-load plus the "silent failure, analytics is non-critical" behaviour of
 * [AnalyticsViewModel.loadTestTypeDetails]/[AnalyticsViewModel.loadAllTestStats]/
 * [AnalyticsViewModel.loadRecentProgress].
 */
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var analyticsRepository: FakeAnalyticsRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        analyticsRepository = FakeAnalyticsRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() = AnalyticsViewModel(analyticsRepository, NoOpLogger())

    private fun difficultyStats() = com.ssbmax.shared.domain.model.DifficultyStats(
        difficulty = "MEDIUM", attempts = 0, accuracy = 0f, averageScore = 0f,
        averageTimeSeconds = 0f, isUnlocked = true, progressToNext = 0f
    )

    private fun progressionStatus() = com.ssbmax.shared.domain.model.ProgressionStatus(
        currentLevel = "MEDIUM", nextLevel = "HARD", progressPercentage = 50f,
        attemptsNeeded = 2, accuracyNeeded = 80f, canProgress = false
    )

    @Test
    fun `loads performance overview on construction`() = runTest(testDispatcher) {
        analyticsRepository.performanceOverviewFlow = flowOf(
            PerformanceOverview(
                totalTests = 5, averageScore = 72f, totalStudyTimeMinutes = 120,
                currentStreak = 3, testsByType = mapOf("TAT" to 5), recentProgress = emptyList()
            )
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(false, state.isLoading)
        assertNotNull(state.overview)
        assertEquals(5, state.overview?.totalTests)
    }

    @Test
    fun `selectTestType loads details for that test type`() = runTest(testDispatcher) {
        analyticsRepository.testTypeStatsFlow = flowOf(
            TestTypeStats(
                testType = "TAT", totalAttempts = 3, averageScore = 65f, bestScore = 80f,
                currentDifficulty = "MEDIUM", easyStats = difficultyStats(), mediumStats = difficultyStats(),
                hardStats = difficultyStats(), recentScores = listOf(60f, 70f, 80f), progressionStatus = progressionStatus()
            )
        )
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTestType("TAT")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("TAT", state.selectedTestType)
        assertEquals(3, state.selectedTestStats?.totalAttempts)
    }

    @Test
    fun `loadAllTestStats failure is swallowed as a non-critical silent failure`() = runTest(testDispatcher) {
        analyticsRepository.allTestTypeStatsFlow = kotlinx.coroutines.flow.flow { throw Exception("offline") }
        val viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadAllTestStats()
        testDispatcher.scheduler.advanceUntilIdle()

        // Non-critical failure: does not touch the top-level error field.
        assertEquals(null, viewModel.uiState.value.error)
    }
}
