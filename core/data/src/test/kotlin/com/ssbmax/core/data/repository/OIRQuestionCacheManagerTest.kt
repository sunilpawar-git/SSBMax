package com.ssbmax.core.data.repository

import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.OIRQuestionCacheDao
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.core.data.local.entity.OIRBatchMetadataEntity
import com.ssbmax.core.domain.model.QuestionDifficulty
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for OIRQuestionCacheManager
 */
class OIRQuestionCacheManagerTest {
    
    private lateinit var cacheManager: OIRQuestionCacheManager
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockCacheDao: OIRQuestionCacheDao
    private lateinit var mockWorkManager: WorkManager
    
    @Before
    fun setup() {
        mockFirestore = mockk(relaxed = true)
        mockCacheDao = mockk(relaxed = true)
        mockWorkManager = mockk(relaxed = true)
        
        cacheManager = OIRQuestionCacheManager(
            firestore = mockFirestore,
            cacheDao = mockCacheDao,
            workManager = mockWorkManager
        )
    }
    
    @After
    fun tearDown() {
        unmockkAll()
    }
    
    @Test
    fun `getTestQuestions returns cached questions when available`() = runTest {
        // Given
        val cachedQuestions = listOf(
            createMockCachedQuestion(id = "q1", questionNumber = 1),
            createMockCachedQuestion(id = "q2", questionNumber = 2)
        )
        
        coEvery { mockCacheDao.getCachedQuestionCount() } returns 100
        coEvery { 
            mockCacheDao.getUnusedQuestions(any(), any()) 
        } returns cachedQuestions
        
        // When
        val result = cacheManager.getTestQuestions(count = 2)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        coVerify { mockCacheDao.getUnusedQuestions(any(), 2) }
    }
    
    @Test
    fun `getCacheStatus returns correct statistics`() = runTest {
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
            createMockCachedQuestion(id = "q1", type = "VERBAL"),
            createMockCachedQuestion(id = "q2", type = "NUMERICAL"),
            createMockCachedQuestion(id = "q3", type = "VERBAL")
        )
        
        coEvery { mockCacheDao.getAllBatchMetadata() } returns batches
        coEvery { mockCacheDao.getCachedQuestionCount() } returns 3
        coEvery { mockCacheDao.getAllQuestions() } returns questions
        
        // When
        val status = cacheManager.getCacheStatus()
        
        // Then
        assertEquals(3, status.cachedQuestions)
        assertEquals(1, status.batchesDownloaded)
        assertEquals(2, status.verbalCount)
        assertEquals(1, status.numericalCount)
    }
    
    @Test
    fun `markQuestionsUsed updates usage timestamp`() = runTest {
        // Given
        val questionIds = listOf("q1", "q2", "q3")
        coEvery { 
            mockCacheDao.markQuestionsUsed(any(), any()) 
        } just Runs
        
        // When
        cacheManager.markQuestionsUsed(questionIds)
        
        // Then
        coVerify { 
            mockCacheDao.markQuestionsUsed(questionIds, any()) 
        }
    }
    
    @Test
    fun `getTestQuestions returns failure when no questions cached`() = runTest {
        // Given
        coEvery { mockCacheDao.getCachedQuestionCount() } returns 0
        
        // When
        val result = cacheManager.getTestQuestions(count = 50)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("No questions") == true
        )
    }
    
    private fun createMockCachedQuestion(
        id: String,
        questionNumber: Int = 1,
        type: String = "VERBAL"
    ): CachedOIRQuestionEntity {
        return CachedOIRQuestionEntity(
            id = id,
            questionNumber = questionNumber,
            type = type,
            subtype = null,
            questionText = "Sample question",
            optionsJson = """[{"id":"A","text":"Option A"}]""",
            correctAnswerId = "A",
            explanation = "Sample explanation",
            difficulty = QuestionDifficulty.MEDIUM.name,
            tags = "reasoning,logical",
            batchId = "batch_001",
            cachedAt = System.currentTimeMillis(),
            lastUsed = null,
            usageCount = 0
        )
    }
}

