package com.ssbmax.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ssbmax.shared.db.SharedDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises GitLiveGPEImageCacheManager's SQLDelight-only surface against a real
 * in-memory SQLite DB, seeded directly via the generated queries -- `downloadBatch`/
 * `initialSync`'s Firestore read needs a live/emulated backend outside this JVM
 * unit test's reach, same category of gap as the GTO/WAT manager tests.
 */
class GitLiveGPEImageCacheManagerTest {

    private lateinit var database: SharedDatabase
    private lateinit var manager: GitLiveGPEImageCacheManager

    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SharedDatabase.Schema.create(driver)
        database = SharedDatabase(driver)
        manager = GitLiveGPEImageCacheManager(database)
    }

    private fun seedImage(
        id: String,
        solution: String? = "Cross the river using the rope and planks.",
        usageCount: Long = 0L
    ) {
        database.sharedDatabaseQueries.insertOrReplaceGPEImage(
            id = id,
            imageUrl = "https://example.com/$id.jpg",
            scenario = "Tactical scenario for $id",
            solution = solution,
            imageDescription = "desc-$id",
            resources = null,
            viewingTimeSeconds = 60L,
            planningTimeSeconds = 1740L,
            minCharacters = 500L,
            maxCharacters = 2000L,
            category = "river_crossing",
            difficulty = "medium",
            batchId = "batch_001",
            cachedAt = 0L,
            lastUsed = null,
            usageCount = usageCount
        )
    }

    @Test
    fun `getImageForTest returns least-used image and marks it used`() = runTest {
        // Why this matters: usage-count-ordered selection rotates users through the
        // pool instead of always handing back the same scenario.
        repeat(5) { i -> seedImage(id = "g$i") }
        seedImage(id = "heavy", usageCount = 999L)

        val result = manager.getImageForTest()

        assertTrue(result.isSuccess)
        val question = result.getOrThrow()
        assertFalse(question.id == "heavy")

        val row = database.sharedDatabaseQueries.selectLeastUsedGPEImages(10).executeAsList()
            .first { it.id == question.id }
        assertEquals(1L, row.usageCount)
    }

    @Test
    fun `getImageForTest fails honestly when the cache is entirely empty`() = runTest {
        // Why this matters: an empty cache must surface as Result.failure, not an empty-but-successful
        // question, so TestContentRepositoryImpl can trigger a real resync instead of masking the gap.
        val result = manager.getImageForTest()

        assertTrue(result.isFailure)
    }

    @Test
    fun `getCacheStatus reports cached image and batch counts`() = runTest {
        seedImage(id = "g1")
        seedImage(id = "g2")
        database.sharedDatabaseQueries.insertOrReplaceGPEBatchMetadata(
            batchId = "batch_001",
            downloadedAt = 555L,
            imageCount = 2,
            version = "1.0.0"
        )

        val status = manager.getCacheStatus()

        assertEquals(2, status.cachedImages)
        assertEquals(1, status.batchesDownloaded)
        assertEquals(555L, status.lastSyncTime)
        // downloadedImages is always 0 -- markImageAsDownloaded is dead in production, see class doc.
        assertEquals(0, status.downloadedImages)
    }

    @Test
    fun `clearCache empties the image table`() = runTest {
        seedImage(id = "g1")

        manager.clearCache()

        assertEquals(0L, database.sharedDatabaseQueries.selectTotalGPEImageCount().executeAsOne())
    }
}
