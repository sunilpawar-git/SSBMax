package com.ssbmax.core.domain.model

import org.junit.Test
import org.junit.Assert.*
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for SSB domain models
 */
class SSBTestTest {
    
    @Test
    fun ssbTest_creation_works() {
        val test = SSBTest(
            id = "tat_001",
            type = TestType.TAT,
            category = SSBCategory.PSYCHOLOGY,
            title = "TAT Test",
            description = "Thematic Apperception Test",
            timeLimit = 60.minutes,
            questionCount = 12,
            instructions = "View images and write stories",
            isPremium = false
        )
        
        assertEquals("tat_001", test.id)
        assertEquals(TestType.TAT, test.type)
        assertEquals(SSBCategory.PSYCHOLOGY, test.category)
        assertFalse(test.isPremium)
    }
    
    @Test
    fun testResult_calculates_pass_correctly() {
        val passingResult = TestResult(
            id = "result_1",
            testId = "test_1",
            userId = "user_1",
            score = 75f,
            completedAt = System.currentTimeMillis(),
            timeSpent = 30.minutes
        )
        
        assertTrue(passingResult.passed)
        
        val failingResult = passingResult.copy(score = 50f)
        assertFalse(failingResult.passed)
    }
    
    @Test
    fun testSession_tracks_progress() {
        val session = TestSession(
            sessionId = "session_1",
            testId = "test_1",
            userId = "user_1",
            startedAt = System.currentTimeMillis(),
            currentQuestion = 5,
            responses = mapOf("q1" to "answer1", "q2" to "answer2")
        )
        
        assertEquals(5, session.currentQuestion)
        assertEquals(2, session.responses.size)
    }
    
    @Test
    fun all_test_types_defined() {
        // Verify all SSB test types are represented
        val testTypes = TestType.values()
        assertTrue(testTypes.contains(TestType.TAT))
        assertTrue(testTypes.contains(TestType.WAT))
        assertTrue(testTypes.contains(TestType.SRT))
        assertTrue(testTypes.contains(TestType.PPDT))
        assertTrue(testTypes.contains(TestType.OIR))
    }
    
    @Test
    fun all_ssb_categories_defined() {
        val categories = SSBCategory.values()
        assertTrue(categories.contains(SSBCategory.SCREENING))
        assertTrue(categories.contains(SSBCategory.PSYCHOLOGY))
        assertTrue(categories.contains(SSBCategory.GTO))
        assertTrue(categories.contains(SSBCategory.INTERVIEW))
        assertTrue(categories.contains(SSBCategory.CONFERENCE))
    }
}

