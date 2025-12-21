package com.ssbmax.core.data.remote

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Facade repository for psychology test submissions (TAT, WAT, SRT, SDT).
 * Delegates to specialized repositories while maintaining backward compatibility.
 *
 * Refactored during Phase 2 to adhere to 300-line limit by delegating to:
 * - TATSubmissionRepository
 * - WATSubmissionRepository
 * - SRTSubmissionRepository
 * - SDTSubmissionRepository
 *
 * This class now serves as a unified API for psychology tests.
 */
@Singleton
class PsychTestSubmissionRepository @Inject constructor(
    private val tatRepo: TATSubmissionRepository,
    private val watRepo: WATSubmissionRepository,
    private val srtRepo: SRTSubmissionRepository,
    private val sdtRepo: SDTSubmissionRepository
) {

    // ===========================
    // TAT Operations (Delegated)
    // ===========================

    suspend fun submitTAT(submission: TATSubmission, batchId: String?): Result<String> =
        tatRepo.submitTAT(submission, batchId)

    suspend fun getTATSubmission(submissionId: String): Result<TATSubmission?> =
        tatRepo.getTATSubmission(submissionId)

    suspend fun getLatestTATSubmission(userId: String): Result<TATSubmission?> =
        tatRepo.getLatestTATSubmission(userId)

    suspend fun updateTATAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> =
        tatRepo.updateTATAnalysisStatus(submissionId, status)

    suspend fun updateTATOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        tatRepo.updateTATOLQResult(submissionId, olqResult)

    suspend fun getTATResult(submissionId: String): Result<OLQAnalysisResult?> =
        tatRepo.getTATResult(submissionId)

    fun observeTATSubmission(submissionId: String): Flow<TATSubmission?> =
        tatRepo.observeTATSubmission(submissionId)

    // ===========================
    // WAT Operations (Delegated)
    // ===========================

    suspend fun submitWAT(submission: WATSubmission, batchId: String?): Result<String> =
        watRepo.submitWAT(submission, batchId)

    suspend fun getWATSubmission(submissionId: String): Result<WATSubmission?> =
        watRepo.getWATSubmission(submissionId)

    suspend fun getLatestWATSubmission(userId: String): Result<WATSubmission?> =
        watRepo.getLatestWATSubmission(userId)

    suspend fun updateWATAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> =
        watRepo.updateWATAnalysisStatus(submissionId, status)

    suspend fun updateWATOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        watRepo.updateWATOLQResult(submissionId, olqResult)

    suspend fun getWATResult(submissionId: String): Result<OLQAnalysisResult?> =
        watRepo.getWATResult(submissionId)

    fun observeWATSubmission(submissionId: String): Flow<WATSubmission?> =
        watRepo.observeWATSubmission(submissionId)

    // ===========================
    // SRT Operations (Delegated)
    // ===========================

    suspend fun submitSRT(submission: SRTSubmission, batchId: String?): Result<String> =
        srtRepo.submitSRT(submission, batchId)

    suspend fun getSRTSubmission(submissionId: String): Result<SRTSubmission?> =
        srtRepo.getSRTSubmission(submissionId)

    suspend fun getLatestSRTSubmission(userId: String): Result<SRTSubmission?> =
        srtRepo.getLatestSRTSubmission(userId)

    suspend fun updateSRTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> =
        srtRepo.updateSRTAnalysisStatus(submissionId, status)

    suspend fun updateSRTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        srtRepo.updateSRTOLQResult(submissionId, olqResult)

    suspend fun getSRTResult(submissionId: String): Result<OLQAnalysisResult?> =
        srtRepo.getSRTResult(submissionId)

    fun observeSRTSubmission(submissionId: String): Flow<SRTSubmission?> =
        srtRepo.observeSRTSubmission(submissionId)

    // ===========================
    // SDT Operations (Delegated)
    // ===========================

    suspend fun submitSDT(submission: SDTSubmission, batchId: String?): Result<String> =
        sdtRepo.submitSDT(submission, batchId)

    suspend fun getSDTSubmission(submissionId: String): Result<SDTSubmission?> =
        sdtRepo.getSDTSubmission(submissionId)

    suspend fun getLatestSDTSubmission(userId: String): Result<SDTSubmission?> =
        sdtRepo.getLatestSDTSubmission(userId)

    suspend fun updateSDTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> =
        sdtRepo.updateSDTAnalysisStatus(submissionId, status)

    suspend fun updateSDTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        sdtRepo.updateSDTOLQResult(submissionId, olqResult)

    suspend fun getSDTResult(submissionId: String): Result<OLQAnalysisResult?> =
        sdtRepo.getSDTResult(submissionId)

    fun observeSDTSubmission(submissionId: String): Flow<SDTSubmission?> =
        sdtRepo.observeSDTSubmission(submissionId)
}
