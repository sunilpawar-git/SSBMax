package com.ssbmax.core.data.repository

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.core.data.local.entity.OIRBatchMetadataEntity
import com.ssbmax.shared.domain.model.OIRQuestionType
import com.ssbmax.shared.domain.model.QuestionDifficulty
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import java.util.concurrent.TimeUnit

/**
 * Integration tests for OIRQuestionCacheManager
 * 
 * Tests the complete caching workflow:
 * - Firestore batch downloads
 * - Question distribution (40/40/15/5 ratio)
 * - Usage tracking
 * - Cache rotation
 * - Error handling
 * 
 * NOTE: These tests currently require Firestore emulator for full integration testing.
 * They are temporarily ignored pending emulator setup or conversion to instrumented tests.
 * The cache manager logic is validated via ViewModel tests and E2E tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OIRCacheManagerIntegrationTest {

    @get:Rule
    val timeout: Timeout = Timeout(60, TimeUnit.SECONDS)

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
    
    private lateinit var cacheManager: OIRQuestionCacheManager
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockCacheDao: OIRQuestionCacheDao
    private val gson = Gson()
    private val testScope = TestScope()

    @Before
    fun setup() {
        mockFirestore = mockk(relaxed = true)
        mockCacheDao = mockk(relaxed = true)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0
        
        val selector = OIRQuestionSelector(cacheDao = mockCacheDao, gson = gson)
        cacheManager = OIRQuestionCacheManager(
            firestore = mockFirestore,
            cacheDao  = mockCacheDao,
            gson      = gson,
            selector  = selector,
            backgroundScope = testScope
        )
        cacheManager = spyk(cacheManager)
        coEvery { cacheManager.downloadBatch(any()) } returns Result.success(Unit)
        // Default meta config (legacy fallback: no version, 20 batches). Tests that exercise
        // reconciliation override this. Stubbing on the spyk keeps initialSync off real Firestore.
        coEvery { cacheManager.fetchMetaConfig() } returns
                OIRQuestionCacheManager.MetaConfig(contentVersion = null, batchCount = 20)
        coEvery { mockCacheDao.getSyncMetadata() } returns null
        coEvery { mockCacheDao.getUnusedQuestionsByType(any(), any(), any()) } returns emptyList()
        coEvery { mockCacheDao.getQuestionsByType(any(), any()) } returns emptyList()
        coEvery { mockCacheDao.getQuestionCountByType(any()) } returns 0
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
    }
    
    // ==================== Question Distribution Tests ====================
    
    @Test
    fun `getTestQuestions returns correct distribution of question types`() = runTest {
        // Given - 100 cached questions with proper distribution
        val verbalQuestions = (1..20).map { createMockCachedQuestion("v$it", OIRQuestionType.VERBAL_REASONING.name) }
        val nonVerbalQuestions = (1..20).map { createMockCachedQuestion("nv$it", OIRQuestionType.NON_VERBAL_REASONING.name) }
        val numericalQuestions = (1..10).map { createMockCachedQuestion("num$it", OIRQuestionType.NUMERICAL_ABILITY.name) }
        val spatialQuestions = emptyList<com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity>()
        
        coEvery { mockCacheDao.getCachedQuestionCount() } returns 100
        coEvery { mockCacheDao.getUnusedQuestionsByType(OIRQuestionType.VERBAL_REASONING.name, any(), any()) } returns verbalQuestions
        coEvery { mockCacheDao.getUnusedQuestionsByType(OIRQuestionType.NON_VERBAL_REASONING.name, any(), any()) } returns nonVerbalQuestions
        coEvery { mockCacheDao.getUnusedQuestionsByType(OIRQuestionType.NUMERICAL_ABILITY.name, any(), any()) } returns numericalQuestions
        coEvery { mockCacheDao.getUnusedQuestionsByType(OIRQuestionType.SPATIAL_REASONING.name, any(), any()) } returns spatialQuestions
        coEvery { mockCacheDao.getQuestionsByType(OIRQuestionType.VERBAL_REASONING.name, any()) } returns verbalQuestions
        coEvery { mockCacheDao.getQuestionsByType(OIRQuestionType.NON_VERBAL_REASONING.name, any()) } returns nonVerbalQuestions
        coEvery { mockCacheDao.getQuestionsByType(OIRQuestionType.NUMERICAL_ABILITY.name, any()) } returns numericalQuestions
        coEvery { mockCacheDao.getQuestionsByType(OIRQuestionType.SPATIAL_REASONING.name, any()) } returns spatialQuestions
        
        // When
        val result = cacheManager.getTestQuestions(count = 50)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        val questions = result.getOrNull()!!
        
        assertEquals("Should have 50 questions", 50, questions.size)
        
        // Verify distribution per OIRQuestionDistribution SSOT (V40/NV40/N20, no spatial)
        val verbalCount = questions.count { it.type == OIRQuestionType.VERBAL_REASONING }
        val nonVerbalCount = questions.count { it.type == OIRQuestionType.NON_VERBAL_REASONING }
        val numericalCount = questions.count { it.type == OIRQuestionType.NUMERICAL_ABILITY }
        val spatialCount = questions.count { it.type == OIRQuestionType.SPATIAL_REASONING }

        assertEquals("Verbal should be 40%", 20, verbalCount)
        assertEquals("Non-verbal should be 40%", 20, nonVerbalCount)
        assertEquals("Numerical should be 20%", 10, numericalCount)
        assertEquals("Spatial should be 0 (none in bank)", 0, spatialCount)
    }
    
    @Test
    fun `getTestQuestions avoids recently used questions`() = runTest {
        // Given - mock returns questions based on timestamp filter
        val freshQuestions = (1..20).map { createMockCachedQuestion("fresh$it", OIRQuestionType.VERBAL_REASONING.name, lastUsed = null) }
        
        coEvery { mockCacheDao.getCachedQuestionCount() } returns 100
        coEvery { mockCacheDao.getUnusedQuestionsByType(any(), any(), any()) } returns freshQuestions
        coEvery { mockCacheDao.getQuestionsByType(any(), any()) } returns freshQuestions
        
        // When
        cacheManager.getTestQuestions(count = 50)
        
        // Then - Verify cache manager queries with timestamp filter
        coVerify {
            mockCacheDao.getUnusedQuestionsByType(
                OIRQuestionType.VERBAL_REASONING.name,
                any(), // Timestamp threshold
                any()
            )
        }
    }
    
    // ==================== Initial Sync Tests ====================
    
    @Test
    fun `initialSync skips when version matches and all batches present`() = runTest {
        coEvery { cacheManager.fetchMetaConfig() } returns
                OIRQuestionCacheManager.MetaConfig(contentVersion = 2, batchCount = 20)
        coEvery { mockCacheDao.getSyncMetadata() } returns
                com.ssbmax.core.data.local.entity.OIRSyncMetadataEntity(contentVersion = 2, lastSyncAt = 1L)
        coEvery { mockCacheDao.isBatchDownloaded(any()) } returns true

        val result = cacheManager.initialSync()

        assertTrue("Should succeed without downloading", result.isSuccess)
        coVerify(exactly = 0) { cacheManager.downloadBatch(any()) }
    }

    @Test
    fun `initialSync reconciles (clear + redownload) when remote version differs`() = testScope.runTest {
        coEvery { cacheManager.fetchMetaConfig() } returns
                OIRQuestionCacheManager.MetaConfig(contentVersion = 3, batchCount = 28)
        coEvery { mockCacheDao.getSyncMetadata() } returns
                com.ssbmax.core.data.local.entity.OIRSyncMetadataEntity(contentVersion = 2, lastSyncAt = 1L)
        coEvery { mockCacheDao.isBatchDownloaded(any()) } returns true // present, but version stale

        cacheManager.initialSync()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockCacheDao.deleteAllQuestions() }
        coVerify(exactly = 1) { mockCacheDao.deleteAllBatchMetadata() }
        // Re-downloads across the full 1..28 range despite batches being "present".
        coVerify(atLeast = 1) { cacheManager.downloadBatch("batch_pdf_028") }
    }

    @Test
    fun `initialSync tops up missing batches when version matches`() = runTest {
        coEvery { cacheManager.fetchMetaConfig() } returns
                OIRQuestionCacheManager.MetaConfig(contentVersion = 2, batchCount = 20)
        coEvery { mockCacheDao.getSyncMetadata() } returns
                com.ssbmax.core.data.local.entity.OIRSyncMetadataEntity(contentVersion = 2, lastSyncAt = 1L)
        coEvery { mockCacheDao.isBatchDownloaded(any()) } returns false // some missing → top-up

        cacheManager.initialSync()

        coVerify(exactly = 0) { mockCacheDao.deleteAllQuestions() } // top-up, not reconcile
        coVerify(atLeast = 1) { cacheManager.downloadBatch(any()) }
    }

    @Test
    fun `initialSync downloads batches 1 to 4 synchronously before returning`() = runTest {
        coEvery { mockCacheDao.isBatchDownloaded(any()) } returns false // fresh install

        val result = cacheManager.initialSync()

        assertTrue("initialSync should succeed", result.isSuccess)
        (1..4).map { "batch_pdf_%03d".format(it) }.forEach { batchId ->
            coVerify(atLeast = 1) { cacheManager.downloadBatch(batchId) }
        }
    }

    @Test
    fun `initialSync enqueues background download up to batchCount`() = testScope.runTest {
        coEvery { cacheManager.fetchMetaConfig() } returns
                OIRQuestionCacheManager.MetaConfig(contentVersion = 2, batchCount = 28)
        coEvery { mockCacheDao.isBatchDownloaded(any()) } returns false

        cacheManager.initialSync()
        advanceUntilIdle()

        (5..28).map { "batch_pdf_%03d".format(it) }.forEach { batchId ->
            coVerify(atLeast = 1) { cacheManager.downloadBatch(batchId) }
        }
    }

    @Test
    fun `initialSync persists the remote content version`() = runTest {
        coEvery { cacheManager.fetchMetaConfig() } returns
                OIRQuestionCacheManager.MetaConfig(contentVersion = 2, batchCount = 20)
        coEvery { mockCacheDao.isBatchDownloaded(any()) } returns false

        cacheManager.initialSync()

        coVerify { mockCacheDao.upsertSyncMetadata(match { it.contentVersion == 2 }) }
    }

    // ==================== Usage Tracking Tests ====================
    
    @Test
    fun `markQuestionsUsed updates timestamp and usage count`() = runTest {
        // Given
        val questionIds = listOf("q1", "q2", "q3")
        coEvery { mockCacheDao.markQuestionsUsed(any(), any()) } just Runs
        
        // When
        cacheManager.markQuestionsUsed(questionIds)
        
        // Then
        coVerify {
            mockCacheDao.markQuestionsUsed(
                questionIds,
                match { it > 0 } // Timestamp should be positive
            )
        }
    }

    @Test
    fun `afterMarkUsed getTestQuestions queries with 7-day threshold excluding recently used`() = runTest {
        // Phase 2 RED: verify that after marking questions used, subsequent
        // getTestQuestions calls query the cache with a recency threshold
        val usedIds = (1..50).map { "q$it" }
        coEvery { mockCacheDao.markQuestionsUsed(any(), any()) } just Runs

        cacheManager.markQuestionsUsed(usedIds)

        // Now call getTestQuestions — it must use getUnusedQuestionsByType (timestamp-filtered)
        val fresh = (1..20).map { createMockCachedQuestion("fresh$it", OIRQuestionType.VERBAL_REASONING.name, lastUsed = null) }
        coEvery { mockCacheDao.getCachedQuestionCount() } returns 200
        coEvery { mockCacheDao.getUnusedQuestionsByType(any(), any(), any()) } returns fresh
        coEvery { mockCacheDao.getQuestionsByType(any(), any()) } returns fresh

        cacheManager.getTestQuestions(count = 50)

        coVerify {
            mockCacheDao.getUnusedQuestionsByType(
                any(),
                match { threshold -> threshold < System.currentTimeMillis() - (6 * 24 * 60 * 60 * 1000L) },
                any()
            )
        }
    }
    
    // ==================== Cache Status Tests ====================
    
    @Test
    fun `getCacheStatus returns accurate statistics`() = runTest {
        // Given
        val batches = listOf(
            OIRBatchMetadataEntity(
                batchId = "batch_001",
                downloadedAt = System.currentTimeMillis(),
                questionCount = 100,
                version = "1.0"
            )
        )
        
        val questions = listOf(
            *((1..45).map { createMockCachedQuestion("v$it", OIRQuestionType.VERBAL_REASONING.name) }).toTypedArray(),
            *((1..35).map { createMockCachedQuestion("nv$it", OIRQuestionType.NON_VERBAL_REASONING.name) }).toTypedArray(),
            *((1..15).map { createMockCachedQuestion("num$it", OIRQuestionType.NUMERICAL_ABILITY.name) }).toTypedArray(),
            *((1..5).map { createMockCachedQuestion("sp$it", OIRQuestionType.SPATIAL_REASONING.name) }).toTypedArray()
        )
        
        coEvery { mockCacheDao.getAllBatchMetadata() } returns batches
        coEvery { mockCacheDao.getCachedQuestionCount() } returns 100
        coEvery { mockCacheDao.getAllQuestions() } returns questions
        coEvery { mockCacheDao.getQuestionCountByType(OIRQuestionType.VERBAL_REASONING.name) } returns 45
        coEvery { mockCacheDao.getQuestionCountByType(OIRQuestionType.NON_VERBAL_REASONING.name) } returns 35
        coEvery { mockCacheDao.getQuestionCountByType(OIRQuestionType.NUMERICAL_ABILITY.name) } returns 15
        coEvery { mockCacheDao.getQuestionCountByType(OIRQuestionType.SPATIAL_REASONING.name) } returns 5
        
        // When
        val status = cacheManager.getCacheStatus()
        
        // Then
        assertEquals("Should have 100 questions", 100, status.cachedQuestions)
        assertEquals("Should have 1 batch", 1, status.batchesDownloaded)
        assertEquals("Verbal count", 45, status.verbalCount)
        assertEquals("Non-verbal count", 35, status.nonVerbalCount)
        assertEquals("Numerical count", 15, status.numericalCount)
        assertEquals("Spatial count", 5, status.spatialCount)
        assertNotNull("Should have last sync time", status.lastSyncTime)
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    fun `getTestQuestions handles insufficient cache gracefully`() = runTest {
        // Given - cache has only 10 questions but need 50
        coEvery { mockCacheDao.getCachedQuestionCount() } returnsMany listOf(10, 10) // Still 10 after sync attempt
        coEvery { mockCacheDao.getUnusedQuestionsByType(any(), any(), any()) } returns emptyList()
        
        // When
        val result = cacheManager.getTestQuestions(count = 50)
        
        // Then - should attempt to trigger sync
        coVerify(atLeast = 1) { mockCacheDao.getCachedQuestionCount() }
    }
    
    @Test
    fun `getCacheStatus handles empty cache`() = runTest {
        // Given - empty cache
        coEvery { mockCacheDao.getAllBatchMetadata() } returns emptyList()
        coEvery { mockCacheDao.getCachedQuestionCount() } returns 0
        coEvery { mockCacheDao.getAllQuestions() } returns emptyList()
        
        // When
        val status = cacheManager.getCacheStatus()
        
        // Then
        assertEquals("Should show 0 questions", 0, status.cachedQuestions)
        assertEquals("Should show 0 batches", 0, status.batchesDownloaded)
        assertNull("Should have no sync time", status.lastSyncTime)
    }
    
    // ==================== Helper Methods ====================

    // ==================== Phase 4-B RED: totalQuestions field name fix ====================

    private fun buildMockFirestoreChainForDownloadBatch(docData: Map<String, Any?>): DocumentSnapshot {
        val mockDoc = mockk<DocumentSnapshot>(relaxed = true)
        val mockBatchesCollection = mockk<CollectionReference>(relaxed = true)
        val mockBatchDoc = mockk<DocumentReference>(relaxed = true)
        val mockOirDocRef = mockk<DocumentReference>(relaxed = true)
        val mockOirCollection = mockk<CollectionReference>(relaxed = true)

        every { mockFirestore.collection(any()) } returns mockOirCollection
        every { mockOirCollection.document(any()) } returns mockOirDocRef
        every { mockOirDocRef.collection(any()) } returns mockBatchesCollection
        every { mockBatchesCollection.document(any()) } returns mockBatchDoc
        every { mockBatchDoc.get() } returns Tasks.forResult(mockDoc)
        every { mockDoc.exists() } returns true
        every { mockDoc.data } returns docData
        return mockDoc
    }

    @Test
    fun `downloadBatch withTotalQuestionsField usesCorrectCount`() = runTest {
        // Arrange — batch doc uses new field name "totalQuestions"
        val docData = mapOf<String, Any?>(
            "totalQuestions" to 50L,
            "version" to "1.0.0",
            "questions" to listOf(
                mapOf(
                    "id" to "q1", "questionNumber" to 1L, "type" to "VERBAL_REASONING",
                    "questionText" to "Q1", "options" to emptyList<Any>(),
                    "correctAnswerId" to "A", "explanation" to "E",
                    "difficulty" to "EASY", "tags" to listOf<String>()
                )
            )
        )
        buildMockFirestoreChainForDownloadBatch(docData)
        coEvery { mockCacheDao.isBatchDownloaded(any()) } returns false
        coEvery { mockCacheDao.insertQuestions(any()) } just Runs
        coEvery { mockCacheDao.insertBatchMetadata(any()) } just Runs

        // Act — call real downloadBatch (un-stub the spyk for this test)
        val cacheManagerReal = OIRQuestionCacheManager(
            firestore = mockFirestore,
            cacheDao = mockCacheDao,
            gson = gson,
            selector = OIRQuestionSelector(cacheDao = mockCacheDao, gson = gson),
            backgroundScope = testScope
        )
        cacheManagerReal.downloadBatch("batch_001")

        // Assert — metadata was stored with questionCount == 50
        coVerify {
            mockCacheDao.insertBatchMetadata(match { it.questionCount == 50 })
        }
    }

    @Test
    fun `downloadBatch withMissingTotalQuestions fallsBackToQuestionListSize`() = runTest {
        // Arrange — batch doc has no totalQuestions field; fallback to questions.size
        val docData = mapOf<String, Any?>(
            "version" to "1.0.0",
            "questions" to listOf(
                mapOf(
                    "id" to "q1", "questionNumber" to 1L, "type" to "VERBAL_REASONING",
                    "questionText" to "Q1", "options" to emptyList<Any>(),
                    "correctAnswerId" to "A", "explanation" to "E",
                    "difficulty" to "EASY", "tags" to listOf<String>()
                )
            )
        )
        buildMockFirestoreChainForDownloadBatch(docData)
        coEvery { mockCacheDao.isBatchDownloaded(any()) } returns false
        coEvery { mockCacheDao.insertQuestions(any()) } just Runs
        coEvery { mockCacheDao.insertBatchMetadata(any()) } just Runs

        val cacheManagerReal = OIRQuestionCacheManager(
            firestore = mockFirestore,
            cacheDao = mockCacheDao,
            gson = gson,
            selector = OIRQuestionSelector(cacheDao = mockCacheDao, gson = gson),
            backgroundScope = testScope
        )
        cacheManagerReal.downloadBatch("batch_001")

        // questionCount falls back to questions.size (1 question in mock)
        coVerify {
            mockCacheDao.insertBatchMetadata(match { it.questionCount == 1 })
        }
    }

    private fun createMockCachedQuestion(
        id: String,
        type: String = OIRQuestionType.VERBAL_REASONING.name,
        lastUsed: Long? = null,
        usageCount: Int = 0
    ): CachedOIRQuestionEntity {
        return CachedOIRQuestionEntity(
            id = id,
            questionNumber = 1,
            type = type,
            subtype = null,
            questionText = "Sample question",
            optionsJson = """[{"id":"opt_a","text":"Option A"},{"id":"opt_b","text":"Option B"},{"id":"opt_c","text":"Option C"},{"id":"opt_d","text":"Option D"}]""",
            correctAnswerId = "opt_a",
            explanation = "Sample explanation",
            difficulty = QuestionDifficulty.MEDIUM.name,
            tags = "test",
            batchId = "batch_001",
            cachedAt = System.currentTimeMillis(),
            lastUsed = lastUsed,
            usageCount = usageCount
        )
    }
}

