package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.core.data.local.entity.OIRBatchMetadataEntity
import com.ssbmax.core.domain.model.OIRQuestionType
import com.ssbmax.core.domain.model.QuestionDifficulty
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Integration tests for OIRQuestionCacheManager
 * 
 * Tests the complete caching workflow:
 * - Firestore batch downloads
 * - Question distribution (40/40/15/5 ratio)
 * - Usage tracking
 * - Cache rotation
 * - Error handling
 */
class OIRCacheManagerIntegrationTest {
    
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
    
    @Before
    fun setup() {
        mockFirestore = mockk(relaxed = true)
        mockCacheDao = mockk(relaxed = true)
        
        cacheManager = OIRQuestionCacheManager(
            firestore = mockFirestore,
            cacheDao = mockCacheDao,
            gson = gson
        )
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    // ==================== Question Distribution Tests ====================
    
    @Test
    fun `getTestQuestions returns correct distribution of question types`() = runTest {
        // Given - 100 cached questions with proper distribution
        val cachedQuestions = createMockCachedQuestions(
            verbal = 45,
            nonVerbal = 35,
            numerical = 15,
            spatial = 5
        )
        
        coEvery { mockCacheDao.getCachedQuestionCount() } returns 100
        coEvery { 
            mockCacheDao.getUnusedQuestionsByType(OIRQuestionType.VERBAL_REASONING.name, any(), any())
        } returns cachedQuestions.filter { it.type == OIRQuestionType.VERBAL_REASONING.name }.take(20)
        
        coEvery { 
            mockCacheDao.getUnusedQuestionsByType(OIRQuestionType.NON_VERBAL_REASONING.name, any(), any())
        } returns cachedQuestions.filter { it.type == OIRQuestionType.NON_VERBAL_REASONING.name }.take(20)
        
        coEvery { 
            mockCacheDao.getUnusedQuestionsByType(OIRQuestionType.NUMERICAL_ABILITY.name, any(), any())
        } returns cachedQuestions.filter { it.type == OIRQuestionType.NUMERICAL_ABILITY.name }.take(8)
        
        coEvery { 
            mockCacheDao.getUnusedQuestionsByType(OIRQuestionType.SPATIAL_REASONING.name, any(), any())
        } returns cachedQuestions.filter { it.type == OIRQuestionType.SPATIAL_REASONING.name }.take(2)
        
        // When
        val result = cacheManager.getTestQuestions(count = 50)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        val questions = result.getOrNull()!!
        
        assertEquals("Should have 50 questions", 50, questions.size)
        
        // Verify distribution (40/40/15/5)
        val verbalCount = questions.count { it.type == OIRQuestionType.VERBAL_REASONING }
        val nonVerbalCount = questions.count { it.type == OIRQuestionType.NON_VERBAL_REASONING }
        val numericalCount = questions.count { it.type == OIRQuestionType.NUMERICAL_ABILITY }
        val spatialCount = questions.count { it.type == OIRQuestionType.SPATIAL_REASONING }
        
        assertEquals("Verbal should be 40%", 20, verbalCount)
        assertEquals("Non-verbal should be 40%", 20, nonVerbalCount)
        assertEquals("Numerical should be 15%", 8, numericalCount)
        assertEquals("Spatial should be ~5%", 2, spatialCount)
    }
    
    @Test
    fun `getTestQuestions avoids recently used questions`() = runTest {
        // Given - some questions marked as recently used
        val unusedThreshold = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val recentlyUsedTime = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L) // 2 days ago
        
        val cachedQuestions = listOf(
            createMockCachedQuestion(id = "q1", lastUsed = recentlyUsedTime, usageCount = 3),
            createMockCachedQuestion(id = "q2", lastUsed = null, usageCount = 0),
            createMockCachedQuestion(id = "q3", lastUsed = null, usageCount = 0)
        )
        
        coEvery { mockCacheDao.getCachedQuestionCount() } returns 100
        coEvery { 
            mockCacheDao.getUnusedQuestionsByType(any(), less(unusedThreshold), any())
        } returns cachedQuestions.filter { it.lastUsed == null || it.lastUsed!! < unusedThreshold }
        
        // When
        val result = cacheManager.getTestQuestions(count = 50)
        
        // Then
        coVerify {
            // Should query for questions older than 7 days
            mockCacheDao.getUnusedQuestionsByType(
                OIRQuestionType.VERBAL_REASONING.name,
                less(unusedThreshold),
                20
            )
        }
    }
    
    // ==================== Initial Sync Tests ====================
    
    @Test
    fun `initialSync skips download when cache already has enough questions`() = runTest {
        // Given - cache already has 100 questions
        coEvery { mockCacheDao.getCachedQuestionCount() } returns 100
        
        // When
        val result = cacheManager.initialSync()
        
        // Then
        assertTrue("Should succeed without downloading", result.isSuccess)
        
        // Verify no Firestore calls made
        verify(exactly = 0) { mockFirestore.collection(any()) }
    }
    
    @Test
    fun `initialSync downloads batch when cache is empty`() = runTest {
        // Given - empty cache
        coEvery { mockCacheDao.getCachedQuestionCount() } returnsMany listOf(0, 100)
        coEvery { mockCacheDao.isBatchDownloaded(any()) } returns false
        coEvery { mockCacheDao.insertQuestions(any()) } just Runs
        coEvery { mockCacheDao.insertBatchMetadata(any()) } just Runs
        
        // Mock Firestore response
        val mockDocSnapshot = mockk<com.google.firebase.firestore.DocumentSnapshot>(relaxed = true)
        val mockBatchDoc = mockk<com.google.firebase.firestore.DocumentReference>(relaxed = true)
        val mockCollection = mockk<com.google.firebase.firestore.CollectionReference>(relaxed = true)
        
        every { mockDocSnapshot.exists() } returns true
        every { mockDocSnapshot.get("questions") } returns createFirestoreQuestionsList(100)
        
        val mockTask = mockk<com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot>>(relaxed = true)
        every { mockTask.isComplete } returns true
        every { mockTask.isSuccessful } returns true
        every { mockTask.result } returns mockDocSnapshot
        every { mockBatchDoc.get() } returns mockTask
        
        every { mockCollection.document(any()) } returns mockBatchDoc
        every { mockFirestore.collection(any()).document(any()).collection(any()) } returns mockCollection
        
        // When
        val result = cacheManager.initialSync()
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        
        // Verify batch was inserted
        coVerify { mockCacheDao.insertQuestions(any()) }
        coVerify { mockCacheDao.insertBatchMetadata(any()) }
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
        
        val questions = createMockCachedQuestions(
            verbal = 45,
            nonVerbal = 35,
            numerical = 15,
            spatial = 5
        )
        
        coEvery { mockCacheDao.getAllBatchMetadata() } returns batches
        coEvery { mockCacheDao.getCachedQuestionCount() } returns 100
        coEvery { mockCacheDao.getAllQuestions() } returns questions
        
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
    
    private fun createMockCachedQuestions(
        verbal: Int,
        nonVerbal: Int,
        numerical: Int,
        spatial: Int
    ): List<CachedOIRQuestionEntity> {
        val questions = mutableListOf<CachedOIRQuestionEntity>()
        
        repeat(verbal) {
            questions.add(createMockCachedQuestion(
                id = "verbal_$it",
                type = OIRQuestionType.VERBAL_REASONING.name
            ))
        }
        
        repeat(nonVerbal) {
            questions.add(createMockCachedQuestion(
                id = "nonverbal_$it",
                type = OIRQuestionType.NON_VERBAL_REASONING.name
            ))
        }
        
        repeat(numerical) {
            questions.add(createMockCachedQuestion(
                id = "numerical_$it",
                type = OIRQuestionType.NUMERICAL_ABILITY.name
            ))
        }
        
        repeat(spatial) {
            questions.add(createMockCachedQuestion(
                id = "spatial_$it",
                type = OIRQuestionType.SPATIAL_REASONING.name
            ))
        }
        
        return questions
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
            optionsJson = """[{"id":"A","text":"Option A"}]""",
            correctAnswerId = "A",
            explanation = "Sample explanation",
            difficulty = QuestionDifficulty.MEDIUM.name,
            tags = "test",
            batchId = "batch_001",
            cachedAt = System.currentTimeMillis(),
            lastUsed = lastUsed,
            usageCount = usageCount
        )
    }
    
    private fun createFirestoreQuestionsList(count: Int): List<Map<String, Any>> {
        return (1..count).map { i ->
            mapOf(
                "id" to "q$i",
                "questionNumber" to i,
                "type" to "VERBAL_REASONING",
                "questionText" to "Question $i",
                "options" to listOf(mapOf("id" to "A", "text" to "Option A")),
                "correctAnswerId" to "A",
                "explanation" to "Explanation $i",
                "difficulty" to "MEDIUM",
                "tags" to listOf("test")
            )
        }
    }
}

