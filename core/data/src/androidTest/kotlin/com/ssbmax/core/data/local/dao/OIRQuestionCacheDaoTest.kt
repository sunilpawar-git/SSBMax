package com.ssbmax.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ssbmax.core.data.local.SSBDatabase
import com.ssbmax.core.data.local.entity.CachedOIRQuestionEntity
import com.ssbmax.core.data.local.entity.OIRBatchMetadataEntity
import com.ssbmax.core.domain.model.OIRQuestionType
import com.ssbmax.core.domain.model.QuestionDifficulty
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for OIRQuestionCacheDao
 * 
 * Runs on an Android device or emulator to test actual Room database operations:
 * - Question insertion and retrieval
 * - Usage tracking (lastUsed, usageCount)
 * - Batch management
 * - Cache cleanup
 * - Query performance
 * 
 * These tests use an in-memory database for isolation and speed.
 */
@RunWith(AndroidJUnit4::class)
class OIRQuestionCacheDaoTest {
    
    private lateinit var database: SSBDatabase
    private lateinit var dao: OIRQuestionCacheDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Use in-memory database for testing (cleared after tests)
        database = Room.inMemoryDatabaseBuilder(context, SSBDatabase::class.java)
            .allowMainThreadQueries() // Only for testing
            .build()
        
        dao = database.oirQuestionCacheDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    // ==================== Question Insertion Tests ====================
    
    @Test
    fun insertQuestion_storesSuccessfully() = runTest {
        // Given
        val question = createMockQuestion("q1", OIRQuestionType.VERBAL_REASONING.name)
        
        // When
        dao.insertQuestion(question)
        
        // Then
        val retrieved = dao.getQuestionById("q1")
        assertNotNull("Question should be retrieved", retrieved)
        assertEquals("Question ID should match", "q1", retrieved?.id)
        assertEquals("Question type should match", OIRQuestionType.VERBAL_REASONING.name, retrieved?.type)
    }
    
    @Test
    fun insertQuestions_storesMultiple() = runTest {
        // Given
        val questions = listOf(
            createMockQuestion("q1", OIRQuestionType.VERBAL_REASONING.name),
            createMockQuestion("q2", OIRQuestionType.NON_VERBAL_REASONING.name),
            createMockQuestion("q3", OIRQuestionType.NUMERICAL_ABILITY.name)
        )
        
        // When
        dao.insertQuestions(questions)
        
        // Then
        val count = dao.getCachedQuestionCount()
        assertEquals("Should have 3 questions", 3, count)
    }
    
    @Test
    fun insertQuestion_withConflict_replacesExisting() = runTest {
        // Given
        val question1 = createMockQuestion("q1", OIRQuestionType.VERBAL_REASONING.name, questionText = "Original")
        val question2 = createMockQuestion("q1", OIRQuestionType.VERBAL_REASONING.name, questionText = "Updated")
        
        // When
        dao.insertQuestion(question1)
        dao.insertQuestion(question2)
        
        // Then
        val count = dao.getCachedQuestionCount()
        val retrieved = dao.getQuestionById("q1")
        
        assertEquals("Should still have 1 question (replaced)", 1, count)
        assertEquals("Question text should be updated", "Updated", retrieved?.questionText)
    }
    
    // ==================== Question Retrieval Tests ====================
    
    @Test
    fun getQuestionsByType_filtersCorrectly() = runTest {
        // Given
        val questions = listOf(
            createMockQuestion("v1", OIRQuestionType.VERBAL_REASONING.name),
            createMockQuestion("v2", OIRQuestionType.VERBAL_REASONING.name),
            createMockQuestion("nv1", OIRQuestionType.NON_VERBAL_REASONING.name),
            createMockQuestion("num1", OIRQuestionType.NUMERICAL_ABILITY.name)
        )
        dao.insertQuestions(questions)
        
        // When
        val verbalQuestions = dao.getQuestionsByType(OIRQuestionType.VERBAL_REASONING.name, 10)
        
        // Then
        assertEquals("Should get 2 verbal questions", 2, verbalQuestions.size)
        assertTrue("All should be verbal", verbalQuestions.all { it.type == OIRQuestionType.VERBAL_REASONING.name })
    }
    
    @Test
    fun getUnusedQuestionsByType_excludesRecentlyUsed() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000L)
        val twoWeeksAgo = now - (14 * 24 * 60 * 60 * 1000L)
        
        val questions = listOf(
            createMockQuestion("q1", OIRQuestionType.VERBAL_REASONING.name, lastUsed = now), // Recently used
            createMockQuestion("q2", OIRQuestionType.VERBAL_REASONING.name, lastUsed = twoWeeksAgo), // Old enough
            createMockQuestion("q3", OIRQuestionType.VERBAL_REASONING.name, lastUsed = null) // Never used
        )
        dao.insertQuestions(questions)
        
        // When - Get questions older than 1 week
        val unusedQuestions = dao.getUnusedQuestionsByType(
            type = OIRQuestionType.VERBAL_REASONING.name,
            olderThan = oneWeekAgo,
            count = 10
        )
        
        // Then
        assertEquals("Should get 2 unused questions", 2, unusedQuestions.size)
        assertFalse("Should not include recently used q1", unusedQuestions.any { it.id == "q1" })
        assertTrue("Should include q2", unusedQuestions.any { it.id == "q2" })
        assertTrue("Should include q3", unusedQuestions.any { it.id == "q3" })
    }
    
    @Test
    fun getQuestionsByBatches_filtersCorrectly() = runTest {
        // Given
        val questions = listOf(
            createMockQuestion("q1", batchId = "batch_001"),
            createMockQuestion("q2", batchId = "batch_001"),
            createMockQuestion("q3", batchId = "batch_002"),
            createMockQuestion("q4", batchId = "batch_003")
        )
        dao.insertQuestions(questions)
        
        // When
        val batch1Questions = dao.getQuestionsByBatches(listOf("batch_001", "batch_002"))
        
        // Then
        assertEquals("Should get 3 questions from batches 1 and 2", 3, batch1Questions.size)
        assertTrue("Should include batch_001 questions", batch1Questions.any { it.batchId == "batch_001" })
        assertTrue("Should include batch_002 questions", batch1Questions.any { it.batchId == "batch_002" })
        assertFalse("Should not include batch_003", batch1Questions.any { it.batchId == "batch_003" })
    }
    
    // ==================== Usage Tracking Tests ====================
    
    @Test
    fun markQuestionsUsed_updatesTimestampAndCount() = runTest {
        // Given
        val questions = listOf(
            createMockQuestion("q1", usageCount = 0, lastUsed = null),
            createMockQuestion("q2", usageCount = 2, lastUsed = 100000L)
        )
        dao.insertQuestions(questions)
        
        val timestamp = System.currentTimeMillis()
        
        // When
        dao.markQuestionsUsed(listOf("q1", "q2"), timestamp)
        
        // Then
        val q1 = dao.getQuestionById("q1")
        val q2 = dao.getQuestionById("q2")
        
        assertEquals("q1 usage count should be 1", 1, q1?.usageCount)
        assertEquals("q1 lastUsed should be updated", timestamp, q1?.lastUsed)
        
        assertEquals("q2 usage count should be 3", 3, q2?.usageCount)
        assertEquals("q2 lastUsed should be updated", timestamp, q2?.lastUsed)
    }
    
    @Test
    fun getMostUsedQuestions_returnsCorrectOrder() = runTest {
        // Given
        val questions = listOf(
            createMockQuestion("q1", usageCount = 5),
            createMockQuestion("q2", usageCount = 10),
            createMockQuestion("q3", usageCount = 3),
            createMockQuestion("q4", usageCount = 8)
        )
        dao.insertQuestions(questions)
        
        // When
        val mostUsed = dao.getMostUsedQuestions(limit = 2)
        
        // Then
        assertEquals("Should get 2 questions", 2, mostUsed.size)
        assertEquals("First should be q2 (10 uses)", "q2", mostUsed[0].id)
        assertEquals("Second should be q4 (8 uses)", "q4", mostUsed[1].id)
    }
    
    // ==================== Count & Statistics Tests ====================
    
    @Test
    fun getCachedQuestionCount_returnsCorrectTotal() = runTest {
        // Given
        val questions = (1..15).map { createMockQuestion("q$it") }
        dao.insertQuestions(questions)
        
        // When
        val count = dao.getCachedQuestionCount()
        
        // Then
        assertEquals("Should have 15 questions", 15, count)
    }
    
    @Test
    fun getQuestionCountByType_countsCorrectly() = runTest {
        // Given
        val questions = listOf(
            createMockQuestion("v1", OIRQuestionType.VERBAL_REASONING.name),
            createMockQuestion("v2", OIRQuestionType.VERBAL_REASONING.name),
            createMockQuestion("v3", OIRQuestionType.VERBAL_REASONING.name),
            createMockQuestion("nv1", OIRQuestionType.NON_VERBAL_REASONING.name)
        )
        dao.insertQuestions(questions)
        
        // When
        val verbalCount = dao.getQuestionCountByType(OIRQuestionType.VERBAL_REASONING.name)
        val nonVerbalCount = dao.getQuestionCountByType(OIRQuestionType.NON_VERBAL_REASONING.name)
        
        // Then
        assertEquals("Should have 3 verbal questions", 3, verbalCount)
        assertEquals("Should have 1 non-verbal question", 1, nonVerbalCount)
    }
    
    // ==================== Deletion Tests ====================
    
    @Test
    fun deleteBatch_removesOnlyBatchQuestions() = runTest {
        // Given
        val questions = listOf(
            createMockQuestion("q1", batchId = "batch_001"),
            createMockQuestion("q2", batchId = "batch_001"),
            createMockQuestion("q3", batchId = "batch_002")
        )
        dao.insertQuestions(questions)
        
        // When
        dao.deleteBatch("batch_001")
        
        // Then
        val remaining = dao.getAllQuestions()
        assertEquals("Should have 1 question remaining", 1, remaining.size)
        assertEquals("Remaining should be from batch_002", "batch_002", remaining[0].batchId)
    }
    
    @Test
    fun deleteOldestQuestions_removesOldestFirst() = runTest {
        // Given - Insert with different cached times
        val now = System.currentTimeMillis()
        val questions = listOf(
            createMockQuestion("q1", cachedAt = now - 30000), // Oldest
            createMockQuestion("q2", cachedAt = now - 20000),
            createMockQuestion("q3", cachedAt = now - 10000),
            createMockQuestion("q4", cachedAt = now) // Newest
        )
        dao.insertQuestions(questions)
        
        // When - Delete 2 oldest
        dao.deleteOldestQuestions(2)
        
        // Then
        val remaining = dao.getAllQuestions()
        assertEquals("Should have 2 questions remaining", 2, remaining.size)
        assertTrue("Should keep q3", remaining.any { it.id == "q3" })
        assertTrue("Should keep q4", remaining.any { it.id == "q4" })
        assertFalse("Should delete q1", remaining.any { it.id == "q1" })
        assertFalse("Should delete q2", remaining.any { it.id == "q2" })
    }
    
    @Test
    fun deleteAllQuestions_clearsCache() = runTest {
        // Given
        val questions = (1..10).map { createMockQuestion("q$it") }
        dao.insertQuestions(questions)
        
        // When
        dao.deleteAllQuestions()
        
        // Then
        val count = dao.getCachedQuestionCount()
        assertEquals("Should have 0 questions", 0, count)
    }
    
    // ==================== Batch Metadata Tests ====================
    
    @Test
    fun insertBatchMetadata_storesSuccessfully() = runTest {
        // Given
        val metadata = OIRBatchMetadataEntity(
            batchId = "batch_001",
            downloadedAt = System.currentTimeMillis(),
            questionCount = 100,
            version = "1.0"
        )
        
        // When
        dao.insertBatchMetadata(metadata)
        
        // Then
        val retrieved = dao.getBatchMetadata("batch_001")
        assertNotNull("Metadata should be retrieved", retrieved)
        assertEquals("Batch ID should match", "batch_001", retrieved?.batchId)
        assertEquals("Question count should match", 100, retrieved?.questionCount)
    }
    
    @Test
    fun isBatchDownloaded_returnsCorrectStatus() = runTest {
        // Given
        val metadata = OIRBatchMetadataEntity(
            batchId = "batch_001",
            downloadedAt = System.currentTimeMillis(),
            questionCount = 100,
            version = "1.0"
        )
        dao.insertBatchMetadata(metadata)
        
        // When & Then
        assertTrue("batch_001 should be downloaded", dao.isBatchDownloaded("batch_001"))
        assertFalse("batch_002 should not be downloaded", dao.isBatchDownloaded("batch_002"))
    }
    
    @Test
    fun getAllBatchMetadata_returnsOrderedByDate() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val batches = listOf(
            OIRBatchMetadataEntity("batch_001", now - 20000, 100, "1.0"),
            OIRBatchMetadataEntity("batch_002", now - 10000, 100, "1.0"),
            OIRBatchMetadataEntity("batch_003", now, 100, "1.0")
        )
        batches.forEach { dao.insertBatchMetadata(it) }
        
        // When
        val retrieved = dao.getAllBatchMetadata()
        
        // Then
        assertEquals("Should have 3 batches", 3, retrieved.size)
        assertEquals("First should be newest (batch_003)", "batch_003", retrieved[0].batchId)
        assertEquals("Last should be oldest (batch_001)", "batch_001", retrieved[2].batchId)
    }
    
    @Test
    fun deleteBatchMetadata_removesMetadata() = runTest {
        // Given
        val metadata = OIRBatchMetadataEntity("batch_001", System.currentTimeMillis(), 100, "1.0")
        dao.insertBatchMetadata(metadata)
        
        // When
        dao.deleteBatchMetadata("batch_001")
        
        // Then
        val retrieved = dao.getBatchMetadata("batch_001")
        assertNull("Metadata should be deleted", retrieved)
    }
    
    // ==================== Integration Tests ====================
    
    @Test
    fun fullCacheWorkflow_insertQueryUpdateDelete() = runTest {
        // 1. Insert batch metadata
        val metadata = OIRBatchMetadataEntity("batch_001", System.currentTimeMillis(), 3, "1.0")
        dao.insertBatchMetadata(metadata)
        
        // 2. Insert questions
        val questions = listOf(
            createMockQuestion("q1", OIRQuestionType.VERBAL_REASONING.name, batchId = "batch_001"),
            createMockQuestion("q2", OIRQuestionType.VERBAL_REASONING.name, batchId = "batch_001"),
            createMockQuestion("q3", OIRQuestionType.NON_VERBAL_REASONING.name, batchId = "batch_001")
        )
        dao.insertQuestions(questions)
        
        // 3. Verify insertion
        assertEquals("Should have 3 questions", 3, dao.getCachedQuestionCount())
        assertTrue("Batch should be marked as downloaded", dao.isBatchDownloaded("batch_001"))
        
        // 4. Query unused questions
        val unusedVerbal = dao.getUnusedQuestionsByType(
            OIRQuestionType.VERBAL_REASONING.name,
            System.currentTimeMillis(),
            2
        )
        assertEquals("Should get 2 verbal questions", 2, unusedVerbal.size)
        
        // 5. Mark as used
        val timestamp = System.currentTimeMillis()
        dao.markQuestionsUsed(listOf("q1"), timestamp)
        
        // 6. Verify usage tracking
        val q1 = dao.getQuestionById("q1")
        assertEquals("Usage count should be 1", 1, q1?.usageCount)
        
        // 7. Delete batch
        dao.deleteBatch("batch_001")
        dao.deleteBatchMetadata("batch_001")
        
        // 8. Verify cleanup
        assertEquals("All questions should be deleted", 0, dao.getCachedQuestionCount())
        assertFalse("Batch should be unmarked", dao.isBatchDownloaded("batch_001"))
    }
    
    // ==================== Helper Methods ====================
    
    private fun createMockQuestion(
        id: String,
        type: String = OIRQuestionType.VERBAL_REASONING.name,
        questionText: String = "Test question $id",
        batchId: String = "batch_001",
        usageCount: Int = 0,
        lastUsed: Long? = null,
        cachedAt: Long = System.currentTimeMillis()
    ): CachedOIRQuestionEntity {
        return CachedOIRQuestionEntity(
            id = id,
            questionNumber = id.filter { it.isDigit() }.toIntOrNull() ?: 1,
            type = type,
            subtype = null,
            questionText = questionText,
            optionsJson = """[{"id":"A","text":"Option A"}]""",
            correctAnswerId = "A",
            explanation = "Test explanation",
            difficulty = QuestionDifficulty.MEDIUM.name,
            tags = "test",
            batchId = batchId,
            cachedAt = cachedAt,
            lastUsed = lastUsed,
            usageCount = usageCount
        )
    }
}

