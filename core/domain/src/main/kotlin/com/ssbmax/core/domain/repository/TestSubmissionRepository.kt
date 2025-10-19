package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.TestSubmission
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing test submissions
 */
interface TestSubmissionRepository {
    
    /**
     * Get submission by ID
     */
    suspend fun getSubmissionById(submissionId: String): Result<TestSubmission>
    
    /**
     * Get all submissions for a student
     */
    fun getSubmissionsForStudent(studentId: String): Flow<List<TestSubmission>>
    
    /**
     * Get submissions pending grading for an assessor
     */
    fun getPendingSubmissions(assessorId: String): Flow<List<TestSubmission>>
    
    /**
     * Submit a new test submission
     */
    suspend fun submitTest(submission: TestSubmission): Result<Unit>
    
    /**
     * Update submission (for grading)
     */
    suspend fun updateSubmission(submission: TestSubmission): Result<Unit>
    
    /**
     * Delete submission
     */
    suspend fun deleteSubmission(submissionId: String): Result<Unit>
}

