package com.ssbmax.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ssbmax.shared.db.SharedDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises GitLiveWATWordCacheManager's SQLDelight-only surface (least-used
 * word selection/usage-tracking/cache-status math) against a real in-memory
 * SQLite DB, seeded directly via the generated queries rather than through
 * `downloadBatch` -- `downloadBatch`/`initialSync`'s Firestore read needs a
 * live/emulated Firebase backend outside this JVM unit test's reach, same
 * category of gap as `GitLiveGTOTaskCacheManagerTest`'s excluded Firestore
 * path. Seeding at or above MIN_CACHE_SIZE (20) for the tests below is
 * deliberate: it keeps `getWordsForTest` from ever calling `initialSync`
 * internally.
 */
class GitLiveWATWordCacheManagerTest {

    private lateinit var database: SharedDatabase
    private lateinit var manager: GitLiveWATWordCacheManager

    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SharedDatabase.Schema.create(driver)
        database = SharedDatabase(driver)
        manager = GitLiveWATWordCacheManager(database)
    }

    private fun seedWord(
        id: String,
        category: String? = "leadership",
        usageCount: Long = 0L,
        lastUsed: Long? = null,
        sequenceNumber: Long = 1L
    ) {
        database.sharedDatabaseQueries.insertOrReplaceWATWord(
            id = id,
            word = "word-$id",
            sequenceNumber = sequenceNumber,
            timeAllowedSeconds = 15L,
            category = category,
            difficulty = "medium",
            batchId = "batch_001",
            cachedAt = 0L,
            lastUsed = lastUsed,
            usageCount = usageCount
        )
    }

    private fun seedMinCache() {
        repeat(20) { i -> seedWord(id = "w$i", sequenceNumber = i.toLong()) }
    }

    @Test
    fun `getWordsForTest picks least-used words and marks them used, never the heaviest`() = runTest {
        // Why this matters: the whole point of usage-count-ordered selection is to rotate
        // candidates through less-seen words rather than repeating the same one -- a naive
        // unordered pick would defeat that policy silently.
        seedMinCache()
        seedWord(id = "heavy", usageCount = 999L)

        val result = manager.getWordsForTest(count = 5)

        assertTrue(result.isSuccess)
        val picked = result.getOrThrow()
        assertEquals(5, picked.size)
        assertFalse(picked.any { it.id == "heavy" })

        val row = database.sharedDatabaseQueries
            .selectLeastUsedWATWords(30)
            .executeAsList()
            .first { it.id == picked.first().id }
        assertEquals(1L, row.usageCount) // markWordsUsed incremented usage on the picked row
        assertNotNull(row.lastUsed)
    }

    @Test
    fun `getWordsForTest fails honestly when the cache is entirely empty`() = runTest {
        // Why this matters: an empty cache must surface as a Result.failure so callers can
        // trigger a real resync, not silently return an empty list that looks like "no words
        // needed" instead of "cache is broken."
        val result = manager.getWordsForTest(count = 5)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getCacheStatus counts words per known category and reports the latest sync time`() = runTest {
        seedWord(id = "l1", category = "leadership")
        seedWord(id = "m1", category = "moral_values")
        seedWord(id = "v1", category = "military_virtues")
        seedWord(id = "p1", category = "personality")
        database.sharedDatabaseQueries.insertOrReplaceWATBatchMetadata(
            batchId = "batch_001",
            downloadedAt = 777L,
            wordCount = 4,
            version = "1.0.0"
        )

        val status = manager.getCacheStatus()

        assertEquals(4, status.cachedWords)
        assertEquals(1, status.batchesDownloaded)
        assertEquals(777L, status.lastSyncTime)
        assertEquals(1, status.leadershipCount)
        assertEquals(1, status.moralValuesCount)
        assertEquals(1, status.militaryVirtuesCount)
        assertEquals(1, status.personalityCount)
    }

    @Test
    fun `clearCache empties the word table`() = runTest {
        seedMinCache()

        manager.clearCache()

        assertEquals(0L, database.sharedDatabaseQueries.selectTotalWATWordCount().executeAsOne())
    }

    @Test
    fun `getWordsForTest never returns duplicate ids across the requested count`() = runTest {
        // Why this matters: a bad LIMIT/ORDER combination could plausibly return the same
        // row twice under RANDOM()-adjacent orderings; usage-count-first ordering with a
        // stable id set must not do that.
        seedMinCache()

        val result = manager.getWordsForTest(count = 10)

        assertTrue(result.isSuccess)
        val ids = result.getOrThrow().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertNotEquals(0, ids.size)
    }
}
