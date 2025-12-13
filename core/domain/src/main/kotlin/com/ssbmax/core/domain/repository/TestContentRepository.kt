package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.CacheStatus
import com.ssbmax.core.domain.model.GPEQuestion
import com.ssbmax.core.domain.model.OIRQuestion
import com.ssbmax.core.domain.model.PPDTQuestion
import com.ssbmax.core.domain.model.SDTQuestion
import com.ssbmax.core.domain.model.SRTSituation
import com.ssbmax.core.domain.model.TATQuestion
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.WATWord

// Type aliases for consistency
typealias WATQuestion = WATWord
typealias SRTQuestion = SRTSituation

/**
 * Repository for fetching test content from Firestore.
 * All test questions are stored in the cloud to prevent APK sideloading/extraction.
 * Content is never persisted locally - only cached in memory during active test sessions.
 */
interface TestContentRepository {
    
    /**
     * Fetch OIR test questions from Firestore
     * @param testId The specific test ID to load
     * @return Result with list of OIR questions or error
     * 
     * @deprecated Use getOIRTestQuestions() for cached implementation
     */
    suspend fun getOIRQuestions(testId: String): Result<List<OIRQuestion>>
    
    /**
     * Get OIR test questions from cache (50 questions with proper distribution)
     * Uses progressive caching strategy for optimal performance
     * 
     * @param count Number of questions to fetch (default 50)
     * @return Result with list of OIR questions or error
     */
    suspend fun getOIRTestQuestions(count: Int = 50, difficulty: String? = null): Result<List<OIRQuestion>>
    
    /**
     * Initialize OIR question cache
     * Downloads first batch of questions from Firestore
     * 
     * @return Result indicating success or failure
     */
    suspend fun initializeOIRCache(): Result<Unit>
    
    /**
     * Get OIR cache status
     * @return Cache status with statistics
     */
    suspend fun getOIRCacheStatus(): CacheStatus
    
    /**
     * Fetch PPDT test questions from Firestore
     * @param testId The specific test ID to load
     * @return Result with list of PPDT questions or error
     */
    suspend fun getPPDTQuestions(testId: String): Result<List<PPDTQuestion>>

    /**
     * Fetch GPE test questions from Firestore/Cache
     * @param testId The specific test ID to load
     * @return Result with list of GPE questions or error
     */
    suspend fun getGPEQuestions(testId: String): Result<List<GPEQuestion>>

    /**
     * Fetch TAT test questions from Firestore
     * @param testId The specific test ID to load
     * @return Result with list of TAT questions or error
     */
    suspend fun getTATQuestions(testId: String): Result<List<TATQuestion>>
    
    /**
     * Fetch WAT test words from Firestore/Cache
     * @param testId The specific test ID to load
     * @return Result with list of WAT words or error
     */
    suspend fun getWATQuestions(testId: String): Result<List<WATWord>>
    
    /**
     * Fetch SRT test situations from Firestore/Cache
     * @param testId The specific test ID to load
     * @return Result with list of SRT situations or error
     */
    suspend fun getSRTQuestions(testId: String): Result<List<SRTSituation>>
    
    /**
     * Fetch SDT test questions (4 predefined questions)
     * @param testId The specific test ID to load
     * @return Result with list of SDT questions or error
     */
    suspend fun getSDTQuestions(testId: String): Result<List<SDTQuestion>>
    
    /**
     * Check if user has active test session for given test
     * This prevents unauthorized access to test content
     * @param userId User ID
     * @param testId Test ID
     * @return True if user has valid session
     */
    suspend fun hasActiveTestSession(userId: String, testId: String): Result<Boolean>
    
    /**
     * Get random Group Discussion topic
     */
    suspend fun getRandomGDTopic(): Result<String>

    /**
     * Get 4 random Lecturette topics for selection
     */
    suspend fun getRandomLecturetteTopics(count: Int = 4): Result<List<String>>
    
    /**
     * Create test session for user
     * Required before accessing test content
     * @param userId User ID
     * @param testId Test ID
     * @param testType Test type
     * @return Session ID or error
     */
    suspend fun createTestSession(
        userId: String,
        testId: String,
        testType: TestType
    ): Result<String>
    
    /**
     * End test session
     * Invalidates access to test content
     * @param sessionId Session ID
     * @return Success or error
     */
    suspend fun endTestSession(sessionId: String): Result<Unit>
    
    /**
     * Clear in-memory cache of test content
     * Call this after test completion or on memory pressure
     */
    fun clearCache()
}

