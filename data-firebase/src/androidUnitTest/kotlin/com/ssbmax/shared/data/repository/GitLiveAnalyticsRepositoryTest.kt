package com.ssbmax.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ssbmax.shared.db.SharedDatabase
import com.ssbmax.shared.domain.model.UserProfile
import com.ssbmax.shared.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Minimal stand-in so getPerformanceOverview's non-Firebase logic is constructible in tests. */
private class FakeUserProfileRepository : UserProfileRepository {
    override fun getUserProfile(userId: String): Flow<Result<UserProfile?>> = flowOf(Result.success(null))
    override suspend fun saveUserProfile(profile: UserProfile): Result<Unit> = Result.success(Unit)
    override suspend fun updateUserProfile(profile: UserProfile): Result<Unit> = Result.success(Unit)
    override fun hasCompletedProfile(userId: String): Flow<Boolean> = flowOf(false)
    override suspend fun updateLoginStreak(userId: String): Result<Int> = Result.success(0)
}

/**
 * Exercises GitLiveAnalyticsRepository's SQLDelight-only surface (the
 * progression-math methods) against a real in-memory SQLite DB, seeded via
 * GitLiveDifficultyProgressionManager -- the actual writer, not hand-crafted
 * rows -- so this also proves the read/write pair agrees on schema and
 * accuracy math. getPerformanceOverview isn't covered here since it also
 * needs Firebase.auth/UserProfileRepository, which need a live/emulated
 * Firebase backend outside this JVM unit test's reach -- same category of
 * gap as GitLiveAuthRepository.currentUser noted in the 2nd Phase 2 report.
 */
class GitLiveAnalyticsRepositoryTest {

    private lateinit var database: SharedDatabase
    private lateinit var manager: GitLiveDifficultyProgressionManager
    private lateinit var repository: GitLiveAnalyticsRepository

    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SharedDatabase.Schema.create(driver)
        database = SharedDatabase(driver)
        manager = GitLiveDifficultyProgressionManager(database)
        repository = GitLiveAnalyticsRepository(database, userProfileRepository = FakeUserProfileRepository())
    }

    @Test
    fun `getTestTypeStats returns null when no attempts recorded`() = runTest {
        assertNull(repository.getTestTypeStats("OIR").first())
    }

    @Test
    fun `recordPerformance then getTestTypeStats aggregates across difficulties`() = runTest {
        manager.recordPerformance("OIR", "EASY", score = 80f, correctAnswers = 8, totalQuestions = 10, timeSeconds = 30f)
        manager.recordPerformance("OIR", "EASY", score = 90f, correctAnswers = 9, totalQuestions = 10, timeSeconds = 25f)

        val stats = repository.getTestTypeStats("OIR").first()

        assertEquals(2, stats?.totalAttempts)
        assertEquals(85f, stats?.averageScore)
        assertEquals("EASY", stats?.easyStats?.difficulty)
        assertEquals(2, stats?.easyStats?.attempts)
    }

    @Test
    fun `getDifficultyStats for EASY with no data still returns unlocked defaults`() = runTest {
        val stats = repository.getDifficultyStats("WAT", "EASY")

        assertEquals(0, stats?.attempts)
        assertTrue(stats?.isUnlocked == true)
    }

    @Test
    fun `getDifficultyStats for MEDIUM with no data returns null (locked)`() = runTest {
        assertNull(repository.getDifficultyStats("WAT", "MEDIUM"))
    }

    @Test
    fun `getRecommendedDifficulty progresses EASY to MEDIUM after 10 attempts at 80pct`() = runTest {
        repeat(10) {
            manager.recordPerformance("SRT", "EASY", score = 85f, correctAnswers = 9, totalQuestions = 10, timeSeconds = 20f)
        }

        assertEquals("MEDIUM", manager.getRecommendedDifficulty("SRT"))
    }

    @Test
    fun `accuracy stays at 80 percent on the very first session of a 10-question test`() = runTest {
        // WHY: totalAttempts counts sessions (1 here), while correctAnswers accumulates the
        // raw per-question correct count (8). The old formula divided by totalAttempts,
        // giving 8/1*100 = 800% on the very first test taken. accuracy must use the
        // cumulative totalQuestionsAttempted (10) as its denominator instead: 8/10*100 = 80%.
        manager.recordPerformance("OIR", "EASY", score = 80f, correctAnswers = 8, totalQuestions = 10, timeSeconds = 30f)

        val stats = repository.getDifficultyStats("OIR", "EASY")

        assertEquals(80f, stats?.accuracy)
    }

    @Test
    fun `accuracy stays at 80 percent across two sessions of a 10-question test`() = runTest {
        // WHY: two sessions of 8/10 correct raw-accumulate to 16 correct out of 20 questions
        // attempted (not 2 sessions). Old formula: 16/2*100 = 800%. Fixed: 16/20*100 = 80%.
        manager.recordPerformance("OIR", "EASY", score = 80f, correctAnswers = 8, totalQuestions = 10, timeSeconds = 30f)
        manager.recordPerformance("OIR", "EASY", score = 80f, correctAnswers = 8, totalQuestions = 10, timeSeconds = 30f)

        val stats = repository.getDifficultyStats("OIR", "EASY")

        assertEquals(80f, stats?.accuracy)
        assertTrue((stats?.accuracy ?: 0f) <= 100f)
    }

    @Test
    fun `getRecommendedDifficulty stays EASY below the accuracy threshold`() = runTest {
        repeat(10) {
            manager.recordPerformance("SRT", "EASY", score = 50f, correctAnswers = 0, totalQuestions = 10, timeSeconds = 20f)
        }

        assertEquals("EASY", manager.getRecommendedDifficulty("SRT"))
    }

    @Test
    fun `getProgressionStatus reports remaining attempts and accuracy needed`() = runTest {
        manager.recordPerformance("PPDT", "EASY", score = 70f, correctAnswers = 0, totalQuestions = 10, timeSeconds = 15f)

        val status = repository.getProgressionStatus("PPDT")

        assertEquals("EASY", status?.currentLevel)
        assertEquals("MEDIUM", status?.nextLevel)
        assertEquals(9, status?.attemptsNeeded)
        assertEquals(80f, status?.accuracyNeeded)
        assertTrue(status?.canProgress == false)
    }

    @Test
    fun `resetPerformance clears all difficulties for a test type`() = runTest {
        manager.recordPerformance("GTO", "EASY", score = 80f, correctAnswers = 8, totalQuestions = 10, timeSeconds = 20f)

        manager.resetPerformance("GTO")

        assertNull(repository.getTestTypeStats("GTO").first())
    }
}
