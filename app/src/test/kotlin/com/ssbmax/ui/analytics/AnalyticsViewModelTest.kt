package com.ssbmax.ui.analytics

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AnalyticsRepository
import com.ssbmax.testing.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive tests for AnalyticsViewModel
 * Tests analytics loading, test type filtering, error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest : BaseViewModelTest() {
    
    private lateinit var viewModel: AnalyticsViewModel
    private lateinit var mockAnalyticsRepository: AnalyticsRepository
    
    private val testOverview = PerformanceOverview(
        totalTests = 10,
        averageScore = 75.5f,
        totalStudyTimeMinutes = 120,
        currentStreak = 5,
        testsByType = mapOf(
            "OIR" to 3,
            "PPDT" to 2,
            "TAT" to 5
        ),
        recentProgress = listOf(
            TestPerformancePoint(
                testType = "OIR",
                difficulty = "MEDIUM",
                score = 80f,
                timestamp = System.currentTimeMillis(),
                date = "2024-01-15"
            )
        )
    )
    
    private val testTypeStats = TestTypeStats(
        testType = "OIR",
        totalAttempts = 5,
        averageScore = 78.5f,
        bestScore = 92.0f,
        currentDifficulty = "MEDIUM",
        easyStats = DifficultyStats(
            difficulty = "EASY",
            attempts = 2,
            accuracy = 85f,
            averageScore = 85f,
            averageTimeSeconds = 120f,
            isUnlocked = true,
            progressToNext = 100f
        ),
        mediumStats = DifficultyStats(
            difficulty = "MEDIUM",
            attempts = 3,
            accuracy = 75f,
            averageScore = 75f,
            averageTimeSeconds = 150f,
            isUnlocked = true,
            progressToNext = 60f
        ),
        hardStats = DifficultyStats(
            difficulty = "HARD",
            attempts = 0,
            accuracy = 0f,
            averageScore = 0f,
            averageTimeSeconds = 0f,
            isUnlocked = false,
            progressToNext = 0f
        ),
        recentScores = listOf(80f, 75f, 82f, 70f, 78f),
        progressionStatus = ProgressionStatus(
            currentLevel = "MEDIUM",
            nextLevel = "HARD",
            progressPercentage = 60f,
            attemptsNeeded = 2,
            accuracyNeeded = 80f,
            canProgress = false
        )
    )
    
    private val recentProgressList = listOf(
        TestPerformancePoint("OIR", "EASY", 80f, System.currentTimeMillis(), "2024-01-15"),
        TestPerformancePoint("PPDT", "MEDIUM", 75f, System.currentTimeMillis(), "2024-01-14"),
        TestPerformancePoint("TAT", "HARD", 85f, System.currentTimeMillis(), "2024-01-13")
    )
    
    @Before
    fun setup() {
        mockAnalyticsRepository = mockk(relaxed = true)
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== Load Performance Overview Tests ====================
    
    @Test
    fun `init loads performance overview successfully`() = runTest {
        // Given
        coEvery { mockAnalyticsRepository.getPerformanceOverview() } returns 
            MutableStateFlow(testOverview)
        
        // When
        viewModel = AnalyticsViewModel(mockAnalyticsRepository)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should have no error", state.error)
        assertNotNull("Should have overview", state.overview)
        assertEquals("Should have correct total tests", 10, state.overview?.totalTests)
        assertEquals("Should have correct average score", 75.5f, state.overview?.averageScore)
        assertEquals("Should have correct study time", 120, state.overview?.totalStudyTimeMinutes)
        assertEquals("Should have correct streak", 5, state.overview?.currentStreak)
    }
    
    @Test
    fun `loadAnalytics updates state with new data`() = runTest {
        // Given
        val updatedOverview = testOverview.copy(totalTests = 15, averageScore = 80f)
        val flowState = MutableStateFlow(testOverview)
        coEvery { mockAnalyticsRepository.getPerformanceOverview() } returns flowState
        
        viewModel = AnalyticsViewModel(mockAnalyticsRepository)
        advanceUntilIdle()
        
        // When - simulate data update
        flowState.value = updatedOverview
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have updated total tests", 15, state.overview?.totalTests)
        assertEquals("Should have updated average score", 80f, state.overview?.averageScore)
    }
    
    // ==================== Load Test Type Details Tests ====================
    
    @Test
    fun `loadTestTypeDetails loads specific test stats`() = runTest {
        // Given
        coEvery { mockAnalyticsRepository.getPerformanceOverview() } returns 
            MutableStateFlow(testOverview)
        coEvery { mockAnalyticsRepository.getTestTypeStats("OIR") } returns 
            MutableStateFlow(testTypeStats)
        
        viewModel = AnalyticsViewModel(mockAnalyticsRepository)
        advanceUntilIdle()
        
        // When
        viewModel.loadTestTypeDetails("OIR")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertNotNull("Should have selected test stats", state.selectedTestStats)
        assertEquals("Should be OIR stats", "OIR", state.selectedTestStats?.testType)
        assertEquals("Should have 5 total attempts", 5, state.selectedTestStats?.totalAttempts)
        assertEquals("Should have average score", 78.5f, state.selectedTestStats?.averageScore)
        assertEquals("Should have best score", 92.0f, state.selectedTestStats?.bestScore)
    }
    
    @Test
    fun `selectTestType updates state and loads details`() = runTest {
        // Given
        coEvery { mockAnalyticsRepository.getPerformanceOverview() } returns 
            MutableStateFlow(testOverview)
        coEvery { mockAnalyticsRepository.getTestTypeStats("PPDT") } returns 
            MutableStateFlow(testTypeStats.copy(testType = "PPDT"))
        
        viewModel = AnalyticsViewModel(mockAnalyticsRepository)
        advanceUntilIdle()
        
        // When
        viewModel.selectTestType("PPDT")
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have selected test type", "PPDT", state.selectedTestType)
        assertNotNull("Should have loaded stats for PPDT", state.selectedTestStats)
        assertEquals("Stats should be for PPDT", "PPDT", state.selectedTestStats?.testType)
    }
    
    @Test
    fun `selectTestType with null clears selection`() = runTest {
        // Given
        coEvery { mockAnalyticsRepository.getPerformanceOverview() } returns 
            MutableStateFlow(testOverview)
        coEvery { mockAnalyticsRepository.getTestTypeStats("OIR") } returns 
            MutableStateFlow(testTypeStats)
        
        viewModel = AnalyticsViewModel(mockAnalyticsRepository)
        advanceUntilIdle()
        
        viewModel.selectTestType("OIR")
        advanceUntilIdle()
        
        // When
        viewModel.selectTestType(null)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertNull("Should have cleared selection", state.selectedTestType)
    }
    
    // ==================== Load All Test Stats Tests ====================
    
    @Test
    fun `loadAllTestStats loads all test type statistics`() = runTest {
        // Given
        val allStats = listOf(
            testTypeStats,
            testTypeStats.copy(testType = "PPDT"),
            testTypeStats.copy(testType = "TAT")
        )
        coEvery { mockAnalyticsRepository.getPerformanceOverview() } returns 
            MutableStateFlow(testOverview)
        coEvery { mockAnalyticsRepository.getAllTestTypeStats() } returns 
            MutableStateFlow(allStats)
        
        viewModel = AnalyticsViewModel(mockAnalyticsRepository)
        advanceUntilIdle()
        
        // When
        viewModel.loadAllTestStats()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 3 test stats", 3, state.allTestStats.size)
        assertTrue("Should contain OIR stats", state.allTestStats.any { it.testType == "OIR" })
        assertTrue("Should contain PPDT stats", state.allTestStats.any { it.testType == "PPDT" })
        assertTrue("Should contain TAT stats", state.allTestStats.any { it.testType == "TAT" })
    }
    
    // ==================== Load Recent Progress Tests ====================
    
    @Test
    fun `loadRecentProgress loads recent test history`() = runTest {
        // Given
        coEvery { mockAnalyticsRepository.getPerformanceOverview() } returns 
            MutableStateFlow(testOverview)
        coEvery { mockAnalyticsRepository.getRecentProgress(10) } returns 
            MutableStateFlow(recentProgressList)
        
        viewModel = AnalyticsViewModel(mockAnalyticsRepository)
        advanceUntilIdle()
        
        // When
        viewModel.loadRecentProgress(10)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 3 recent progress points", 3, state.recentProgress.size)
        assertEquals("First should be OIR", "OIR", state.recentProgress[0].testType)
        assertEquals("Second should be PPDT", "PPDT", state.recentProgress[1].testType)
        assertEquals("Third should be TAT", "TAT", state.recentProgress[2].testType)
    }
    
    @Test
    fun `loadRecentProgress with custom limit`() = runTest {
        // Given
        coEvery { mockAnalyticsRepository.getPerformanceOverview() } returns 
            MutableStateFlow(testOverview)
        coEvery { mockAnalyticsRepository.getRecentProgress(5) } returns 
            MutableStateFlow(recentProgressList.take(2))
        
        viewModel = AnalyticsViewModel(mockAnalyticsRepository)
        advanceUntilIdle()
        
        // When
        viewModel.loadRecentProgress(5)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should have 2 recent progress points", 2, state.recentProgress.size)
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    fun `loadAnalytics handles repository error gracefully`() = runTest {
        // Given
        coEvery { mockAnalyticsRepository.getPerformanceOverview() } throws 
            RuntimeException("Network error")
        
        // When
        viewModel = AnalyticsViewModel(mockAnalyticsRepository)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        assertNotNull("Should have error", state.error)
        assertTrue(
            "Error should mention failure",
            state.error?.contains("Failed to load analytics") == true
        )
    }
    
    @Test
    fun `loadTestTypeDetails handles error without crashing`() = runTest {
        // Given
        coEvery { mockAnalyticsRepository.getPerformanceOverview() } returns 
            MutableStateFlow(testOverview)
        coEvery { mockAnalyticsRepository.getTestTypeStats(any()) } throws 
            Exception("Stats not found")
        
        viewModel = AnalyticsViewModel(mockAnalyticsRepository)
        advanceUntilIdle()
        
        // When - should not throw
        viewModel.loadTestTypeDetails("UNKNOWN")
        advanceUntilIdle()
        
        // Then - error is logged but state remains valid
        val state = viewModel.uiState.value
        assertFalse("Should not be loading", state.isLoading)
        // selectedTestStats should remain null or unchanged
    }
    
    @Test
    fun `loadAllTestStats handles error without crashing`() = runTest {
        // Given
        coEvery { mockAnalyticsRepository.getPerformanceOverview() } returns 
            MutableStateFlow(testOverview)
        coEvery { mockAnalyticsRepository.getAllTestTypeStats() } throws 
            Exception("Failed to load stats")
        
        viewModel = AnalyticsViewModel(mockAnalyticsRepository)
        advanceUntilIdle()
        
        // When - should not throw
        viewModel.loadAllTestStats()
        advanceUntilIdle()
        
        // Then - error is logged but app doesn't crash
        val state = viewModel.uiState.value
        assertTrue("All test stats should remain empty", state.allTestStats.isEmpty())
    }
    
    @Test
    fun `loadRecentProgress handles error without crashing`() = runTest {
        // Given
        coEvery { mockAnalyticsRepository.getPerformanceOverview() } returns 
            MutableStateFlow(testOverview)
        coEvery { mockAnalyticsRepository.getRecentProgress(any()) } throws 
            Exception("Progress load failed")
        
        viewModel = AnalyticsViewModel(mockAnalyticsRepository)
        advanceUntilIdle()
        
        // When - should not throw
        viewModel.loadRecentProgress()
        advanceUntilIdle()
        
        // Then - error is logged but app doesn't crash
        val state = viewModel.uiState.value
        assertTrue("Recent progress should remain empty", state.recentProgress.isEmpty())
    }
    
    // ==================== UI State Tests ====================
    
    @Test
    fun `ui state has correct default values`() {
        // Test the UI state data class defaults
        val defaultState = AnalyticsUiState()
        
        assertTrue("Initial state should be loading", defaultState.isLoading)
        assertNull("Should have no error", defaultState.error)
        assertNull("Should have no overview", defaultState.overview)
        assertNull("Should have no selected test type", defaultState.selectedTestType)
        assertNull("Should have no selected test stats", defaultState.selectedTestStats)
        assertTrue("Should have empty all test stats", defaultState.allTestStats.isEmpty())
        assertTrue("Should have empty recent progress", defaultState.recentProgress.isEmpty())
    }
}

