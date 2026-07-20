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
 * Exercises GitLiveTATImageCacheManager's SQLDelight-only surface against a real
 * in-memory SQLite DB. `downloadBatch`/`initialSync`'s Firestore reads (including
 * the staleness-check network call) need a live/emulated backend outside this JVM
 * unit test's reach, same category of gap as the GTO/WAT manager tests -- seeding
 * at or above MINIMUM_CACHE_SIZE (33: 3 per position across 11 positions) keeps
 * `getImagesForTest` from ever calling `initialSync` internally.
 */
class GitLiveTATImageCacheManagerTest {

    private lateinit var database: SharedDatabase
    private lateinit var manager: GitLiveTATImageCacheManager

    @Before
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SharedDatabase.Schema.create(driver)
        database = SharedDatabase(driver)
        manager = GitLiveTATImageCacheManager(database)
    }

    private fun seedImage(id: String, cardPosition: Long, genderTag: String = "MIXED", usageCount: Long = 0L) {
        database.sharedDatabaseQueries.insertOrReplaceTATImage(
            id = id,
            imageUrl = "https://example.com/$id.jpg",
            cardPosition = cardPosition,
            genderTag = genderTag,
            imageContextJson = "{}",
            viewingTimeSeconds = 30L,
            writingTimeMinutes = 4L,
            minCharacters = 150L,
            maxCharacters = 1500L,
            category = null,
            difficulty = null,
            batchId = "batch_001",
            cachedAt = 0L,
            lastUsed = null,
            usageCount = usageCount
        )
    }

    /** 3 images per position (1-11) = 33, meeting MINIMUM_CACHE_SIZE without triggering a sync. */
    private fun seedFullPool(genderTag: String = "MIXED") {
        for (position in 1..11) {
            repeat(3) { i -> seedImage(id = "pos${position}_$i", cardPosition = position.toLong(), genderTag = genderTag) }
        }
    }

    @Test
    fun `getImagesForTest assembles 12 cards -- 11 from the pool plus one programmatic blank`() = runTest {
        // Why this matters: TAT is a fixed 12-card test; position 12 is never stored in the DB
        // (it's the constant blank card) -- assembly must always synthesize it, not fail because
        // the pool has no row for position 12.
        seedFullPool()

        val result = manager.getImagesForTest()

        assertTrue(result.isSuccess)
        val questions = result.getOrThrow()
        assertEquals(12, questions.size)
        assertEquals(12, questions.last().cardPosition)
        assertEquals("blank_card", questions.last().id)
        assertEquals((1..11).toList(), questions.dropLast(1).map { it.cardPosition })
    }

    @Test
    fun `getImagesForTest never serves a MALE-only image to a FEMALE candidate`() = runTest {
        // Why this matters: gender routing must live at the SQL layer (cardPosition match AND
        // (genderTag = requested OR MIXED)) -- a fallback bug could leak a MALE-only image.
        seedFullPool(genderTag = "MALE")
        for (position in 1..11) seedImage(id = "mixed_$position", cardPosition = position.toLong(), genderTag = "MIXED")

        val result = manager.getImagesForTest(genderTag = GenderTag.FEMALE)

        assertTrue(result.isSuccess)
        val ids = result.getOrThrow().map { it.id }
        assertTrue(ids.none { it.startsWith("pos") }) // none of the MALE-only rows were used
    }

    @Test
    fun `getImagesForTest marks the picked image at each position used exactly once`() = runTest {
        // Why this matters: usage-count-ordered selection only rotates candidates through the
        // pool if usage actually gets recorded -- an unmarked pick would keep returning the
        // same image at that position forever.
        seedFullPool()

        val result = manager.getImagesForTest()
        assertTrue(result.isSuccess)
        val pickedIds = result.getOrThrow().dropLast(1).map { it.id }.toSet()

        for (position in 1..11) {
            // After marking, the next least-used pick for this position must be a different,
            // still-unused (usageCount 0) row -- proving the previously-picked row's count moved.
            val nextPick = database.sharedDatabaseQueries
                .selectLeastUsedTATImageByPosition(position.toLong(), GenderTag.MIXED.name)
                .executeAsOneOrNull()
            assertTrue(nextPick != null && nextPick.id !in pickedIds)
        }
    }

    @Test
    fun `getImagesForTest fails honestly when a card position has no eligible image`() = runTest {
        // Only seed 10 of the 11 required positions -- position 11 is missing entirely.
        for (position in 1..10) {
            repeat(3) { i -> seedImage(id = "pos${position}_$i", cardPosition = position.toLong()) }
        }
        // Pad up to MINIMUM_CACHE_SIZE with extra images at position 1 so the sync-trigger
        // threshold doesn't mask this into an initialSync() attempt.
        repeat(3) { i -> seedImage(id = "extra_$i", cardPosition = 1L) }

        val result = manager.getImagesForTest()

        assertTrue(result.isFailure)
    }

    @Test
    fun `getCacheStatus reports cached image and batch counts`() = runTest {
        seedFullPool()
        database.sharedDatabaseQueries.insertOrReplaceTATBatchMetadata(
            batchId = "batch_001", downloadedAt = 42L, imageCount = 33, version = "1.0.0", lastStalenessCheckAt = 0L
        )

        val status = manager.getCacheStatus()

        assertEquals(33, status.cachedImages)
        assertEquals(1, status.batchesDownloaded)
        assertEquals(42L, status.lastSyncTime)
    }

    @Test
    fun `clearCache empties the image table`() = runTest {
        seedFullPool()

        manager.clearCache()

        assertEquals(0L, database.sharedDatabaseQueries.selectTotalTATImageCount().executeAsOne())
    }
}
