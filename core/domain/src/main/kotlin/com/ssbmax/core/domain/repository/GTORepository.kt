package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.gto.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for GTO (Group Testing Officer) tests
 * 
 * Handles all 8 GTO test types:
 * - Group Discussion (GD)
 * - Group Planning Exercise (GPE)
 * - Lecturette
 * - Progressive Group Task (PGT)
 * - Half Group Task (HGT)
 * - Group Obstacle Race (GOR)
 * - Individual Obstacles (IO)
 * - Command Task (CT)
 */
interface GTORepository {
    
    // ==================== Test Content Management ====================
    
    /**
     * Get a random GTO test of specified type
     */
    suspend fun getRandomTest(testType: GTOTestType): Result<GTOTest>
    
    /**
     * Get a specific test by ID
     */
    suspend fun getTestById(testId: String): Result<GTOTest>
    
    /**
     * Get random Group Discussion topic
     */
    suspend fun getRandomGDTopic(): Result<String>
    
    /**
     * Get 4 random Lecturette topics for selection
     */
    suspend fun getRandomLecturetteTopics(count: Int = 4): Result<List<String>>
    
    /**
     * Get random GPE scenario with image
     */
    suspend fun getRandomGPEScenario(): Result<GTOTest.GPETest>
    
    /**
     * Get obstacles for animation-based tests
     */
    suspend fun getObstaclesForTest(testType: GTOTestType): Result<List<ObstacleConfig>>
    
    // ==================== Submission Management ====================
    
    /**
     * Submit a GTO test response
     * Returns submission ID for navigation
     */
    suspend fun submitTest(submission: GTOSubmission): Result<String>
    
    /**
     * Get submission by ID
     */
    suspend fun getSubmission(submissionId: String): Result<GTOSubmission>
    
    /**
     * Observe submission updates (for real-time status changes)
     */
    fun observeSubmission(submissionId: String): Flow<GTOSubmission?>
    
    /**
     * Update submission status (used by worker)
     */
    suspend fun updateSubmissionStatus(
        submissionId: String,
        status: GTOSubmissionStatus
    ): Result<Unit>
    
    /**
     * Update submission with OLQ scores (after AI analysis)
     */
    suspend fun updateSubmissionOLQScores(
        submissionId: String,
        olqScores: Map<com.ssbmax.core.domain.model.interview.OLQ, com.ssbmax.core.domain.model.interview.OLQScore>
    ): Result<Unit>
    
    /**
     * Get all submissions for a user
     */
    suspend fun getUserSubmissions(
        userId: String,
        testType: GTOTestType? = null
    ): Result<List<GTOSubmission>>
    
    /**
     * Get submissions pending analysis (for worker)
     */
    suspend fun getPendingSubmissions(limit: Int = 10): Result<List<GTOSubmission>>
    
    // ==================== Progress Tracking ====================
    
    /**
     * Get user's GTO progress
     */
    suspend fun getUserProgress(userId: String): Result<GTOProgress>
    
    /**
     * Observe user's GTO progress (real-time updates)
     */
    fun observeUserProgress(userId: String): Flow<GTOProgress?>
    
    /**
     * Update user's progress after completing a test
     */
    suspend fun updateProgress(
        userId: String,
        completedTestType: GTOTestType
    ): Result<Unit>
    
    /**
     * Check if user can take a specific test (sequential access check)
     */
    suspend fun canUserTakeTest(
        userId: String,
        testType: GTOTestType
    ): Result<Boolean>
    
    /**
     * Get completed test types for a user
     */
    suspend fun getCompletedTests(userId: String): Result<List<GTOTestType>>
    
    /**
     * Get next available test for user (sequential order)
     */
    suspend fun getNextAvailableTest(userId: String): Result<GTOTestType?>
    
    // ==================== Test Usage Tracking ====================
    
    /**
     * Record test usage for subscription limits
     */
    suspend fun recordTestUsage(
        userId: String,
        testType: GTOTestType,
        submissionId: String
    ): Result<Unit>
    
    /**
     * Get test usage count for current month
     */
    suspend fun getTestUsageCount(
        userId: String,
        testType: GTOTestType
    ): Result<Int>
    
    /**
     * Reset monthly usage counters (called on 1st of each month)
     */
    suspend fun resetMonthlyUsage(userId: String): Result<Unit>
    
    // ==================== Results & Analysis ====================
    
    /**
     * Get test result with OLQ scores
     */
    suspend fun getTestResult(submissionId: String): Result<GTOResult>
    
    /**
     * Get all results for a user (for progress analytics)
     */
    suspend fun getUserResults(
        userId: String,
        testType: GTOTestType? = null
    ): Result<List<GTOResult>>
    
    /**
     * Get average OLQ scores across all GTO tests
     */
    suspend fun getAverageOLQScores(userId: String): Result<Map<com.ssbmax.core.domain.model.interview.OLQ, Float>>
    
    // ==================== Cache Management ====================
    
    /**
     * Cache test content locally for offline access
     */
    suspend fun cacheTestContent(testType: GTOTestType): Result<Unit>
    
    /**
     * Clear cached test content
     */
    suspend fun clearCache(testType: GTOTestType? = null): Result<Unit>
    
    /**
     * Check if content is cached
     */
    suspend fun isContentCached(testType: GTOTestType): Result<Boolean>
}
