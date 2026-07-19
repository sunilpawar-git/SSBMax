package com.ssbmax.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ssbmax.shared.db.SharedDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Exercises the SQLDelight cache against a real in-memory SQLite DB (JVM
 * driver) — closes the Phase 0 exit report's flagged gap ("SQLDelight
 * unexercised at runtime") without needing an Android emulator/iOS simulator.
 * Why this matters: a cache that only compiles but never round-trips data is
 * exactly the kind of silent migration failure the plan's regression strategy
 * warns about.
 */
class OirResultCacheTest {

    private lateinit var cache: OirResultCache

    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SharedDatabase.Schema.create(driver)
        cache = OirResultCache(SharedDatabase(driver))
    }

    @Test
    fun `cache miss returns null`() {
        assertNull(cache.get("unknown-submission"))
    }

    @Test
    fun `put then get round-trips the full result, not just percentage`() {
        val dto = OirTestResultDto(
            testId = "test-1",
            sessionId = "session-1",
            userId = "user-1",
            totalQuestions = 50,
            correctAnswers = 40,
            incorrectAnswers = 5,
            skippedQuestions = 5,
            timeTakenSeconds = 1200,
            rawScore = 40,
            percentageScore = 80f,
            categoryScores = mapOf(
                "VERBAL" to OirCategoryScoreDto(totalQuestions = 25, correctAnswers = 20, percentage = 80f)
            ),
            completedAt = 1_700_000_000_000L
        )

        cache.put("submission-1", dto)
        val cached = cache.get("submission-1")

        assertEquals(dto, cached)
    }

    @Test
    fun `insertOrReplace overwrites the previous cached value for the same id`() {
        val first = OirTestResultDto(percentageScore = 50f)
        val second = OirTestResultDto(percentageScore = 90f)

        cache.put("submission-2", first)
        cache.put("submission-2", second)

        assertEquals(90f, cache.get("submission-2")?.percentageScore)
    }
}
