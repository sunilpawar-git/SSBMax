package com.ssbmax.core.data.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.TATImageCacheDao
import com.ssbmax.core.data.local.entity.CachedTATImageEntity
import com.ssbmax.core.data.local.entity.TATBatchMetadataEntity
import com.ssbmax.shared.domain.model.GenderTag
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TATImageCacheManager — Phase 2 pool-aware architecture.
 * Verifies: initialSync (empty / stale / fresh), getImagesForTest (per position),
 * blank card insertion, usage tracking, and 24h TTL staleness gate.
 */
class TATImageCacheManagerTest {

    private lateinit var cacheManager: TATImageCacheManager
    private val mockDao = mockk<TATImageCacheDao>(relaxed = true)
    private val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)

    @Before
    fun setup() {
        cacheManager = TATImageCacheManager(mockDao, mockFirestore)
        clearAllMocks()
    }

    // ==================== initialSync ====================

    @Test
    fun `initialSync downloads batch_001 when cache is empty`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 0
        mockFirestoreSuccess()

        val result = cacheManager.initialSync()

        assertTrue(result.isSuccess)
        coVerify { mockDao.insertImages(any()) }
        coVerify { mockDao.insertBatchMetadata(any()) }
    }

    @Test
    fun `initialSync skips download when cache is current and TTL not expired`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 167
        coEvery { mockDao.getBatchMetadata("batch_001") } returns freshMeta()

        val result = cacheManager.initialSync()

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { mockDao.insertImages(any()) }
    }

    @Test
    fun `initialSync clears and re-downloads when Firestore version is newer`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 167
        // lastStalenessCheckAt = 0 → TTL expired → will hit Firestore
        coEvery { mockDao.getBatchMetadata("batch_001") } returns staleMeta(localVersion = "1.0.0")
        mockFirestoreSuccess(version = "1.0.1")

        val result = cacheManager.initialSync()

        assertTrue(result.isSuccess)
        coVerify { mockDao.clearAllImages() }
        coVerify { mockDao.insertImages(any()) }
    }

    @Test
    fun `initialSync returns failure when Firestore fails on empty cache`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 0
        mockFirestoreFailure()

        val result = cacheManager.initialSync()

        assertTrue(result.isFailure)
    }

    // ==================== getImagesForTest ====================

    @Test
    fun `getImagesForTest returns 12 questions (11 real + blank card)`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 167
        coEvery { mockDao.getBatchMetadata("batch_001") } returns freshMeta()
        // Return one entity per position
        for (pos in 1..11) {
            coEvery { mockDao.getLeastUsedImageByPosition(pos, any()) } returns entity("tat_00${pos}_mixed", pos)
        }

        val result = cacheManager.getImagesForTest(GenderTag.MALE)

        assertTrue(result.isSuccess)
        val questions = result.getOrNull()!!
        assertEquals(12, questions.size)
        assertEquals(12, questions.last().cardPosition) // blank card is last
        assertEquals("blank_card", questions.last().id)
    }

    @Test
    fun `getImagesForTest marks all 11 real images as used (not blank card)`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 167
        coEvery { mockDao.getBatchMetadata("batch_001") } returns freshMeta()
        for (pos in 1..11) {
            coEvery { mockDao.getLeastUsedImageByPosition(pos, any()) } returns entity("img_$pos", pos)
        }

        cacheManager.getImagesForTest(GenderTag.MIXED)

        coVerify {
            mockDao.markImagesAsUsed(
                match { ids -> ids.size == 11 && ids.none { it == "blank_card" } },
                any()
            )
        }
    }

    @Test
    fun `getImagesForTest uses MIXED fallback when gender position is null`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 167
        coEvery { mockDao.getBatchMetadata("batch_001") } returns freshMeta()
        // Position 3 has no FEMALE image but has MIXED
        for (pos in 1..11) {
            if (pos == 3) {
                coEvery { mockDao.getLeastUsedImageByPosition(pos, "FEMALE") } returns null
                coEvery { mockDao.getLeastUsedImageByPosition(pos, "MIXED") } returns entity("tat_003_mixed", pos, "MIXED")
            } else {
                coEvery { mockDao.getLeastUsedImageByPosition(pos, any()) } returns entity("tat_00${pos}_female", pos, "FEMALE")
            }
        }

        val result = cacheManager.getImagesForTest(GenderTag.FEMALE)

        assertTrue(result.isSuccess)
        val q3 = result.getOrNull()!!.first { it.cardPosition == 3 }
        assertEquals("tat_003_mixed", q3.id)
    }

    @Test
    fun `getImagesForTest fails when a position has no image at all`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 40
        coEvery { mockDao.getBatchMetadata("batch_001") } returns freshMeta()
        for (pos in 1..11) {
            coEvery { mockDao.getLeastUsedImageByPosition(pos, any()) } returns
                if (pos == 7) null else entity("img_$pos", pos)
        }

        val result = cacheManager.getImagesForTest(null)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getImagesForTest triggers initialSync when cache is below minimum`() = runTest {
        // Count stays 0 on both calls (getImagesForTest + initialSync) so initialSync
        // falls straight through to downloadBatch without a staleness check.
        coEvery { mockDao.getTotalImageCount() } returns 0
        mockFirestoreSuccess()
        for (pos in 1..11) {
            coEvery { mockDao.getLeastUsedImageByPosition(pos, any()) } returns entity("img_$pos", pos)
        }

        val result = cacheManager.getImagesForTest(GenderTag.MIXED)

        assertTrue(result.isSuccess)
        coVerify { mockDao.insertImages(any()) }
    }

    // ==================== getCacheStatus ====================

    @Test
    fun `getCacheStatus returns accurate statistics`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 167
        coEvery { mockDao.getTotalBatchCount() } returns 1
        coEvery { mockDao.getAllBatchMetadata() } returns listOf(freshMeta())

        val status = cacheManager.getCacheStatus()

        assertEquals(167, status.cachedImages)
        assertEquals(1, status.batchesDownloaded)
        assertNotNull(status.lastSyncTime)
    }

    @Test
    fun `getCacheStatus handles empty cache`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 0
        coEvery { mockDao.getTotalBatchCount() } returns 0
        coEvery { mockDao.getAllBatchMetadata() } returns emptyList()

        val status = cacheManager.getCacheStatus()

        assertEquals(0, status.cachedImages)
        assertNull(status.lastSyncTime)
    }

    // ==================== clearCache ====================

    @Test
    fun `clearCache removes all images`() = runTest {
        cacheManager.clearCache()
        coVerify { mockDao.clearAllImages() }
    }

    // ==================== Helpers ====================

    private fun entity(
        id: String,
        pos: Int,
        gender: String = "MIXED"
    ) = CachedTATImageEntity(
        id = id,
        imageUrl = "https://storage.googleapis.com/test/$id.jpg",
        cardPosition = pos,
        genderTag = gender,
        imageContextJson = "{}",
        viewingTimeSeconds = 30,
        writingTimeMinutes = 4,
        minCharacters = 150,
        maxCharacters = 1500,
        category = null,
        difficulty = "medium",
        batchId = "batch_001",
        cachedAt = System.currentTimeMillis(),
        lastUsed = null,
        usageCount = 0
    )

    private fun freshMeta(version: String = "1.0.0") = TATBatchMetadataEntity(
        batchId = "batch_001",
        downloadedAt = System.currentTimeMillis(),
        imageCount = 167,
        version = version,
        lastStalenessCheckAt = System.currentTimeMillis() // checked just now → TTL not expired
    )

    private fun staleMeta(localVersion: String = "1.0.0") = TATBatchMetadataEntity(
        batchId = "batch_001",
        downloadedAt = System.currentTimeMillis() - 48 * 3_600_000L,
        imageCount = 167,
        version = localVersion,
        lastStalenessCheckAt = 0L // never checked → TTL expired
    )

    /** Stubs Firestore to return a successful document with 11 position-1..11 images. */
    private fun mockFirestoreSuccess(version: String = "1.0.0") {
        val images = (1..11).map { pos ->
            mapOf(
                "id" to "tat_00${pos}_mixed",
                "imageUrl" to "https://storage.googleapis.com/test/tat_00${pos}_mixed.jpg",
                "cardPosition" to pos.toLong(),
                "genderTag" to "MIXED",
                "viewingTimeSeconds" to 30L,
                "writingTimeMinutes" to 4L,
                "minCharacters" to 150L,
                "maxCharacters" to 1500L
            )
        }

        val mockDoc = mockk<DocumentSnapshot>(relaxed = true)
        every { mockDoc.exists() } returns true
        every { mockDoc.get("images") } returns images
        every { mockDoc.getString("version") } returns version

        val mockTask = mockk<com.google.android.gms.tasks.Task<DocumentSnapshot>>(relaxed = true)
        every { mockTask.result } returns mockDoc
        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns true
        every { mockTask.isCanceled } returns false
        every { mockTask.exception } returns null

        val mockDocRef = mockk<DocumentReference>(relaxed = true)
        every { mockDocRef.get() } returns mockTask
        every { mockFirestore.document(any()) } returns mockDocRef
    }

    private fun mockFirestoreFailure() {
        val mockTask = mockk<com.google.android.gms.tasks.Task<DocumentSnapshot>>(relaxed = true)
        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns false
        every { mockTask.isCanceled } returns false
        every { mockTask.exception } returns Exception("Network error")

        val mockDocRef = mockk<DocumentReference>(relaxed = true)
        every { mockDocRef.get() } returns mockTask
        every { mockFirestore.document(any()) } returns mockDocRef
    }
}
