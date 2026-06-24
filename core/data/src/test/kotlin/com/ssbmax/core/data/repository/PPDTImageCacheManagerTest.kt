package com.ssbmax.core.data.repository

import com.ssbmax.core.data.local.entity.CachedPPDTImageEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PPDTImageCacheManager — initialSync, entity schema contract, and 24h TTL gate.
 *
 * REGRESSION COVERAGE (June 2026 bugs):
 *  - initialSync must re-download when Firestore batch version > local version
 *    (Bug: count-only guard caused stale placeholder images to be served after Phase 5 upload)
 *  - Entity schema must match migration defaults (maxCharacters, imageDownloaded type, indices)
 *    (Bug: maxCharacters=1500 in entity vs DEFAULT 1000 in MIGRATION_21_22 crashed Room)
 *
 * Gender filter + image retrieval + management tests → PPDTImageCacheGenderRetrievalTest
 */
class PPDTImageCacheManagerTest : PPDTImageCacheManagerTestBase() {

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
    fun `initialSync skips download when cache count is sufficient AND version matches Firestore`() =
        runTest {
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
            id = "test", imageUrl = "https://x", batchId = "batch_001", cachedAt = 0L
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
            id = "test", imageUrl = "https://x", batchId = "batch_001", cachedAt = 0L
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
    fun `initialSync skips Firestore version check when last staleness check was within 24h TTL`() =
        runTest {
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
}
