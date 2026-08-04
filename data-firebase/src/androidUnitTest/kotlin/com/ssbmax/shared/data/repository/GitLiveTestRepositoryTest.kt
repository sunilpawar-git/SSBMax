package com.ssbmax.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ssbmax.shared.db.SharedDatabase
import com.ssbmax.shared.domain.model.SSBCategory
import com.ssbmax.shared.domain.model.TestResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/**
 * Exercises the SQLDelight-backed local store against a real in-memory
 * SQLite DB, same pattern as OirResultCacheTest -- this repo has no Firestore
 * counterpart in the Android original, so the local round-trip *is* the
 * contract being ported, not just an optimization layer in front of one.
 */
class GitLiveTestRepositoryTest {

    private lateinit var repository: GitLiveTestRepository

    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SharedDatabase.Schema.create(driver)
        repository = GitLiveTestRepository(SharedDatabase(driver))
    }

    @Test
    fun `getTests returns the psychology sample tests`() = runTest {
        val tests = repository.getTests(SSBCategory.PSYCHOLOGY).first().getOrThrow()
        assertEquals(listOf("tat_001", "wat_001"), tests.map { it.id })
    }

    @Test
    fun `getTests returns empty for categories with no samples`() = runTest {
        val tests = repository.getTests(SSBCategory.GTO).first().getOrThrow()
        assertTrue(tests.isEmpty())
    }

    @Test
    fun `submitTestResult then getTestResults round-trips score and maxScore as Float`() = runTest {
        val result = TestResult(
            id = "result-1",
            testId = "tat_001",
            userId = "user-1",
            score = 82.5f,
            maxScore = 100f,
            completedAt = 1_700_000_000_000L,
            timeSpent = 12.minutes
        )

        repository.submitTestResult(result).getOrThrow()
        val stored = repository.getTestResults("user-1").first().getOrThrow()

        assertEquals(listOf(result), stored)
    }

    @Test
    fun `getTestResultsByTest filters by testId within the same user`() = runTest {
        repository.submitTestResult(
            TestResult(
                id = "r1", testId = "tat_001", userId = "user-1",
                score = 50f, completedAt = 1L, timeSpent = 5.minutes
            )
        )
        repository.submitTestResult(
            TestResult(
                id = "r2", testId = "wat_001", userId = "user-1",
                score = 60f, completedAt = 2L, timeSpent = 5.minutes
            )
        )

        val results = repository.getTestResultsByTest("user-1", "tat_001").first().getOrThrow()

        assertEquals(listOf("r1"), results.map { it.id })
    }

    @Test
    fun `startTestSession stamps a fresh sessionId and zero progress`() = runTest {
        val session = repository.startTestSession("tat_001", "user-1").getOrThrow()

        assertEquals("tat_001", session.testId)
        assertEquals("user-1", session.userId)
        assertEquals(0, session.currentQuestion)
        assertTrue(session.responses.isEmpty())
        assertTrue(session.sessionId.isNotBlank())
    }
}
