package com.ssbmax.core.data.repository

import com.ssbmax.shared.domain.model.GenderTag
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PPDTImageCacheManager tests for gender filter at SQL layer, image retrieval, and cache management.
 * initialSync + schema + TTL tests → PPDTImageCacheManagerTest
 */
class PPDTImageCacheGenderRetrievalTest : PPDTImageCacheManagerTestBase() {

    // ==================== Cache A: Gender filter must live at DAO layer ====================

    @Test
    fun `getImageForTest with gender tag does not call getLeastUsedImages`() = runTest {
        // WHY: Current code fetches 15 images with getLeastUsedImages then filters in-memory.
        // In-memory filtering discards images from the cache pool without accounting for gender,
        // making the selection non-deterministic and wasting a DB round-trip on irrelevant rows.
        // The DAO query (getLeastUsedImagesByGender) must be the single filter — SSOT/SOLID SR.
        coEvery { mockDao.getTotalImageCount() } returns 15

        cacheManager.getImageForTest(genderTag = GenderTag.FEMALE)

        coVerify(exactly = 0) { mockDao.getLeastUsedImages(any()) }
    }

    // ==================== Get Image For Test ====================

    @Test
    fun `getImageForTest returns least-used image`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 15
        coEvery { mockDao.getLeastUsedImages(1) } returns listOf(mockImages.first())

        val result = cacheManager.getImageForTest()

        assertTrue("Should succeed", result.isSuccess)
        val question = result.getOrNull()
        assertNotNull("Should return question", question)
        assertEquals("ppdt_001", question!!.id)
        // WHY count=1: SQL already orders by usageCount ASC, RANDOM() — no need to fetch a pool
        coVerify { mockDao.getLeastUsedImages(1) }
        coVerify { mockDao.markImagesAsUsed(listOf("ppdt_001"), any()) }
    }

    @Test
    fun `getImageForTest normalizes gs URLs to https`() = runTest {
        val gsImage = createMockImage("ppdt_gs", "gs://my-bucket/ppdt/image.jpg")
        coEvery { mockDao.getTotalImageCount() } returns 15
        coEvery { mockDao.getLeastUsedImages(any()) } returns listOf(gsImage)

        val result = cacheManager.getImageForTest()

        assertTrue("Should succeed", result.isSuccess)
        assertEquals(
            "gs:// URL should be normalized to https",
            "https://storage.googleapis.com/my-bucket/ppdt/image.jpg",
            result.getOrNull()!!.imageUrl
        )
    }

    @Test
    fun `getImageForTest preserves https URLs unchanged`() = runTest {
        val httpsImage = createMockImage(
            "ppdt_https",
            "https://storage.googleapis.com/bucket/img.jpg"
        )
        coEvery { mockDao.getTotalImageCount() } returns 15
        coEvery { mockDao.getLeastUsedImages(any()) } returns listOf(httpsImage)

        val result = cacheManager.getImageForTest()

        assertTrue("Should succeed", result.isSuccess)
        assertEquals(
            "https URL should pass through unchanged",
            "https://storage.googleapis.com/bucket/img.jpg",
            result.getOrNull()!!.imageUrl
        )
    }

    @Test
    fun `getImageForTest refreshes cache when below minimum`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 3
        setupFirestoreVersionFailure()

        val result = cacheManager.getImageForTest()

        assertTrue("Should fail when cache is low and sync fails", result.isFailure)
    }

    @Test
    fun `getImageForTest fails when cache is empty`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 15
        coEvery { mockDao.getLeastUsedImages(any()) } returns emptyList()

        val result = cacheManager.getImageForTest()

        assertTrue("Should fail when no images", result.isFailure)
    }

    // ==================== Get Multiple Images ====================

    @Test
    fun `getImagesForTest returns requested count`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 15
        coEvery { mockDao.getLeastUsedImages(3) } returns mockImages.take(3)

        val result = cacheManager.getImagesForTest(3)

        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should return 3 questions", 3, result.getOrNull()!!.size)
        coVerify { mockDao.markImagesAsUsed(match { it.size == 3 }, any()) }
    }

    // ==================== Cache Status ====================

    @Test
    fun `getCacheStatus returns accurate statistics`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 15
        coEvery { mockDao.getDownloadedImageCount() } returns 5
        coEvery { mockDao.getTotalBatchCount() } returns 1
        coEvery { mockDao.getAllBatchMetadata() } returns listOf(localMeta(version = "2.0.0"))

        val status = cacheManager.getCacheStatus()

        assertEquals(15, status.cachedImages)
        assertEquals(5, status.downloadedImages)
        assertEquals(1, status.batchesDownloaded)
        assertNotNull(status.lastSyncTime)
    }

    @Test
    fun `getCacheStatus handles empty cache gracefully`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 0
        coEvery { mockDao.getDownloadedImageCount() } returns 0
        coEvery { mockDao.getTotalBatchCount() } returns 0
        coEvery { mockDao.getAllBatchMetadata() } returns emptyList()

        val status = cacheManager.getCacheStatus()

        assertEquals(0, status.cachedImages)
        assertEquals(0, status.downloadedImages)
        assertNull(status.lastSyncTime)
    }

    // ==================== Clear Cache ====================

    @Test
    fun `clearCache removes all images from database`() = runTest {
        cacheManager.clearCache()

        coVerify { mockDao.clearAllImages() }
    }

    // ==================== Get Image By ID ====================

    @Test
    fun `getImageById returns cached image`() = runTest {
        coEvery { mockDao.getImageById("ppdt_001") } returns mockImages.first()

        val result = cacheManager.getImageById("ppdt_001")

        assertTrue("Should succeed", result.isSuccess)
        assertEquals("ppdt_001", result.getOrNull()!!.id)
    }

    @Test
    fun `getImageById fails when image not in cache`() = runTest {
        coEvery { mockDao.getImageById("missing") } returns null

        val result = cacheManager.getImageById("missing")

        assertTrue("Should fail", result.isFailure)
    }
}
