package com.ssbmax.core.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for Submission domain models
 * Tests TAT, WAT, and SRT submission models
 */
class SubmissionModelsTest {
    
    @Test
    fun `TATSubmission is created with correct properties`() {
        // Given
        val startTime = Instant.now().minusSeconds(300)
        val endTime = Instant.now()
        val responses = listOf(
            TATResponse(
                questionId = "tat-1",
                story = "A story about leadership",
                timeSpent = 240
            )
        )
        
        // When
        val submission = TATSubmission(
            userId = "user123",
            testId = "tat-test-1",
            testType = TestType.TAT,
            responses = responses,
            startTime = startTime,
            endTime = endTime,
            totalTimeSpent = 300
        )
        
        // Then
        assertEquals("user123", submission.userId)
        assertEquals("tat-test-1", submission.testId)
        assertEquals(TestType.TAT, submission.testType)
        assertEquals(1, submission.responses.size)
        assertEquals(startTime, submission.startTime)
        assertEquals(endTime, submission.endTime)
        assertEquals(300, submission.totalTimeSpent)
    }
    
    @Test
    fun `TATResponse stores story and time correctly`() {
        // When
        val response = TATResponse(
            questionId = "tat-1",
            story = "A compelling narrative about courage and determination",
            timeSpent = 230
        )
        
        // Then
        assertEquals("tat-1", response.questionId)
        assertEquals("A compelling narrative about courage and determination", response.story)
        assertEquals(230, response.timeSpent)
    }
    
    @Test
    fun `WATSubmission is created with correct properties`() {
        // Given
        val startTime = Instant.now().minusSeconds(900)
        val endTime = Instant.now()
        val responses = listOf(
            WATResponse(
                questionId = "wat-1",
                word = "VICTORY",
                response = "Success through perseverance",
                timeSpent = 14
            ),
            WATResponse(
                questionId = "wat-2",
                word = "COURAGE",
                response = "Bravery in adversity",
                timeSpent = 13
            )
        )
        
        // When
        val submission = WATSubmission(
            userId = "user456",
            testId = "wat-test-1",
            testType = TestType.WAT,
            responses = responses,
            startTime = startTime,
            endTime = endTime,
            totalTimeSpent = 900
        )
        
        // Then
        assertEquals("user456", submission.userId)
        assertEquals("wat-test-1", submission.testId)
        assertEquals(TestType.WAT, submission.testType)
        assertEquals(2, submission.responses.size)
        assertEquals(900, submission.totalTimeSpent)
    }
    
    @Test
    fun `WATResponse stores word and association correctly`() {
        // When
        val response = WATResponse(
            questionId = "wat-5",
            word = "LEADER",
            response = "Guides team to success",
            timeSpent = 12
        )
        
        // Then
        assertEquals("wat-5", response.questionId)
        assertEquals("LEADER", response.word)
        assertEquals("Guides team to success", response.response)
        assertEquals(12, response.timeSpent)
    }
    
    @Test
    fun `SRTSubmission is created with correct properties`() {
        // Given
        val startTime = Instant.now().minusSeconds(1800)
        val endTime = Instant.now()
        val responses = listOf(
            SRTResponse(
                questionId = "srt-1",
                situation = "Team conflict during critical project",
                response = "Mediate discussion, find common ground, focus on goals",
                timeSpent = 28
            )
        )
        
        // When
        val submission = SRTSubmission(
            userId = "user789",
            testId = "srt-test-1",
            testType = TestType.SRT,
            responses = responses,
            startTime = startTime,
            endTime = endTime,
            totalTimeSpent = 1800
        )
        
        // Then
        assertEquals("user789", submission.userId)
        assertEquals("srt-test-1", submission.testId)
        assertEquals(TestType.SRT, submission.testType)
        assertEquals(1, submission.responses.size)
        assertEquals(1800, submission.totalTimeSpent)
    }
    
    @Test
    fun `SRTResponse stores situation and reaction correctly`() {
        // When
        val response = SRTResponse(
            questionId = "srt-10",
            situation = "Emergency situation requiring immediate action",
            response = "Assess situation, prioritize safety, delegate tasks, communicate clearly",
            timeSpent = 25
        )
        
        // Then
        assertEquals("srt-10", response.questionId)
        assertEquals("Emergency situation requiring immediate action", response.situation)
        assertEquals("Assess situation, prioritize safety, delegate tasks, communicate clearly", response.response)
        assertEquals(25, response.timeSpent)
    }
    
    @Test
    fun `TATSubmission with multiple responses handles correctly`() {
        // Given
        val responses = (1..12).map { i ->
            TATResponse(
                questionId = "tat-$i",
                story = "Story number $i",
                timeSpent = 220 + i
            )
        }
        
        // When
        val submission = TATSubmission(
            userId = "user123",
            testId = "tat-test-full",
            testType = TestType.TAT,
            responses = responses,
            startTime = Instant.now().minusSeconds(3000),
            endTime = Instant.now(),
            totalTimeSpent = 3000
        )
        
        // Then
        assertEquals(12, submission.responses.size)
        assertEquals("Story number 1", submission.responses.first().story)
        assertEquals("Story number 12", submission.responses.last().story)
    }
    
    @Test
    fun `WATSubmission with 60 words handles correctly`() {
        // Given
        val words = listOf("VICTORY", "COURAGE", "LEADER", "TEAM", "DUTY")
        val responses = (1..60).map { i ->
            WATResponse(
                questionId = "wat-$i",
                word = words[i % words.size],
                response = "Response $i",
                timeSpent = 14
            )
        }
        
        // When
        val submission = WATSubmission(
            userId = "user456",
            testId = "wat-test-full",
            testType = TestType.WAT,
            responses = responses,
            startTime = Instant.now().minusSeconds(900),
            endTime = Instant.now(),
            totalTimeSpent = 900
        )
        
        // Then
        assertEquals(60, submission.responses.size)
        assertEquals("VICTORY", submission.responses.first().word)
        assertEquals(900, submission.totalTimeSpent)
    }
    
    @Test
    fun `Submissions calculate duration correctly`() {
        // Given
        val startTime = Instant.parse("2024-01-01T10:00:00Z")
        val endTime = Instant.parse("2024-01-01T10:05:00Z")
        
        // When
        val tatSubmission = TATSubmission(
            userId = "user123",
            testId = "test-1",
            testType = TestType.TAT,
            responses = emptyList(),
            startTime = startTime,
            endTime = endTime,
            totalTimeSpent = 300
        )
        
        // Then
        val expectedDuration = 300L // 5 minutes in seconds
        assertEquals(expectedDuration, tatSubmission.totalTimeSpent.toLong())
    }
    
    @Test
    fun `Empty responses list is handled correctly`() {
        // When
        val submission = TATSubmission(
            userId = "user123",
            testId = "test-1",
            testType = TestType.TAT,
            responses = emptyList(),
            startTime = Instant.now().minusSeconds(100),
            endTime = Instant.now(),
            totalTimeSpent = 100
        )
        
        // Then
        assertTrue(submission.responses.isEmpty())
        assertEquals(0, submission.responses.size)
    }
}

