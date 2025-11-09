package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for test submissions
 */
interface SubmissionRepository {

    /**
     * Submit TAT test
     */
    suspend fun submitTAT(submission: TATSubmission, batchId: String? = null): Result<String>

    /**
     * Submit WAT test
     */
    suspend fun submitWAT(submission: WATSubmission, batchId: String? = null): Result<String>

    /**
     * Submit SRT test
     */
    suspend fun submitSRT(submission: SRTSubmission, batchId: String? = null): Result<String>

    /**
     * Submit SDT test
     */
    suspend fun submitSDT(submission: SDTSubmission, batchId: String? = null): Result<String>

    /**
     * Submit PPDT test
     */
    suspend fun submitPPDT(submission: PPDTSubmission, batchId: String? = null): Result<String>

    /**
     * Submit OIR test
     */
    suspend fun submitOIR(submission: OIRSubmission, batchId: String? = null): Result<String>

    /**
     * Submit PIQ test
     */
    suspend fun submitPIQ(submission: PIQSubmission, batchId: String? = null): Result<String>

    /**
     * Get submission by ID
     */
    suspend fun getSubmission(submissionId: String): Result<Map<String, Any>?>

    /**
     * Get user's submissions
     */
    suspend fun getUserSubmissions(userId: String, limit: Int = 50): Result<List<Map<String, Any>>>

    /**
     * Get user's submissions by test type
     */
    suspend fun getUserSubmissionsByTestType(
        userId: String,
        testType: TestType,
        limit: Int = 20
    ): Result<List<Map<String, Any>>>

    /**
     * Observe submission changes in real-time
     */
    fun observeSubmission(submissionId: String): Flow<Map<String, Any>?>

    /**
     * Observe user's submissions in real-time
     */
    fun observeUserSubmissions(userId: String, limit: Int = 50): Flow<List<Map<String, Any>>>

    /**
     * Update submission status
     */
    suspend fun updateSubmissionStatus(
        submissionId: String,
        status: SubmissionStatus
    ): Result<Unit>
}

