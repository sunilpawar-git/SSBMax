package com.ssbmax.shared.domain.repository

import com.ssbmax.shared.domain.model.SSBCategory
import com.ssbmax.shared.domain.model.SSBTest
import com.ssbmax.shared.domain.model.TestResult
import com.ssbmax.shared.domain.model.TestSession
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for SSB tests
 * Defines contract for test data operations
 */
interface TestRepository {
    
    /**
     * Get all tests for a specific category
     */
    fun getTests(category: SSBCategory): Flow<Result<List<SSBTest>>>
    
    /**
     * Get a specific test by ID
     */
    suspend fun getTestById(testId: String): Result<SSBTest>
    
    /**
     * Start a new test session
     */
    suspend fun startTestSession(testId: String, userId: String): Result<TestSession>
    
    /**
     * Submit test results
     */
    suspend fun submitTestResult(result: TestResult): Result<Unit>
    
    /**
     * Get all test results for a user
     */
    fun getTestResults(userId: String): Flow<Result<List<TestResult>>>
    
    /**
     * Get test results for a specific test
     */
    fun getTestResultsByTest(userId: String, testId: String): Flow<Result<List<TestResult>>>
}

