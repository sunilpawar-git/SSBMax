package com.ssbmax.core.data.repository

import app.cash.turbine.test
import com.ssbmax.core.data.local.dao.UserPerformanceDao
import com.ssbmax.core.data.local.entity.UserPerformanceEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AnalyticsRepositoryImpl
 *
 * Tests performance analytics aggregation, difficulty stats, and progression status calculations.
 */
class AnalyticsRepositoryImplTest {

    private lateinit var performanceDao: UserPerformanceDao
    private lateinit var repository: AnalyticsRepositoryImpl

    @Before
    fun setup() {
        performanceDao = mockk()
        repository = AnalyticsRepositoryImpl(performanceDao)
    }

    @Test
    fun `getPerformanceOverview returns null when no data`() = runTest {
        // Given: No performance data
        every { performanceDao.getAllPerformance() } returns flowOf(emptyList())

        // When
        repository.getPerformanceOverview().test {
            val overview = awaitItem()

            // Then
            assertNull(overview)
            awaitComplete()
        }
    }

    @Test
    fun `getPerformanceOverview calculates correct aggregates`() = runTest {
        // Given: Performance data for multiple tests
        val performances = listOf(
            createPerformance("WAT", "EASY", attempts = 5, avgScore = 80f, avgTime = 300f),
            createPerformance("WAT", "MEDIUM", attempts = 3, avgScore = 70f, avgTime = 400f),
            createPerformance("SRT", "EASY", attempts = 2, avgScore = 90f, avgTime = 200f)
        )
        every { performanceDao.getAllPerformance() } returns flowOf(performances)

        // When
        repository.getPerformanceOverview().test {
            val overview = awaitItem()

            // Then
            assertNotNull(overview)
            assertEquals(10, overview!!.totalTests) // 5 + 3 + 2
            // Weighted average: (5*80 + 3*70 + 2*90) / 10 = 79.0
            assertEquals(79.0f, overview.averageScore, 0.01f)
            assertEquals(2, overview.testsByType.size)
            assertEquals(8, overview.testsByType["WAT"]) // 5 + 3
            assertEquals(2, overview.testsByType["SRT"])
            assertFalse(overview.recentProgress.isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `getTestTypeStats returns null for unknown test type`() = runTest {
        // Given
        every { performanceDao.getPerformanceByTestType("UNKNOWN") } returns flowOf(emptyList())

        // When
        repository.getTestTypeStats("UNKNOWN").test {
            val stats = awaitItem()

            // Then
            assertNull(stats)
            awaitComplete()
        }
    }

    @Test
    fun `getTestTypeStats calculates correct stats for test type`() = runTest {
        // Given: Performance data for WAT test
        val performances = listOf(
            createPerformance("WAT", "EASY", attempts = 15, avgScore = 85f, avgTime = 300f, correctAnswers = 13), // 13/15 ≈ 87%
            createPerformance("WAT", "MEDIUM", attempts = 8, avgScore = 75f, avgTime = 400f, correctAnswers = 6), // 6/8 = 75%
            createPerformance("WAT", "HARD", attempts = 3, avgScore = 60f, avgTime = 500f, correctAnswers = 2) // 2/3 ≈ 67%
        )
        every { performanceDao.getPerformanceByTestType("WAT") } returns flowOf(performances)

        // When
        repository.getTestTypeStats("WAT").test {
            val stats = awaitItem()

            // Then
            assertNotNull(stats)
            assertEquals("WAT", stats!!.testType)
            assertEquals(26, stats.totalAttempts) // 15 + 8 + 3
            // Weighted average: (15*85 + 8*75 + 3*60) / 26 = 77.88
            assertEquals(77.88f, stats.averageScore, 0.01f)
            assertEquals(85f, stats.bestScore)
            assertNotNull(stats.easyStats)
            assertNotNull(stats.mediumStats)
            assertNotNull(stats.hardStats)
            awaitComplete()
        }
    }

    @Test
    fun `getAllTestTypeStats returns stats for all test types`() = runTest {
        // Given: Multiple test types
        val performances = listOf(
            createPerformance("WAT", "EASY", attempts = 5, avgScore = 80f),
            createPerformance("SRT", "EASY", attempts = 3, avgScore = 70f),
            createPerformance("TAT", "MEDIUM", attempts = 2, avgScore = 90f)
        )
        every { performanceDao.getAllPerformance() } returns flowOf(performances)

        // When
        repository.getAllTestTypeStats().test {
            val statsList = awaitItem()

            // Then
            assertEquals(3, statsList.size)
            val testTypes = statsList.map { it.testType }
            assertTrue(testTypes.contains("WAT"))
            assertTrue(testTypes.contains("SRT"))
            assertTrue(testTypes.contains("TAT"))
            awaitComplete()
        }
    }

    @Test
    fun `getRecentProgress returns limited results`() = runTest {
        // Given: 15 performance records
        val performances = (1..15).map { index ->
            createPerformance(
                "TEST$index",
                "EASY",
                attempts = 1,
                avgScore = 80f,
                lastAttempt = System.currentTimeMillis() - (index * 1000L)
            )
        }
        every { performanceDao.getAllPerformance() } returns flowOf(performances)

        // When: Request only 5 most recent
        repository.getRecentProgress(5).test {
            val progress = awaitItem()

            // Then
            assertEquals(5, progress.size)
            // Should be in descending order (most recent first)
            assertEquals("TEST1", progress[0].testType)
            awaitComplete()
        }
    }

    @Test
    fun `getDifficultyStats returns null for non-existent difficulty`() = runTest {
        // Given
        coEvery { performanceDao.getPerformance("WAT", "HARD") } returns null

        // When
        val stats = repository.getDifficultyStats("WAT", "HARD")

        // Then
        assertNull(stats)
    }

    @Test
    fun `getDifficultyStats returns unlocked easy level with zero progress`() = runTest {
        // Given: No performance data
        coEvery { performanceDao.getPerformance("WAT", "EASY") } returns null

        // When
        val stats = repository.getDifficultyStats("WAT", "EASY")

        // Then
        assertNotNull(stats)
        assertEquals("EASY", stats!!.difficulty)
        assertEquals(0, stats.attempts)
        assertEquals(0f, stats.accuracy)
        assertTrue(stats.isUnlocked) // EASY is always unlocked
        assertEquals(0f, stats.progressToNext, 0.01f)
    }

    @Test
    fun `getDifficultyStats calculates progress to next level`() = runTest {
        // Given: EASY level with partial progress
        val easyPerf = createPerformance("WAT", "EASY", attempts = 5, avgScore = 80f, correctAnswers = 3) // 3/5 = 60%
        coEvery { performanceDao.getPerformance("WAT", "EASY") } returns easyPerf
        coEvery { performanceDao.getPerformance("WAT", "MEDIUM") } returns null

        // When
        val stats = repository.getDifficultyStats("WAT", "EASY")

        // Then
        assertNotNull(stats)
        assertEquals(5, stats!!.attempts)
        assertEquals(60f, stats.accuracy)
        assertTrue(stats.isUnlocked)
        // Progress = ((60/80 * 100) + (5/10 * 100)) / 2 = (75 + 50) / 2 = 62.5
        assertTrue(stats.progressToNext > 0f)
        assertTrue(stats.progressToNext < 100f)
    }

    @Test
    fun `getProgressionStatus returns EASY for new user`() = runTest {
        // Given: No performance data
        coEvery { performanceDao.getPerformance("WAT", "EASY") } returns null
        coEvery { performanceDao.getPerformance("WAT", "MEDIUM") } returns null
        coEvery { performanceDao.getPerformance("WAT", "HARD") } returns null

        // When
        val status = repository.getProgressionStatus("WAT")

        // Then
        assertNotNull(status)
        assertEquals("EASY", status!!.currentLevel)
        assertEquals("MEDIUM", status.nextLevel)
        assertEquals(0f, status.progressPercentage, 0.01f)
        assertEquals(10, status.attemptsNeeded)
        assertEquals(80f, status.accuracyNeeded, 0.01f)
        assertFalse(status.canProgress)
    }

    @Test
    fun `getProgressionStatus returns HARD with no next level`() = runTest {
        // Given: User at HARD level
        val hardPerf = createPerformance("WAT", "HARD", attempts = 15, avgScore = 90f, correctAnswers = 14) // 14/15 ≈ 93%
        coEvery { performanceDao.getPerformance("WAT", "EASY") } returns null
        coEvery { performanceDao.getPerformance("WAT", "MEDIUM") } returns null
        coEvery { performanceDao.getPerformance("WAT", "HARD") } returns hardPerf

        // When
        val status = repository.getProgressionStatus("WAT")

        // Then
        assertNotNull(status)
        assertEquals("HARD", status!!.currentLevel)
        assertNull(status.nextLevel)
        assertEquals(100f, status.progressPercentage, 0.01f)
        assertEquals(0, status.attemptsNeeded)
        assertFalse(status.canProgress)
    }

    @Test
    fun `getProgressionStatus allows progression when criteria met`() = runTest {
        // Given: EASY level with sufficient attempts and accuracy
        val easyPerf = createPerformance("WAT", "EASY", attempts = 12, avgScore = 85f, correctAnswers = 10) // 10/12 ≈ 83%
        coEvery { performanceDao.getPerformance("WAT", "EASY") } returns easyPerf
        coEvery { performanceDao.getPerformance("WAT", "MEDIUM") } returns null
        coEvery { performanceDao.getPerformance("WAT", "HARD") } returns null

        // When
        val status = repository.getProgressionStatus("WAT")

        // Then
        assertNotNull(status)
        assertEquals("EASY", status!!.currentLevel)
        assertEquals("MEDIUM", status.nextLevel)
        assertEquals(0, status.attemptsNeeded) // 12 >= 10
        assertEquals(0f, status.accuracyNeeded, 0.01f) // 85 >= 80
        assertTrue(status.canProgress)
    }

    @Test
    fun `getStudyPattern returns null (not implemented)`() = runTest {
        // When
        val pattern = repository.getStudyPattern()

        // Then
        assertNull(pattern)
    }

    @Test
    fun `getAchievements returns empty list (not implemented)`() = runTest {
        // When
        repository.getAchievements().test {
            val achievements = awaitItem()

            // Then
            assertTrue(achievements.isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `getGoalProgress returns empty list (not implemented)`() = runTest {
        // When
        repository.getGoalProgress().test {
            val goals = awaitItem()

            // Then
            assertTrue(goals.isEmpty())
            awaitComplete()
        }
    }

    // Helper function to create test data
    private fun createPerformance(
        testType: String,
        difficulty: String,
        attempts: Int = 1,
        avgScore: Float = 80f,
        avgTime: Float = 300f,
        correctAnswers: Int = 8, // Default: 8 out of 10 attempts = 80% accuracy
        lastAttempt: Long = System.currentTimeMillis()
    ): UserPerformanceEntity {
        return UserPerformanceEntity(
            testType = testType,
            difficulty = difficulty,
            totalAttempts = attempts,
            correctAnswers = correctAnswers,
            incorrectAnswers = attempts - correctAnswers,
            averageScore = avgScore,
            averageTimeSeconds = avgTime,
            currentLevel = difficulty,
            lastAttemptAt = lastAttempt
        )
    }
}
