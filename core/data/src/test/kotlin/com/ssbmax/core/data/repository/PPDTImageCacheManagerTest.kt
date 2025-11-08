package com.ssbmax.core.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.PPDTImageCacheDao
import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
import com.ssbmax.core.data.local.entity.PPDTBatchMetadataEntity
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Unit tests for PPDTImageCacheManager
 * 
 * Tests caching workflows:
 * - Initial sync and batch downloads
 * - Image retrieval for tests
 * - Usage tracking and rotation
 * - Cache status and diagnostics
 * - Error handling
 */
class PPDTImageCacheManagerTest {
    
    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            // Mock android.util.Log for all tests
            mockkStatic(Log::class)
            every { Log.d(any(), any()) } returns 0
            every { Log.e(any(), any()) } returns 0
            every { Log.e(any(), any(), any()) } returns 0
            every { Log.w(any(), any<String>()) } returns 0
            every { Log.i(any(), any()) } returns 0
            every { Log.v(any(), any()) } returns 0
        }
    }
    
    private lateinit var cacheManager: PPDTImageCacheManager
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockDao: PPDTImageCacheDao
    
    @Before
    fun setup() {
        mockFirestore = mockk(relaxed = true)
        mockDao = mockk(relaxed = true)
        
        cacheManager = PPDTImageCacheManager(
            dao = mockDao,
            firestore = mockFirestore
        )
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== Initial Sync Tests ====================
    
    @Test
    fun `initialSync downloads batch_001 when cache is empty`() = runTest {
        // Given - empty cache
        coEvery { mockDao.getTotalImageCount() } returns 0
        
        // Mock Firestore document fetch
        val mockDoc = mockk<DocumentSnapshot>()
        
        every { mockDoc.exists() } returns true
        every { mockDoc.getString("version") } returns "1.0.0"
        every { mockDoc.get("images") } returns listOf(
            createMockImageMap("ppdt_001", "https://example.com/image1.jpg")
        )
        
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        every { mockDocRef.get() } returns Tasks.forResult(mockDoc)
        
        every { 
            mockFirestore.document("test_content/ppdt/image_batches/batch_001")
        } returns mockDocRef
        
        coEvery { mockDao.insertImages(any()) } just Runs
        coEvery { mockDao.insertBatchMetadata(any()) } just Runs
        
        // When
        val result = cacheManager.initialSync()
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        coVerify { mockDao.insertImages(any()) }
        coVerify { mockDao.insertBatchMetadata(any()) }
    }
    
    @Test
    fun `initialSync skips download when cache already initialized`() = runTest {
        // Given - cache has 15+ images (TARGET_CACHE_SIZE)
        coEvery { mockDao.getTotalImageCount() } returns 15
        
        // When
        val result = cacheManager.initialSync()
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        // Verify no download attempt
        verify(exactly = 0) { mockFirestore.document(any()) }
    }
    
    @Test
    fun `downloadBatch parses images correctly`() = runTest {
        // Given
        val mockDoc = mockk<DocumentSnapshot>()
        
        every { mockDoc.exists() } returns true
        every { mockDoc.getString("version") } returns "1.0.0"
        every { mockDoc.get("images") } returns listOf(
            createMockImageMap("ppdt_001", "https://example.com/image1.jpg", viewingTime = 30L, writingTime = 4L),
            createMockImageMap("ppdt_002", "https://example.com/image2.jpg")
        )
        
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        every { mockDocRef.get() } returns Tasks.forResult(mockDoc)
        
        every { 
            mockFirestore.document("test_content/ppdt/image_batches/batch_001")
        } returns mockDocRef
        
        coEvery { mockDao.insertImages(any()) } just Runs
        coEvery { mockDao.insertBatchMetadata(any()) } just Runs
        
        // When
        val result = cacheManager.downloadBatch("batch_001")
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        
        coVerify { 
            mockDao.insertImages(match { images ->
                images.size == 2 &&
                images[0].id == "ppdt_001" &&
                images[0].viewingTimeSeconds == 30 &&
                images[0].writingTimeMinutes == 4
            })
        }
    }
    
    @Test
    fun `downloadBatch handles missing batch gracefully`() = runTest {
        // Given - batch doesn't exist
        val mockDoc = mockk<DocumentSnapshot>()
        
        every { mockDoc.exists() } returns false
        
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        every { mockDocRef.get() } returns Tasks.forResult(mockDoc)
        
        every { 
            mockFirestore.document("test_content/ppdt/image_batches/batch_999")
        } returns mockDocRef
        
        // When
        val result = cacheManager.downloadBatch("batch_999")
        
        // Then
        assertTrue("Should fail", result.isFailure)
        assertTrue(
            "Should mention batch not found",
            result.exceptionOrNull()?.message?.contains("not found") == true
        )
    }
    
    // ==================== Get Image For Test ====================
    
    @Test
    fun `getImageForTest returns least used image`() = runTest {
        // Given - cache has images
        coEvery { mockDao.getTotalImageCount() } returns 10
        coEvery { mockDao.getLeastUsedImages(1) } returns listOf(
            createMockCachedImage("ppdt_005", usageCount = 0)
        )
        coEvery { mockDao.markImagesAsUsed(any()) } just Runs
        
        // When
        val result = cacheManager.getImageForTest()
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        val question = result.getOrNull()!!
        assertEquals("Should return ppdt_005", "ppdt_005", question.id)
        
        coVerify { mockDao.markImagesAsUsed(listOf("ppdt_005")) }
    }
    
    @Test
    fun `getImageForTest triggers sync when cache below minimum`() = runTest {
        // Given - cache below MIN_CACHE_SIZE (5)
        var cacheCount = 3
        coEvery { mockDao.getTotalImageCount() } answers { cacheCount }
        
        // Mock initialSync behavior
        val mockDoc = mockk<DocumentSnapshot>()
        
        every { mockDoc.exists() } returns true
        every { mockDoc.getString("version") } returns "1.0.0"
        every { mockDoc.get("images") } returns listOf(
            createMockImageMap("ppdt_001", "https://example.com/image1.jpg")
        )
        
        val mockDocRef = mockk<com.google.firebase.firestore.DocumentReference>()
        every { mockDocRef.get() } returns Tasks.forResult(mockDoc)
        
        every { 
            mockFirestore.document(any())
        } returns mockDocRef
        
        coEvery { mockDao.insertImages(any()) } answers {
            cacheCount = 10 // Simulate cache populated
        }
        coEvery { mockDao.insertBatchMetadata(any()) } just Runs
        coEvery { mockDao.getLeastUsedImages(1) } returns listOf(
            createMockCachedImage("ppdt_001")
        )
        coEvery { mockDao.markImagesAsUsed(any()) } just Runs
        
        // When
        val result = cacheManager.getImageForTest()
        
        // Then
        assertTrue("Should succeed after sync", result.isSuccess)
        coVerify { mockDao.insertImages(any()) } // Verify sync happened
    }
    
    @Test
    fun `getImageForTest handles empty cache`() = runTest {
        // Given - cache has images but getLeastUsedImages returns empty
        coEvery { mockDao.getTotalImageCount() } returns 10
        coEvery { mockDao.getLeastUsedImages(1) } returns emptyList()
        
        // When
        val result = cacheManager.getImageForTest()
        
        // Then
        assertTrue("Should fail", result.isFailure)
        assertTrue(
            "Should mention no images",
            result.exceptionOrNull()?.message?.contains("No images") == true
        )
    }
    
    // ==================== Get Multiple Images ====================
    
    @Test
    fun `getImagesForTest returns correct count`() = runTest {
        // Given
        coEvery { mockDao.getTotalImageCount() } returns 20
        coEvery { mockDao.getLeastUsedImages(3) } returns listOf(
            createMockCachedImage("ppdt_001"),
            createMockCachedImage("ppdt_002"),
            createMockCachedImage("ppdt_003")
        )
        coEvery { mockDao.markImagesAsUsed(any()) } just Runs
        
        // When
        val result = cacheManager.getImagesForTest(count = 3)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        val questions = result.getOrNull()!!
        assertEquals("Should return 3 images", 3, questions.size)
        
        coVerify { mockDao.markImagesAsUsed(listOf("ppdt_001", "ppdt_002", "ppdt_003")) }
    }
    
    // ==================== Cache Status ====================
    
    @Test
    fun `getCacheStatus returns correct statistics`() = runTest {
        // Given
        coEvery { mockDao.getTotalImageCount() } returns 25
        coEvery { mockDao.getDownloadedImageCount() } returns 10
        coEvery { mockDao.getTotalBatchCount() } returns 2
        coEvery { mockDao.getAllBatchMetadata() } returns listOf(
            PPDTBatchMetadataEntity(
                batchId = "batch_001",
                downloadedAt = 1000000L,
                imageCount = 15,
                version = "1.0.0"
            ),
            PPDTBatchMetadataEntity(
                batchId = "batch_002",
                downloadedAt = 2000000L,
                imageCount = 10,
                version = "1.0.0"
            )
        )
        
        // When
        val status = cacheManager.getCacheStatus()
        
        // Then
        assertEquals("Should have 25 cached images", 25, status.cachedImages)
        assertEquals("Should have 10 downloaded images", 10, status.downloadedImages)
        assertEquals("Should have 2 batches", 2, status.batchesDownloaded)
        assertEquals("Last sync should be most recent", 2000000L, status.lastSyncTime)
    }
    
    @Test
    fun `getCacheStatus handles empty cache`() = runTest {
        // Given - empty cache
        coEvery { mockDao.getTotalImageCount() } returns 0
        coEvery { mockDao.getDownloadedImageCount() } returns 0
        coEvery { mockDao.getTotalBatchCount() } returns 0
        coEvery { mockDao.getAllBatchMetadata() } returns emptyList()
        
        // When
        val status = cacheManager.getCacheStatus()
        
        // Then
        assertEquals("Should have 0 cached images", 0, status.cachedImages)
        assertNull("Should have null last sync time", status.lastSyncTime)
    }
    
    // ==================== Clear Cache ====================
    
    @Test
    fun `clearCache removes all images`() = runTest {
        // Given
        coEvery { mockDao.clearAllImages() } just Runs
        
        // When
        cacheManager.clearCache()
        
        // Then
        coVerify { mockDao.clearAllImages() }
    }
    
    @Test
    fun `clearCache handles errors gracefully`() = runTest {
        // Given - DAO throws exception
        coEvery { mockDao.clearAllImages() } throws Exception("Database error")
        
        // When & Then - should not throw, just log
        cacheManager.clearCache()
        
        // Verify error was logged (via mockkStatic Log)
        verify { Log.e("PPDTCacheManager", "Failed to clear cache", any()) }
    }
    
    // ==================== Helper Methods ====================
    
    private fun createMockImageMap(
        id: String,
        imageUrl: String,
        viewingTime: Long = 30L,
        writingTime: Long = 4L
    ): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "imageUrl" to imageUrl,
            "imageDescription" to "A hazy picture showing an ambiguous scene",
            "viewingTimeSeconds" to viewingTime,
            "writingTimeMinutes" to writingTime,
            "minCharacters" to 200L,
            "maxCharacters" to 1000L,
            "category" to "leadership",
            "difficulty" to "medium"
        )
    }
    
    private fun createMockCachedImage(
        id: String,
        usageCount: Int = 0
    ): CachedPPDTImageEntity {
        return CachedPPDTImageEntity(
            id = id,
            imageUrl = "https://example.com/$id.jpg",
            localFilePath = null,
            imageDescription = "Test PPDT image",
            viewingTimeSeconds = 30,
            writingTimeMinutes = 4,
            minCharacters = 200,
            maxCharacters = 1000,
            category = "leadership",
            difficulty = "medium",
            batchId = "batch_001",
            cachedAt = System.currentTimeMillis(),
            lastUsed = if (usageCount > 0) System.currentTimeMillis() else null,
            usageCount = usageCount,
            imageDownloaded = false
        )
    }
}

