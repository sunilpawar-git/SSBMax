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
 * Exercises GitLiveGTOTaskCacheManager's SQLDelight-only surface (task
 * rotation/usage-tracking/cache-status math) against a real in-memory SQLite
 * DB, seeded directly via the generated queries rather than through
 * `downloadBatch` -- `downloadBatch`/`initialSync`'s Firestore read needs a
 * live/emulated Firebase backend outside this JVM unit test's reach, same
 * category of gap as `GitLiveAnalyticsRepositoryTest`'s excluded
 * `getPerformanceOverview`. Seeding at or above MIN_CACHE_SIZE (10) for the
 * tests below is deliberate: it keeps `getTasksByType`/`getRandomTasks` from
 * ever calling `initialSync` internally.
 */
class GitLiveGTOTaskCacheManagerTest {

    private lateinit var database: SharedDatabase
    private lateinit var manager: GitLiveGTOTaskCacheManager

    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SharedDatabase.Schema.create(driver)
        database = SharedDatabase(driver)
        manager = GitLiveGTOTaskCacheManager(database)
    }

    private fun seedTask(
        id: String,
        taskType: String = "GD",
        usageCount: Long = 0L,
        lastUsed: Long? = null
    ) {
        database.sharedDatabaseQueries.insertOrReplaceGTOTask(
            id = id,
            taskType = taskType,
            title = "Title $id",
            description = "desc",
            instructions = "inst",
            timeAllowedMinutes = 10L,
            difficultyLevel = "easy",
            category = "leadership",
            scenario = null,
            resources = null,
            objectives = null,
            evaluationCriteria = null,
            batchId = "batch_001",
            cachedAt = 0L,
            lastUsed = lastUsed,
            usageCount = usageCount
        )
    }

    private fun seedMinCache(taskType: String = "GD") {
        repeat(10) { i -> seedTask(id = "$taskType-$i", taskType = taskType) }
    }

    @Test
    fun `getTasksByType picks a least-used task and marks it used, never the heaviest`() = runTest {
        // Why this matters: the whole point of usage-count-ordered selection is to rotate
        // candidates through less-seen tasks rather than repeating the same one -- a naive
        // unordered pick would defeat that policy silently.
        seedMinCache("GD")
        seedTask(id = "GD-heavy", taskType = "GD", usageCount = 99L)

        val result = manager.getTasksByType("GD", count = 1)

        assertTrue(result.isSuccess)
        val picked = result.getOrThrow().single()
        assertNotEquals("GD-heavy", picked.id)

        val row = database.sharedDatabaseQueries
            .selectLeastUsedGTOTasksByType("GD", 20)
            .executeAsList()
            .first { it.id == picked.id }
        assertEquals(1L, row.usageCount) // markTasksUsed incremented usage on the picked row
        assertNotNull(row.lastUsed)
    }

    @Test
    fun `getRandomTasks never returns a heavily-used task while lighter ones exist`() = runTest {
        seedMinCache("GD")
        seedTask(id = "extra-heavy", taskType = "GD", usageCount = 500L)

        val result = manager.getRandomTasks(count = 5)

        assertTrue(result.isSuccess)
        assertFalse(result.getOrThrow().any { it.id == "extra-heavy" })
    }

    @Test
    fun `getTasksByType fails honestly when the requested type has no cached rows`() = runTest {
        // Why this matters: MIN_CACHE_SIZE is checked against the *overall* total, not
        // per-type -- so a well-stocked cache of one type must still fail cleanly for a
        // different, never-cached type instead of silently returning an empty success.
        seedMinCache("GD")

        val result = manager.getTasksByType("LECTURETTE", count = 1)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getCacheStatus counts tasks per known type and reports the latest sync time`() = runTest {
        seedTask(id = "gd1", taskType = "GD")
        seedTask(id = "gpe1", taskType = "GPE")
        seedTask(id = "pgt1", taskType = "PGT")
        database.sharedDatabaseQueries.insertOrReplaceGTOBatchMetadata(
            batchId = "batch_001",
            downloadedAt = 555L,
            taskCount = 3,
            version = "1.0.0"
        )

        val status = manager.getCacheStatus()

        assertEquals(3, status.cachedTasks)
        assertEquals(1, status.batchesDownloaded)
        assertEquals(555L, status.lastSyncTime)
        assertEquals(1, status.gdTaskCount)
        assertEquals(1, status.gpeTaskCount)
        assertEquals(1, status.pgtTaskCount)
        assertEquals(0, status.commandTaskCount)
    }

    @Test
    fun `clearCache empties the task table so isContentCached would honestly report false`() = runTest {
        seedMinCache("GD")

        manager.clearCache()

        assertEquals(0L, database.sharedDatabaseQueries.selectTotalGTOTaskCount().executeAsOne())
    }
}
