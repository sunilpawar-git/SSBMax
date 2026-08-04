package com.ssbmax.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ssbmax.shared.db.SharedDatabase
import com.ssbmax.shared.domain.model.GenderTag
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises GitLivePPDTImageCacheManager's SQLDelight-only surface against a real
 * in-memory SQLite DB. `downloadBatch`/`initialSync`'s Firestore reads (including the
 * staleness-check network call) need a live/emulated backend outside this JVM unit
 * test's reach, same category of gap as the GTO/WAT manager tests -- seeding stays
 * at or above MIN_CACHE_SIZE (5) so `getImageForTest`/`getImageById` never trigger
 * `initialSync` internally.
 */
class GitLivePPDTImageCacheManagerTest {

    private lateinit var database: SharedDatabase
    private lateinit var manager: GitLivePPDTImageCacheManager

    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SharedDatabase.Schema.create(driver)
        database = SharedDatabase(driver)
        manager = GitLivePPDTImageCacheManager(database)
    }

    private fun seedImage(
        id: String,
        genderTag: String = "MIXED",
        usageCount: Long = 0L,
        imageUrl: String = "https://example.com/$id.jpg"
    ) {
        database.sharedDatabaseQueries.insertOrReplacePPDTImage(
            id = id,
            imageUrl = imageUrl,
            imageDescription = "desc-$id",
            imageContextJson = "{}",
            viewingTimeSeconds = 30L,
            writingTimeMinutes = 4L,
            minCharacters = 200L,
            maxCharacters = 1000L,
            category = null,
            difficulty = null,
            batchId = "batch_001",
            cachedAt = 0L,
            lastUsed = null,
            usageCount = usageCount,
            genderTag = genderTag
        )
    }

    private fun seedMinCache() {
        repeat(5) { i -> seedImage(id = "p$i") }
    }

    @Test
    fun `getImageForTest with a gender tag never returns an image tagged for the other gender`() = runTest {
        // Why this matters: the gender filter must live at the SQL layer (WHERE genderTag = ? OR
        // genderTag = 'MIXED') -- an in-memory post-filter could silently serve a MALE-only image
        // to a FEMALE candidate if the pool ordering changed.
        seedImage(id = "male_only", genderTag = "MALE", usageCount = 0L)
        seedMinCache()

        val result = manager.getImageForTest(genderTag = GenderTag.FEMALE)

        assertTrue(result.isSuccess)
        assertEquals(false, result.getOrThrow().id == "male_only")
    }

    @Test
    fun `getImageForTest marks the returned image used exactly once`() = runTest {
        seedMinCache()

        val result = manager.getImageForTest()

        assertTrue(result.isSuccess)
        val picked = result.getOrThrow()
        val row = database.sharedDatabaseQueries.selectPPDTImageById(picked.id).executeAsOne()
        assertEquals(1L, row.usageCount)
    }

    @Test
    fun `getImageForTest normalizes gs URLs to https`() = runTest {
        seedImage(id = "gs_img", imageUrl = "gs://my-bucket/ppdt/image.jpg")
        seedMinCache()

        val result = manager.getImageForTest()

        assertTrue(result.isSuccess)
        // At least the gs image, if picked, must be normalized; assert on direct lookup instead
        // so the assertion doesn't depend on which least-used row the query happens to return.
        val entityResult = manager.getImageById("gs_img")
        assertTrue(entityResult.isSuccess)
        assertEquals("https://storage.googleapis.com/my-bucket/ppdt/image.jpg", entityResult.getOrThrow().imageUrl)
    }

    @Test
    fun `getImageById fails honestly for an unknown id`() = runTest {
        val result = manager.getImageById("does-not-exist")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getCacheStatus reports cached image and batch counts`() = runTest {
        seedMinCache()
        database.sharedDatabaseQueries.insertOrReplacePPDTBatchMetadata(
            batchId = "batch_001",
            downloadedAt = 999L,
            imageCount = 5,
            version = "1.0.0",
            lastStalenessCheckAt = 0L
        )

        val status = manager.getCacheStatus()

        assertEquals(5, status.cachedImages)
        assertEquals(1, status.batchesDownloaded)
        assertEquals(999L, status.lastSyncTime)
        assertEquals(0, status.downloadedImages)
    }

    @Test
    fun `clearCache empties the image table`() = runTest {
        seedMinCache()

        manager.clearCache()

        assertEquals(0L, database.sharedDatabaseQueries.selectTotalPPDTImageCount().executeAsOne())
    }
}
