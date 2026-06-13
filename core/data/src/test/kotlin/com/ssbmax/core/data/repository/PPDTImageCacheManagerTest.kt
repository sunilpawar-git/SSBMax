package com.ssbmax.core.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.PPDTImageCacheDao
import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
import com.ssbmax.core.data.local.entity.PPDTBatchMetadataEntity
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PPDTImageCacheManager
 * Tests cache initialization, image retrieval, URL normalization, and cache management
 */
class PPDTImageCacheManagerTest {

    private lateinit var cacheManager: PPDTImageCacheManager
    private val mockDao = mockk<PPDTImageCacheDao>(relaxed = true)
    private val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)

    private val mockImages = createMockImages(15)

    @Before
    fun setup() {
        cacheManager = PPDTImageCacheManager(mockDao, mockFirestore)
        clearAllMocks()
    }

    // ==================== Initial Sync Tests ====================

    @Test
    fun `initialSync downloads batch when cache is empty`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 0

        val mockDoc = mockk<DocumentSnapshot>(relaxed = true)
        every { mockDoc.exists() } returns true
        every { mockDoc.get("images") } returns mockImages.map { it.toFirestoreMap() }
        every { mockDoc.getString("version") } returns "1.0.0"

        val mockTask = mockk<com.google.android.gms.tasks.Task<DocumentSnapshot>>(relaxed = true)
        every { mockTask.result } returns mockDoc
        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns true
        every { mockTask.isCanceled } returns false
        every { mockTask.exception } returns null

        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
        every { mockDocRef.get() } returns mockTask
        every { mockFirestore.document(any()) } returns mockDocRef

        val result = cacheManager.initialSync()

        assertTrue("Should succeed", result.isSuccess)
        coVerify { mockDao.insertImages(any()) }
        coVerify { mockDao.insertBatchMetadata(any()) }
    }

    @Test
    fun `initialSync skips download when cache has sufficient images`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 15

        val result = cacheManager.initialSync()

        assertTrue("Should succeed", result.isSuccess)
        coVerify(exactly = 0) { mockDao.insertImages(any()) }
    }

    @Test
    fun `initialSync returns failure when Firestore fails`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 0

        val mockTask = mockk<com.google.android.gms.tasks.Task<DocumentSnapshot>>(relaxed = true)
        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns false
        every { mockTask.isCanceled } returns false
        every { mockTask.exception } returns Exception("Network error")

        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
        every { mockDocRef.get() } returns mockTask
        every { mockFirestore.document(any()) } returns mockDocRef

        val result = cacheManager.initialSync()

        assertTrue("Should fail", result.isFailure)
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

        coVerify { mockDao.getLeastUsedImages(1) }
        coVerify { mockDao.markImagesAsUsed(listOf("ppdt_001"), any()) }
    }

    @Test
    fun `getImageForTest normalizes gs URLs to https`() = runTest {
        val gsImage = createMockImage("ppdt_gs", "gs://my-bucket/ppdt/image.jpg")
        coEvery { mockDao.getTotalImageCount() } returns 15
        coEvery { mockDao.getLeastUsedImages(1) } returns listOf(gsImage)

        val result = cacheManager.getImageForTest()

        assertTrue("Should succeed", result.isSuccess)
        val question = result.getOrNull()!!
        assertEquals(
            "gs:// URL should be normalized to https",
            "https://storage.googleapis.com/my-bucket/ppdt/image.jpg",
            question.imageUrl
        )
    }

    @Test
    fun `getImageForTest preserves https URLs unchanged`() = runTest {
        val httpsImage = createMockImage("ppdt_https", "https://storage.googleapis.com/bucket/img.jpg")
        coEvery { mockDao.getTotalImageCount() } returns 15
        coEvery { mockDao.getLeastUsedImages(1) } returns listOf(httpsImage)

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
        // First call returns below minimum, triggering sync which will then still find empty
        coEvery { mockDao.getTotalImageCount() } returns 3

        val mockTask = mockk<com.google.android.gms.tasks.Task<DocumentSnapshot>>(relaxed = true)
        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns false
        every { mockTask.exception } returns Exception("Network error")

        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
        every { mockDocRef.get() } returns mockTask
        every { mockFirestore.document(any()) } returns mockDocRef

        val result = cacheManager.getImageForTest()

        assertTrue("Should fail when cache is low and sync fails", result.isFailure)
    }

    @Test
    fun `getImageForTest fails when cache is empty`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 15
        coEvery { mockDao.getLeastUsedImages(1) } returns emptyList()

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
        coEvery { mockDao.getAllBatchMetadata() } returns listOf(
            PPDTBatchMetadataEntity(
                batchId = "batch_001",
                downloadedAt = System.currentTimeMillis(),
                imageCount = 15,
                version = "1.0.0"
            )
        )

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

    // ==================== Helper Methods ====================

    private fun createMockImages(count: Int): List<CachedPPDTImageEntity> {
        return (1..count).map { index ->
            val paddedNum = String.format("%03d", index)
            CachedPPDTImageEntity(
                id = "ppdt_$paddedNum",
                imageUrl = "https://storage.googleapis.com/test/ppdt_$paddedNum.jpg",
                localFilePath = null,
                imageDescription = "Test image $index showing an ambiguous scene",
                imageContextJson = "{}",
                viewingTimeSeconds = 30,
                writingTimeMinutes = 4,
                minCharacters = 200,
                maxCharacters = 1000,
                category = "leadership",
                difficulty = "medium",
                batchId = "batch_001",
                cachedAt = System.currentTimeMillis(),
                lastUsed = null,
                usageCount = 0,
                imageDownloaded = false
            )
        }
    }

    private fun createMockImage(id: String, imageUrl: String): CachedPPDTImageEntity {
        return CachedPPDTImageEntity(
            id = id,
            imageUrl = imageUrl,
            localFilePath = null,
            imageDescription = "Test image",
            imageContextJson = "{}",
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4,
            minCharacters = 200,
            maxCharacters = 1000,
            category = null,
            difficulty = "medium",
            batchId = "batch_001",
            cachedAt = System.currentTimeMillis(),
            lastUsed = null,
            usageCount = 0,
            imageDownloaded = false
        )
    }

    private fun CachedPPDTImageEntity.toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "imageUrl" to imageUrl,
            "imageDescription" to imageDescription,
            "imageContext" to emptyMap<String, Any>(),
            "genderTag" to genderTag.name,
            "viewingTimeSeconds" to viewingTimeSeconds,
            "writingTimeMinutes" to writingTimeMinutes,
            "minCharacters" to minCharacters,
            "maxCharacters" to maxCharacters,
            "category" to (category ?: ""),
            "difficulty" to (difficulty ?: "medium")
        )
    }
}
