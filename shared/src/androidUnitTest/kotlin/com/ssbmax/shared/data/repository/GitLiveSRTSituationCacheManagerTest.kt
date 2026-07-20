package com.ssbmax.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ssbmax.shared.db.SharedDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises GitLiveSRTSituationCacheManager's SQLDelight-only surface against a real
 * in-memory SQLite DB. `downloadBatch`/`initialSync`'s Firestore read needs a live/
 * emulated backend outside this JVM unit test's reach, same category of gap as the
 * GTO/WAT manager tests. Seeding at or above MIN_CACHE_SIZE (20) keeps
 * `getSituationsForTest` from ever calling `initialSync` internally.
 */
class GitLiveSRTSituationCacheManagerTest {

    private lateinit var database: SharedDatabase
    private lateinit var manager: GitLiveSRTSituationCacheManager

    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SharedDatabase.Schema.create(driver)
        database = SharedDatabase(driver)
        manager = GitLiveSRTSituationCacheManager(database)
    }

    private fun seedSituation(id: String, category: String = "GENERAL", usageCount: Long = 0L, seq: Long = 1L) {
        database.sharedDatabaseQueries.insertOrReplaceSRTSituation(
            id = id,
            situation = "Situation $id",
            sequenceNumber = seq,
            category = category,
            timeAllowedSeconds = 30L,
            difficulty = "medium",
            batchId = "batch_001",
            cachedAt = 0L,
            lastUsed = null,
            usageCount = usageCount
        )
    }

    private fun seedMinCache() {
        repeat(20) { i -> seedSituation(id = "s$i", seq = i.toLong()) }
    }

    @Test
    fun `getSituationsForTest returns a balanced spread across categories when the pool is large enough`() = runTest {
        // Why this matters: SRT deliberately mixes categories (leadership, crisis management,
        // ethics, etc.) so a candidate never gets a lopsided test -- a naive least-used pick
        // without category balancing could return 60 situations from a single category.
        val categories = listOf(
            "LEADERSHIP", "DECISION_MAKING", "CRISIS_MANAGEMENT", "ETHICAL_DILEMMA",
            "RESPONSIBILITY", "TEAMWORK", "INTERPERSONAL", "COURAGE", "GENERAL"
        )
        categories.forEach { cat -> repeat(5) { i -> seedSituation(id = "${cat}_$i", category = cat, seq = i.toLong()) } }

        val result = manager.getSituationsForTest(count = 45)

        assertTrue(result.isSuccess)
        val situations = result.getOrThrow()
        assertEquals(45, situations.size)
        val distinctCategories = situations.map { it.category }.toSet()
        assertTrue("Expected multiple categories represented, got $distinctCategories", distinctCategories.size > 1)
    }

    @Test
    fun `getSituationsForTest never returns duplicate ids`() = runTest {
        seedMinCache()

        val result = manager.getSituationsForTest(count = 20)

        assertTrue(result.isSuccess)
        val ids = result.getOrThrow().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `getSituationsForTest marks selected situations as used`() = runTest {
        seedMinCache()

        val result = manager.getSituationsForTest(count = 20)

        assertTrue(result.isSuccess)
        val pickedId = result.getOrThrow().first().id
        val row = database.sharedDatabaseQueries.selectLeastUsedSRTSituations(30).executeAsList()
            .first { it.id == pickedId }
        assertEquals(1L, row.usageCount)
    }

    @Test
    fun `getSituationsForTest fails honestly when the cache is entirely empty`() = runTest {
        val result = manager.getSituationsForTest(count = 20)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getCacheStatus counts situations per known category`() = runTest {
        seedSituation(id = "l1", category = "LEADERSHIP")
        seedSituation(id = "d1", category = "DECISION_MAKING")
        database.sharedDatabaseQueries.insertOrReplaceSRTBatchMetadata(
            batchId = "batch_001", downloadedAt = 123L, situationCount = 2, version = "1.0.0"
        )

        val status = manager.getCacheStatus()

        assertEquals(2, status.cachedSituations)
        assertEquals(1, status.batchesDownloaded)
        assertEquals(123L, status.lastSyncTime)
        assertEquals(1, status.leadershipCount)
        assertEquals(1, status.decisionMakingCount)
    }

    @Test
    fun `clearCache empties the situation table`() = runTest {
        seedMinCache()

        manager.clearCache()

        assertEquals(0L, database.sharedDatabaseQueries.selectTotalSRTSituationCount().executeAsOne())
    }
}
