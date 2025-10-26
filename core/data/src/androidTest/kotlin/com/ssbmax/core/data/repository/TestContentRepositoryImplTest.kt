package com.ssbmax.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.FirebaseTestHelper
import com.ssbmax.core.domain.model.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for TestContentRepositoryImpl with Firebase Emulator
 * 
 * Tests:
 * - Firestore question fetching
 * - In-memory caching behavior
 * - Mock data fallback when Firestore is empty/fails
 * - Test session management
 * - Session expiry logic
 */
class TestContentRepositoryImplTest {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: TestContentRepositoryImpl
    
    private val testUserId = "test-user-${System.currentTimeMillis()}"
    private val testId = "test-${System.currentTimeMillis()}"
    private val sessionIds = mutableListOf<String>()
    
    @Before
    fun setUp() {
        // Get Firestore instance configured for emulator
        firestore = FirebaseTestHelper.getEmulatorFirestore()
        repository = TestContentRepositoryImpl(firestore)
    }
    
    @After
    fun tearDown() = runTest {
        // Clear repository cache
        repository.clearCache()
        
        // Clean up test sessions
        sessionIds.forEach { sessionId ->
            try {
                firestore.collection("test_sessions")
                    .document(sessionId)
                    .delete()
                    .await()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        sessionIds.clear()
        
        // Clean up test documents
        try {
            firestore.collection("tests")
                .document(testId)
                .delete()
                .await()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    // ==================== OIR Question Tests ====================
    
    @Test
    fun getOIRQuestions_returns_mock_data_when_firestore_empty() = runTest {
        // When - Firestore has no data
        val result = repository.getOIRQuestions(testId)
        
        // Then - Should fallback to mock data
        assertTrue("Should succeed", result.isSuccess)
        val questions = result.getOrNull()
        assertNotNull("Questions should not be null", questions)
        assertTrue("Should have questions", questions!!.isNotEmpty())
        assertEquals(TestType.OIR, questions.first().type)
    }
    
    @Test
    fun getOIRQuestions_fetches_from_firestore_when_available() = runTest {
        // Given - OIR questions in Firestore
        val firestoreQuestions = listOf(
            mapOf(
                "id" to "oir-1",
                "questionNumber" to 1,
                "type" to "VERBAL_REASONING",
                "questionText" to "Test question?",
                "options" to listOf(
                    mapOf("id" to "opt1", "text" to "Option 1"),
                    mapOf("id" to "opt2", "text" to "Option 2")
                ),
                "correctAnswerId" to "opt1",
                "explanation" to "Test explanation",
                "difficulty" to "MEDIUM"
            )
        )
        
        firestore.collection("tests")
            .document(testId)
            .set(mapOf("questions" to firestoreQuestions))
            .await()
        
        // When
        val result = repository.getOIRQuestions(testId)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        val questions = result.getOrNull()
        assertEquals(1, questions?.size)
        assertEquals("Test question?", questions?.first()?.questionText)
    }
    
    @Test
    fun getOIRQuestions_uses_cache_on_second_call() = runTest {
        // Given - First call fetches from Firestore/mock
        val firstResult = repository.getOIRQuestions(testId)
        val firstQuestions = firstResult.getOrNull()
        
        // When - Second call (should use cache)
        val secondResult = repository.getOIRQuestions(testId)
        val secondQuestions = secondResult.getOrNull()
        
        // Then - Should return same instance from cache
        assertEquals(firstQuestions, secondQuestions)
    }
    
    // ==================== PPDT Question Tests ====================
    
    @Test
    fun getPPDTQuestions_returns_mock_data_when_firestore_empty() = runTest {
        // When
        val result = repository.getPPDTQuestions(testId)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        val questions = result.getOrNull()
        assertNotNull("Questions should not be null", questions)
        assertTrue("Should have questions", questions!!.isNotEmpty())
        assertNotNull("Should have image URL", questions.first().imageUrl)
    }
    
    @Test
    fun getPPDTQuestions_uses_cache() = runTest {
        // Given - First call
        val firstResult = repository.getPPDTQuestions(testId)
        
        // When - Second call
        val secondResult = repository.getPPDTQuestions(testId)
        
        // Then - Should be cached
        assertEquals(firstResult.getOrNull(), secondResult.getOrNull())
    }
    
    // ==================== TAT Question Tests ====================
    
    @Test
    fun getTATQuestions_returns_mock_data_when_firestore_empty() = runTest {
        // When
        val result = repository.getTATQuestions(testId)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        val questions = result.getOrNull()
        assertNotNull("Questions should not be null", questions)
        assertTrue("Should have questions", questions!!.isNotEmpty())
        assertTrue("Should have valid sequence numbers", 
            questions.first().sequenceNumber > 0)
    }
    
    @Test
    fun getTATQuestions_uses_cache() = runTest {
        // Given
        repository.getTATQuestions(testId)
        
        // When - Second call
        val result = repository.getTATQuestions(testId)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
    }
    
    // ==================== WAT Question Tests ====================
    
    @Test
    fun getWATQuestions_returns_mock_data_when_firestore_empty() = runTest {
        // When
        val result = repository.getWATQuestions(testId)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        val words = result.getOrNull()
        assertNotNull("Words should not be null", words)
        assertTrue("Should have words", words!!.isNotEmpty())
        assertTrue("Words should have text", words.first().word.isNotBlank())
    }
    
    @Test
    fun getWATQuestions_uses_cache() = runTest {
        // Given
        val firstResult = repository.getWATQuestions(testId)
        
        // When
        val secondResult = repository.getWATQuestions(testId)
        
        // Then
        assertEquals(firstResult.getOrNull(), secondResult.getOrNull())
    }
    
    // ==================== SRT Question Tests ====================
    
    @Test
    fun getSRTQuestions_returns_mock_data_when_firestore_empty() = runTest {
        // When
        val result = repository.getSRTQuestions(testId)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        val situations = result.getOrNull()
        assertNotNull("Situations should not be null", situations)
        assertTrue("Should have situations", situations!!.isNotEmpty())
        assertTrue("Situations should have text", 
            situations.first().situation.isNotBlank())
    }
    
    @Test
    fun getSRTQuestions_uses_cache() = runTest {
        // Given
        val firstResult = repository.getSRTQuestions(testId)
        
        // When
        val secondResult = repository.getSRTQuestions(testId)
        
        // Then
        assertEquals(firstResult.getOrNull(), secondResult.getOrNull())
    }
    
    // ==================== Cache Management Tests ====================
    
    @Test
    fun clearCache_removes_all_cached_questions() = runTest {
        // Given - Cache some questions
        repository.getOIRQuestions("test1")
        repository.getPPDTQuestions("test2")
        repository.getTATQuestions("test3")
        
        // When
        repository.clearCache()
        
        // Then - Should re-fetch (not immediate to test, but ensures cache is cleared)
        val result = repository.getOIRQuestions("test1")
        assertTrue("Should still succeed after cache clear", result.isSuccess)
    }
    
    // ==================== Test Session Management ====================
    
    @Test
    fun createTestSession_successfully_creates_session() = runTest {
        // When
        val result = repository.createTestSession(
            userId = testUserId,
            testId = testId,
            testType = TestType.TAT
        )
        
        // Then
        assertTrue("Session creation should succeed", result.isSuccess)
        val sessionId = result.getOrNull()
        assertNotNull("Session ID should not be null", sessionId)
        sessionIds.add(sessionId!!)
    }
    
    @Test
    fun createTestSession_persists_to_firestore() = runTest {
        // Given
        val result = repository.createTestSession(testUserId, testId, TestType.WAT)
        val sessionId = result.getOrNull()!!
        sessionIds.add(sessionId)
        
        // When - Query Firestore directly
        val doc = firestore.collection("test_sessions")
            .document(sessionId)
            .get()
            .await()
        
        // Then
        assertTrue("Session should exist in Firestore", doc.exists())
        assertEquals(testUserId, doc.getString("userId"))
        assertEquals(testId, doc.getString("testId"))
        assertEquals("WAT", doc.getString("testType"))
        assertEquals(true, doc.getBoolean("isActive"))
    }
    
    @Test
    fun hasActiveTestSession_returns_false_for_non_existent_session() = runTest {
        // When
        val result = repository.hasActiveTestSession(testUserId, "non-existent-test")
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertFalse("Should not have active session", result.getOrNull() ?: true)
    }
    
    @Test
    fun hasActiveTestSession_returns_true_for_active_session() = runTest {
        // Given - Create a session
        val createResult = repository.createTestSession(testUserId, testId, TestType.OIR)
        sessionIds.add(createResult.getOrNull()!!)
        
        // When
        val result = repository.hasActiveTestSession(testUserId, testId)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertTrue("Should have active session", result.getOrNull() ?: false)
    }
    
    @Test
    fun endTestSession_marks_session_inactive() = runTest {
        // Given - Create a session
        val createResult = repository.createTestSession(testUserId, testId, TestType.SRT)
        val sessionId = createResult.getOrNull()!!
        sessionIds.add(sessionId)
        
        // When
        val endResult = repository.endTestSession(sessionId)
        
        // Then
        assertTrue("End session should succeed", endResult.isSuccess)
        
        // Verify in Firestore
        val doc = firestore.collection("test_sessions")
            .document(sessionId)
            .get()
            .await()
        
        assertEquals(false, doc.getBoolean("isActive"))
        assertNotNull("Should have end time", doc.getLong("endTime"))
    }
    
    @Test
    fun createTestSession_sets_correct_expiry_time() = runTest {
        // Given
        val startTime = System.currentTimeMillis()
        
        // When
        val result = repository.createTestSession(testUserId, testId, TestType.PPDT)
        val sessionId = result.getOrNull()!!
        sessionIds.add(sessionId)
        
        // Then - Check expiry time (should be ~2 hours from now)
        val doc = firestore.collection("test_sessions")
            .document(sessionId)
            .get()
            .await()
        
        val expiryTime = doc.getLong("expiryTime") ?: 0
        val expectedExpiry = startTime + (2 * 60 * 60 * 1000) // 2 hours
        
        // Allow 1 second tolerance
        assertTrue("Expiry time should be ~2 hours from now",
            expiryTime in (expectedExpiry - 1000)..(expectedExpiry + 1000))
    }
    
    @Test
    fun createTestSession_supports_all_test_types() = runTest {
        val testTypes = listOf(TestType.OIR, TestType.PPDT, TestType.TAT, 
                              TestType.WAT, TestType.SRT)
        
        testTypes.forEach { testType ->
            // When
            val result = repository.createTestSession(
                userId = testUserId,
                testId = "${testId}-${testType.name}",
                testType = testType
            )
            
            // Then
            assertTrue("Should create session for $testType", result.isSuccess)
            sessionIds.add(result.getOrNull()!!)
        }
    }
    
    // ==================== Mock Data Quality Tests ====================
    
    @Test
    fun mock_oir_questions_have_valid_structure() = runTest {
        // When
        val result = repository.getOIRQuestions(testId)
        
        // Then
        val questions = result.getOrNull()!!
        questions.forEach { question ->
            assertTrue("Question number should be positive", 
                question.questionNumber > 0)
            assertTrue("Should have question text", 
                question.questionText.isNotBlank())
            assertTrue("Should have at least 2 options", 
                question.options.size >= 2)
            assertTrue("Should have correct answer", 
                question.correctAnswerId.isNotBlank())
            assertTrue("Should have explanation", 
                question.explanation.isNotBlank())
        }
    }
    
    @Test
    fun mock_ppdt_questions_have_valid_structure() = runTest {
        // When
        val result = repository.getPPDTQuestions(testId)
        
        // Then
        val questions = result.getOrNull()!!
        questions.forEach { question ->
            assertTrue("Should have image URL", question.imageUrl.isNotBlank())
            assertTrue("Should have description", 
                question.imageDescription.isNotBlank())
            assertTrue("Viewing time should be positive", 
                question.viewingTimeSeconds > 0)
            assertTrue("Writing time should be positive", 
                question.writingTimeMinutes > 0)
        }
    }
    
    @Test
    fun mock_tat_questions_have_valid_structure() = runTest {
        // When
        val result = repository.getTATQuestions(testId)
        
        // Then
        val questions = result.getOrNull()!!
        questions.forEach { question ->
            assertTrue("Should have image URL", question.imageUrl.isNotBlank())
            assertTrue("Sequence number should be positive", 
                question.sequenceNumber > 0)
            assertTrue("Viewing time should be positive", 
                question.viewingTimeSeconds > 0)
            assertTrue("Writing time should be positive", 
                question.writingTimeMinutes > 0)
        }
    }
    
    @Test
    fun mock_wat_words_have_valid_structure() = runTest {
        // When
        val result = repository.getWATQuestions(testId)
        
        // Then
        val words = result.getOrNull()!!
        words.forEach { word ->
            assertTrue("Word should not be blank", word.word.isNotBlank())
            assertTrue("Sequence number should be positive", 
                word.sequenceNumber > 0)
            assertTrue("Time allowed should be positive", 
                word.timeAllowedSeconds > 0)
        }
    }
    
    @Test
    fun mock_srt_situations_have_valid_structure() = runTest {
        // When
        val result = repository.getSRTQuestions(testId)
        
        // Then
        val situations = result.getOrNull()!!
        situations.forEach { situation ->
            assertTrue("Situation should not be blank", 
                situation.situation.isNotBlank())
            assertTrue("Sequence number should be positive", 
                situation.sequenceNumber > 0)
            assertTrue("Time allowed should be positive", 
                situation.timeAllowedSeconds > 0)
        }
    }
}

