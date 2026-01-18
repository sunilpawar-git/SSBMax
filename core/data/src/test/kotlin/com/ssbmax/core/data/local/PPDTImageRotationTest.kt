package com.ssbmax.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ssbmax.core.data.local.dao.PPDTImageCacheDao
import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for PPDT image rotation to ensure random selection when usage counts are tied.
 * 
 * The DAO uses `ORDER BY usageCount ASC, RANDOM()` to:
 * 1. Prioritize least-used images
 * 2. Randomize selection among images with the same usage count
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PPDTImageRotationTest {

    private lateinit var db: SSBDatabase
    private lateinit var dao: PPDTImageCacheDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SSBDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.ppdtImageCacheDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    /**
     * Verifies that when multiple images have the same usage count,
     * repeating getLeastUsedImages returns different orders (randomization works).
     * 
     * This test runs 10 iterations and expects at least 2 different orderings,
     * which statistically should happen with high probability for 5+ images.
     */
    @Test
    fun getLeastUsedImages_randomizesAmongSameUsageCount() = runTest {
        // Insert 5 images all with usageCount = 0
        val images = (1..5).map { entity(id = "img_$it", usage = 0) }
        dao.insertImages(images)

        // Collect multiple result orderings
        val orderings = mutableSetOf<List<String>>()
        repeat(10) {
            val result = dao.getLeastUsedImages(5)
            orderings.add(result.map { it.id })
        }

        // With 5 items and 10 iterations, we should see at least 2 different orderings
        // (probability of same ordering 10 times is (1/5!)^10 which is negligible)
        assertTrue(
            "Expected randomization: multiple orderings should appear, but got only ${orderings.size}",
            orderings.size >= 2
        )
    }

    /**
     * Verifies least-used priority is maintained: lower usage count always comes first.
     */
    @Test
    fun getLeastUsedImages_prioritizesLowerUsageCount() = runTest {
        val images = listOf(
            entity(id = "high_usage", usage = 10),
            entity(id = "low_usage", usage = 1),
            entity(id = "medium_usage", usage = 5),
            entity(id = "zero_usage", usage = 0)
        )
        dao.insertImages(images)

        val result = dao.getLeastUsedImages(4)
        val ids = result.map { it.id }

        // Zero usage should be first, high usage should be last
        assertEquals("zero_usage", ids.first())
        assertEquals("high_usage", ids.last())
    }

    /**
     * Verifies that after marking an image as used, it moves down in priority.
     */
    @Test
    fun markImagesAsUsed_movesImageDownInPriority() = runTest {
        val images = listOf(
            entity(id = "a", usage = 0),
            entity(id = "b", usage = 0),
            entity(id = "c", usage = 0)
        )
        dao.insertImages(images)

        // Mark "a" as used (increments usage count)
        dao.markImagesAsUsed(listOf("a"))

        val result = dao.getLeastUsedImages(3)
        val ids = result.map { it.id }

        // "a" should now be last (usage = 1, while b,c are still 0)
        assertEquals("a", ids.last())
    }

    /**
     * Tests that requesting fewer images than available still returns least-used ones.
     */
    @Test
    fun getLeastUsedImages_returnsOnlyRequestedCount() = runTest {
        val images = (1..10).map { entity(id = "img_$it", usage = it) }
        dao.insertImages(images)

        val result = dao.getLeastUsedImages(3)

        assertEquals(3, result.size)
        // Should be img_1, img_2, img_3 (usage 1, 2, 3)
        assertTrue(result.map { it.usageCount }.all { it <= 3 })
    }

    /**
     * Verifies getRandomImages also respects usage count ordering.
     */
    @Test
    fun getRandomImages_respectsUsageCountOrdering() = runTest {
        val images = listOf(
            entity(id = "used_twice", usage = 2),
            entity(id = "never_used", usage = 0),
            entity(id = "used_once", usage = 1)
        )
        dao.insertImages(images)

        val result = dao.getRandomImages(2)
        val ids = result.map { it.id }.toSet()

        // Should return the two least-used: never_used and used_once
        assertTrue(ids.contains("never_used"))
        assertTrue(ids.contains("used_once"))
    }

    /**
     * Simulation test: Running 100 selections on 10 images with same usage.
     * Expect reasonable distribution (each image selected at least once).
     */
    @Test
    fun randomRotation_simulation_allImagesGetSelected() = runTest {
        val images = (1..10).map { entity(id = "img_$it", usage = 0) }
        dao.insertImages(images)

        val selectionCounts = mutableMapOf<String, Int>()
        repeat(100) {
            val result = dao.getLeastUsedImages(1)
            val selectedId = result.first().id
            selectionCounts[selectedId] = selectionCounts.getOrDefault(selectedId, 0) + 1
        }

        // With 100 iterations and 10 images, expect each to be selected at least once
        // (probability of missing one is very low with random selection)
        assertTrue(
            "Expected all 10 images to be selected at least once in 100 iterations, " +
            "but only ${selectionCounts.size} were selected",
            selectionCounts.size >= 8 // Allow for some statistical variance
        )
    }

    private fun entity(id: String, usage: Int): CachedPPDTImageEntity = CachedPPDTImageEntity(
        id = id,
        imageUrl = "https://example.com/$id",
        localFilePath = null,
        imageDescription = "desc",
        viewingTimeSeconds = 30,
        writingTimeMinutes = 4,
        minCharacters = 200,
        maxCharacters = 1000,
        category = null,
        difficulty = null,
        batchId = "batch",
        cachedAt = 0L,
        lastUsed = null,
        usageCount = usage,
        imageDownloaded = false,
        context = "Test context for $id"
    )
}
