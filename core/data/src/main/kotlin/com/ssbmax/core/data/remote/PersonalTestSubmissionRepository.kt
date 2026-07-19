package com.ssbmax.core.data.remote

import com.ssbmax.shared.domain.model.*
import com.ssbmax.shared.domain.model.scoring.AnalysisStatus
import com.ssbmax.shared.domain.model.scoring.OLQAnalysisResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade repository for personal test submissions (PIQ, OIR, PPDT).
 * Delegates to focused data sources:
 *   - [PPDTPersonalSubmissionDataSource]
 *   - [OIRPersonalSubmissionDataSource]
 *   - [PIQPersonalSubmissionDataSource]
 */
@Singleton
class PersonalTestSubmissionRepository @Inject constructor(
    private val ppdtDataSource: PPDTPersonalSubmissionDataSource,
    private val oirDataSource: OIRPersonalSubmissionDataSource,
    private val piqDataSource: PIQPersonalSubmissionDataSource,
) {
    // ===========================
    // PPDT Operations
    // ===========================

    suspend fun submitPPDT(submission: PPDTSubmission, batchId: String?): Result<String> =
        ppdtDataSource.submitPPDT(submission, batchId)

    suspend fun getPPDTSubmission(submissionId: String): Result<PPDTSubmission?> =
        ppdtDataSource.getPPDTSubmission(submissionId)

    suspend fun getLatestPPDTSubmission(userId: String): Result<PPDTSubmission?> =
        ppdtDataSource.getLatestPPDTSubmission(userId)

    suspend fun updatePPDTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> =
        ppdtDataSource.updatePPDTAnalysisStatus(submissionId, status)

    suspend fun updatePPDTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        ppdtDataSource.updatePPDTOLQResult(submissionId, olqResult)

    suspend fun getPPDTResult(submissionId: String): Result<OLQAnalysisResult?> =
        ppdtDataSource.getPPDTResult(submissionId)

    fun observePPDTSubmission(submissionId: String): Flow<PPDTSubmission?> =
        ppdtDataSource.observePPDTSubmission(submissionId)

    // ===========================
    // OIR Operations
    // ===========================

    suspend fun submitOIR(submission: OIRSubmission, batchId: String?): Result<String> =
        oirDataSource.submitOIR(submission, batchId)

    suspend fun getLatestOIRSubmission(userId: String): Result<OIRSubmission?> =
        oirDataSource.getLatestOIRSubmission(userId)

    // ===========================
    // PIQ Operations
    // ===========================

    suspend fun submitPIQ(submission: PIQSubmission, batchId: String?): Result<String> =
        piqDataSource.submitPIQ(submission, batchId)

    suspend fun getLatestPIQSubmission(userId: String): Result<PIQSubmission?> =
        piqDataSource.getLatestPIQSubmission(userId)
}
