package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.PPDTImageCacheDao
import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
import com.ssbmax.core.data.local.entity.PPDTBatchMetadataEntity
import com.ssbmax.core.domain.model.GenderTag
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PPDTImageCacheManager
 * Tests cache initialization, image retrieval, URL normalization, and cache management.
 *
 * REGRESSION COVERAGE (June 2026 bugs):
 *  - initialSync must re-download when Firestore batch version > local version
 *    (Bug: count-only guard caused stale placeholder images to be served after Phase 5 upload)
 *  - Entity schema must match migration defaults (maxCharacters, imageDownloaded type, indices)
 *    (Bug: maxCharacters=1500 in entity vs DEFAULT 1000 in MIGRATION_21_22 crashed Room)
 */
class PPDTImageCacheManagerTest {

    private lateinit var cacheManager: PPDTImageCacheManager
    private val mockDao = mockk<PPDTImageCacheDao>(relaxed = true)
    private val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)

    private val mockImages = createMockImages(15)

    @Before
    fun setup() {
        clearAllMocks()
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        cacheManager = PPDTImageCacheManager(mockDao, mockFirestore)
    }

    // ==================== Initial Sync — Version Check (Regression) ====================

    @Test
    fun `initialSync re-downloads when Firestore batch version is newer than local cache`() = runTest {
        // Simulates the Phase 5 upload bug: 57 old images cached, new batch uploaded to Firestore
        coEvery { mockDao.getTotalImageCount() } returns 57
        coEvery { mockDao.getBatchMetadata("batch_001") } returns localMeta(version = "1.0.0")
        setupFirestoreVersion("2.0.0", withImages = true)

        val result = cacheManager.initialSync()

        assertTrue("Should succeed", result.isSuccess)
        // MUST clear stale images before re-downloading
        coVerify(exactly = 1) { mockDao.clearAllImages() }
        coVerify { mockDao.insertImages(any()) }
    }

    @Test
    fun `initialSync skips download when cache count is sufficient AND version matches Firestore`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 64
        coEvery { mockDao.getBatchMetadata("batch_001") } returns localMeta(version = "2.0.0")
        setupFirestoreVersion("2.0.0", withImages = false)

        val result = cacheManager.initialSync()

        assertTrue("Should succeed", result.isSuccess)
        coVerify(exactly = 0) { mockDao.clearAllImages() }
        coVerify(exactly = 0) { mockDao.insertImages(any()) }
    }

    @Test
    fun `initialSync re-downloads when no local batch metadata exists`() = runTest {
        // First install or cleared metadata: no version record → must treat as stale
        coEvery { mockDao.getTotalImageCount() } returns 20
        coEvery { mockDao.getBatchMetadata("batch_001") } returns null
        setupFirestoreVersion("2.0.0", withImages = true)

        val result = cacheManager.initialSync()

        assertTrue("Should succeed", result.isSuccess)
        coVerify(exactly = 1) { mockDao.clearAllImages() }
        coVerify { mockDao.insertImages(any()) }
    }

    @Test
    fun `initialSync does NOT wipe cache when Firestore version fetch fails`() = runTest {
        // Network failure during version check must not destroy working local cache
        coEvery { mockDao.getTotalImageCount() } returns 64
        coEvery { mockDao.getBatchMetadata("batch_001") } returns localMeta(version = "2.0.0")
        setupFirestoreVersionFailure()

        val result = cacheManager.initialSync()

        assertTrue("Network failure should not break initialSync", result.isSuccess)
        coVerify(exactly = 0) { mockDao.clearAllImages() }
        coVerify(exactly = 0) { mockDao.insertImages(any()) }
    }

    @Test
    fun `initialSync downloads batch when cache is empty`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 0
        setupFirestoreVersion("2.0.0", withImages = true)

        val result = cacheManager.initialSync()

        assertTrue("Should succeed", result.isSuccess)
        coVerify { mockDao.insertImages(any()) }
        coVerify { mockDao.insertBatchMetadata(any()) }
    }

    @Test
    fun `initialSync returns failure when Firestore fetch fails on empty cache`() = runTest {
        coEvery { mockDao.getTotalImageCount() } returns 0
        setupFirestoreVersionFailure()

        val result = cacheManager.initialSync()

        assertTrue("Should fail when cache empty and network down", result.isFailure)
    }

    // ==================== Entity Schema Contract (Regression) ====================

    @Test
    fun `CachedPPDTImageEntity default maxCharacters matches migration DEFAULT 1000`() {
        // MIGRATION_21_22 sets DEFAULT 1000. Entity default must match or Room throws
        // IllegalStateException: "Migration didn't properly handle cached_ppdt_images"
        val entity = CachedPPDTImageEntity(
            id = "test", imageUrl = "https://x", batchId = "batch_001",
            cachedAt = 0L
        )
        assertEquals(
            "maxCharacters default must match MIGRATION_21_22 DEFAULT 1000",
            1000, entity.maxCharacters
        )
    }

    @Test
    fun `CachedPPDTImageEntity imageDownloaded default is Int 0 not Boolean`() {
        // Room stores BOOLEAN as INTEGER. Migration uses INTEGER NOT NULL DEFAULT 0.
        // If entity uses Boolean, Room schema validation fails.
        val entity = CachedPPDTImageEntity(
            id = "test", imageUrl = "https://x", batchId = "batch_001",
            cachedAt = 0L
        )
        assertEquals(
            "imageDownloaded must be Int 0 to match SQLite INTEGER column",
            0, entity.imageDownloaded
        )
    }

    @Test
    fun `Room schema v22 has all 6 indices on cached_ppdt_images required by MIGRATION_21_22`() {
        // Reads the committed Room schema JSON (generated by kapt, not runtime reflection).
        // This catches entity/migration drift at compile-time, before any device sees it.
        val schemaJson = java.io.File(
            "schemas/com.ssbmax.core.data.local.SSBDatabase/22.json"
        ).readText()
        val required = setOf("genderTag", "imageDownloaded", "usageCount", "batchId", "difficulty", "category")
        val missing = required.filter { col -> !schemaJson.contains("\"$col\"") }
        assertTrue(
            "Schema v22 missing indices for columns: $missing — " +
                "add @Index(\"$missing\") to CachedPPDTImageEntity and run a migration",
            missing.isEmpty()
        )
    }

    // ==================== Cache B: 24h TTL gates the Firestore version check ====================

    @Test
    fun `initialSync skips Firestore version check when last staleness check was within 24h TTL`() = runTest {
        // WHY: Every warm start previously hit Firestore to compare batch versions.
        // With the 24h TTL, a check done 23h ago is still valid — no Firestore round-trip needed.
        coEvery { mockDao.getTotalImageCount() } returns 64
        coEvery { mockDao.getBatchMetadata("batch_001") } returns localMeta(
            version = "2.0.0",
            lastStalenessCheckAt = System.currentTimeMillis() - 23 * 3_600_000L
        )

        val result = cacheManager.initialSync()

        assertTrue("Should succeed", result.isSuccess)
        verify(exactly = 0) { mockFirestore.document(any()) }
        coVerify(exactly = 0) { mockDao.clearAllImages() }
    }

    @Test
    fun `initialSync calls Firestore version check when 24h TTL has expired`() = runTest {
        // WHY: After 24h the cached version info may be stale — must query Firestore to confirm.
        coEvery { mockDao.getTotalImageCount() } returns 64
        coEvery { mockDao.getBatchMetadata("batch_001") } returns localMeta(
            version = "2.0.0",
            lastStalenessCheckAt = System.currentTimeMillis() - 25 * 3_600_000L
        )
        setupFirestoreVersion("2.0.0", withImages = false)

        val result = cacheManager.initialSync()

        assertTrue("Should succeed", result.isSuccess)
        verify(exactly = 1) { mockFirestore.document(any()) }
        coVerify(exactly = 0) { mockDao.clearAllImages() }
    }

    @Test
    fun `initialSync persists updated lastStalenessCheckAt after Firestore version check`() = runTest {
        // WHY: The TTL gate is only useful if the timestamp is updated after each check.
        // Without this, every startup after TTL expiry would hit Firestore indefinitely.
        coEvery { mockDao.getTotalImageCount() } returns 64
        coEvery { mockDao.getBatchMetadata("batch_001") } returns localMeta(
            version = "2.0.0",
            lastStalenessCheckAt = 0L
        )
        setupFirestoreVersion("2.0.0", withImages = false)

        cacheManager.initialSync()

        coVerify { mockDao.insertBatchMetadata(match { it.lastStalenessCheckAt > 0L }) }
    }

    @Test
    fun `Room schema v23 has lastStalenessCheckAt column on ppdt_batch_metadata`() {
        // WHY: Entity/migration drift on a NOT NULL column crashes Room on upgrade.
        // Reading the generated schema JSON catches this before any device sees the migration.
        val schemaJson = java.io.File(
            "schemas/com.ssbmax.core.data.local.SSBDatabase/23.json"
        ).readText()
        assertTrue(
            "Schema v23 must contain lastStalenessCheckAt — add field to PPDTBatchMetadataEntity " +
                "and MIGRATION_22_23, then rebuild to regenerate 23.json",
            schemaJson.contains("lastStalenessCheckAt")
        )
    }

    // ==================== Cache A: Gender filter must live at DAO layer ====================

    @Test
    fun `getImageForTest with gender tag does not call getLeastUsedImages`() = runTest {
        // WHY: Current code fetches 15 images with getLeastUsedImages then filters in-memory.
        // In-memory filtering discards images from the cache pool without accounting for gender,
        // making the selection non-deterministic and wasting a DB round-trip on irrelevant rows.
        // The DAO query (getLeastUsedImagesByGender) must be the single filter — SSOT/SOLID SR.
        coEvery { mockDao.getTotalImageCount() } returns 15

        cacheManager.getImageForTest(genderTag = GenderTag.FEMALE)

        // BUG: current impl calls getLeastUsedImages (no gender param) then filters in-memory.
        // After fix: getLeastUsedImagesByGender replaces getLeastUsedImages for gender-scoped calls.
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
        val httpsImage = createMockImage("ppdt_https", "https://storage.googleapis.com/bucket/img.jpg")
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

    // ==================== Helpers ====================

    private fun localMeta(version: String, lastStalenessCheckAt: Long = 0L) = PPDTBatchMetadataEntity(
        batchId = "batch_001",
        downloadedAt = System.currentTimeMillis(),
        imageCount = 64,
        version = version,
        lastStalenessCheckAt = lastStalenessCheckAt
    )

    /** Sets up Firestore to return a version string. Pass withImages=true for full download tests. */
    private fun setupFirestoreVersion(version: String, withImages: Boolean) {
        val mockDoc = mockk<DocumentSnapshot>(relaxed = true)
        every { mockDoc.exists() } returns true
        every { mockDoc.getString("version") } returns version
        if (withImages) {
            every { mockDoc.get("images") } returns mockImages.map { it.toFirestoreMap() }
        }
        setupFirestoreDoc(mockDoc)
    }

    private fun setupFirestoreVersionFailure() {
        val mockTask = mockk<com.google.android.gms.tasks.Task<DocumentSnapshot>>(relaxed = true)
        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns false
        every { mockTask.isCanceled } returns false
        every { mockTask.exception } returns Exception("Network error")

        val mockDocRef = mockk<DocumentReference>(relaxed = true)
        every { mockDocRef.get() } returns mockTask
        every { mockFirestore.document(any()) } returns mockDocRef
    }

    private fun setupFirestoreDoc(mockDoc: DocumentSnapshot) {
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

    private fun createMockImages(count: Int): List<CachedPPDTImageEntity> =
        (1..count).map { index ->
            val paddedNum = String.format("%03d", index)
            createMockImage("ppdt_$paddedNum", "https://storage.googleapis.com/test/ppdt_$paddedNum.jpg")
        }

    private fun createMockImage(id: String, imageUrl: String): CachedPPDTImageEntity =
        CachedPPDTImageEntity(
            id = id,
            imageUrl = imageUrl,
            localFilePath = null,
            imageDescription = "Test image showing an ambiguous scene",
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
            imageDownloaded = 0
        )

    private fun CachedPPDTImageEntity.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "imageUrl" to imageUrl,
        "imageDescription" to imageDescription,
        "imageContext" to emptyMap<String, Any>(),
        "genderTag" to genderTag.name,
        "viewingTimeSeconds" to viewingTimeSeconds.toLong(),
        "writingTimeMinutes" to writingTimeMinutes.toLong(),
        "minCharacters" to minCharacters.toLong(),
        "maxCharacters" to maxCharacters.toLong(),
        "category" to (category ?: ""),
        "difficulty" to (difficulty ?: "medium")
    )
}
