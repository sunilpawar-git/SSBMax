package com.ssbmax.core.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for TestProgress domain models
 * Tests progress calculation, completion percentage, and status handling
 */
class TestProgressTest {
    
    @Test
    fun `Phase1Progress calculates 0 percent when no tests completed`() {
        // Given
        val progress = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.NOT_ATTEMPTED),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.NOT_ATTEMPTED)
        )
        
        // Then
        assertEquals(0f, progress.completionPercentage, 0.01f)
    }
    
    @Test
    fun `Phase1Progress calculates 50 percent when one test completed`() {
        // Given
        val progress = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.COMPLETED),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.NOT_ATTEMPTED)
        )
        
        // Then
        assertEquals(50f, progress.completionPercentage, 0.01f)
    }
    
    @Test
    fun `Phase1Progress calculates 100 percent when all tests completed`() {
        // Given
        val progress = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.COMPLETED),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.GRADED)
        )
        
        // Then
        assertEquals(100f, progress.completionPercentage, 0.01f)
    }
    
    @Test
    fun `Phase1Progress counts GRADED status as completed`() {
        // Given
        val progress = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.GRADED),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.GRADED)
        )
        
        // Then
        assertEquals(100f, progress.completionPercentage, 0.01f)
    }
    
    @Test
    fun `Phase1Progress does not count IN_PROGRESS as completed`() {
        // Given
        val progress = Phase1Progress(
            oirProgress = TestProgress(TestType.OIR, TestStatus.IN_PROGRESS),
            ppdtProgress = TestProgress(TestType.PPDT, TestStatus.NOT_ATTEMPTED)
        )
        
        // Then
        assertEquals(0f, progress.completionPercentage, 0.01f)
    }
    
    @Test
    fun `Phase2Progress calculates 0 percent when no tests completed`() {
        // Given
        val progress = Phase2Progress(
            psychologyProgress = TestProgress(TestType.TAT, TestStatus.NOT_ATTEMPTED),
            gtoProgress = TestProgress(TestType.GTO_GD, TestStatus.NOT_ATTEMPTED),
            interviewProgress = TestProgress(TestType.IO, TestStatus.NOT_ATTEMPTED)
        )
        
        // Then
        assertEquals(0f, progress.completionPercentage, 0.01f)
    }
    
    @Test
    fun `Phase2Progress calculates 33 percent when one test completed`() {
        // Given
        val progress = Phase2Progress(
            psychologyProgress = TestProgress(TestType.TAT, TestStatus.COMPLETED),
            gtoProgress = TestProgress(TestType.GTO_GD, TestStatus.NOT_ATTEMPTED),
            interviewProgress = TestProgress(TestType.IO, TestStatus.NOT_ATTEMPTED)
        )
        
        // Then
        assertEquals(33.33f, progress.completionPercentage, 0.1f)
    }
    
    @Test
    fun `Phase2Progress calculates 66 percent when two tests completed`() {
        // Given
        val progress = Phase2Progress(
            psychologyProgress = TestProgress(TestType.TAT, TestStatus.COMPLETED),
            gtoProgress = TestProgress(TestType.GTO_GD, TestStatus.GRADED),
            interviewProgress = TestProgress(TestType.IO, TestStatus.NOT_ATTEMPTED)
        )
        
        // Then
        assertEquals(66.66f, progress.completionPercentage, 0.1f)
    }
    
    @Test
    fun `Phase2Progress calculates 100 percent when all tests completed`() {
        // Given
        val progress = Phase2Progress(
            psychologyProgress = TestProgress(TestType.TAT, TestStatus.COMPLETED),
            gtoProgress = TestProgress(TestType.GTO_GD, TestStatus.GRADED),
            interviewProgress = TestProgress(TestType.IO, TestStatus.COMPLETED)
        )
        
        // Then
        assertEquals(100f, progress.completionPercentage, 0.01f)
    }
    
    @Test
    fun `TestProgress default status is NOT_ATTEMPTED`() {
        // Given
        val testProgress = TestProgress(testType = TestType.TAT)
        
        // Then
        assertEquals(TestStatus.NOT_ATTEMPTED, testProgress.status)
        assertNull(testProgress.lastAttemptDate)
        assertNull(testProgress.latestScore)
        assertFalse(testProgress.isPendingReview)
    }
    
    @Test
    fun `TestProgress stores last attempt date correctly`() {
        // Given
        val attemptDate = System.currentTimeMillis()
        val testProgress = TestProgress(
            testType = TestType.WAT,
            status = TestStatus.COMPLETED,
            lastAttemptDate = attemptDate
        )
        
        // Then
        assertEquals(attemptDate, testProgress.lastAttemptDate)
    }
    
    @Test
    fun `TestProgress stores latest score correctly`() {
        // Given
        val testProgress = TestProgress(
            testType = TestType.SRT,
            status = TestStatus.GRADED,
            latestScore = 85.5f
        )
        
        // Then
        assertEquals(85.5f, testProgress.latestScore)
    }
    
    @Test
    fun `TestProgress pending review flag works correctly`() {
        // Given
        val testProgress = TestProgress(
            testType = TestType.TAT,
            status = TestStatus.SUBMITTED_PENDING_REVIEW,
            isPendingReview = true
        )
        
        // Then
        assertTrue(testProgress.isPendingReview)
    }
    
    @Test
    fun `TestProgress can be created for all test types`() {
        // When/Then - should not throw exceptions
        TestProgress(testType = TestType.OIR)
        TestProgress(testType = TestType.PPDT)
        TestProgress(testType = TestType.TAT)
        TestProgress(testType = TestType.WAT)
        TestProgress(testType = TestType.SRT)
        TestProgress(testType = TestType.SD)
        TestProgress(testType = TestType.GTO_GD)
        TestProgress(testType = TestType.IO)
    }
}

