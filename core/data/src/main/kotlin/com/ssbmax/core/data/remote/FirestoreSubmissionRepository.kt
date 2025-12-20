package com.ssbmax.core.data.remote

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.SubmissionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore Submission Repository Facade
 * 
 * Delegates all operations to specialized domain repositories:
 * - Common: Generic CRUD, observation
 * - Archive: Archival operations
 * - GTO: Group Testing Officer tests (GPE, GD, Lecturette)
 * - Personal: Personal tests (PIQ, OIR, PPDT)
 * - Psych: Psychology tests (TAT, WAT, SRT, SDT)
 * 
 * Refactored from monolithic 1,900+ lines to pure facade.
 */
@Singleton
class FirestoreSubmissionRepository @Inject constructor(
    private val commonRepo: CommonSubmissionRepository,
    private val archiveRepo: SubmissionArchiveRepository,
    private val gtoRepo: GTOSubmissionRepository,
    private val personalRepo: PersonalTestSubmissionRepository,
    private val psychRepo: PsychTestSubmissionRepository
) : SubmissionRepository {

    // ===========================
    // GTO Methods (GTOSubmissionRepository)
    // ===========================

    override suspend fun submitGPE(
        submission: GTOSubmission.GPESubmission, 
        batchId: String?
    ): Result<String> = gtoRepo.submitGPE(submission, batchId)

    override suspend fun submitGD(
        submission: GTOSubmission.GDSubmission, 
        batchId: String?
    ): Result<String> = gtoRepo.submitGD(submission, batchId)

    override suspend fun submitLecturette(
        submission: GTOSubmission.LecturetteSubmission, 
        batchId: String?
    ): Result<String> = gtoRepo.submitLecturette(submission, batchId)

    // ===========================
    // Common Methods (CommonSubmissionRepository)
    // ===========================

    override suspend fun getSubmission(submissionId: String): Result<Map<String, Any>?> =
        commonRepo.getSubmission(submissionId)

    override suspend fun getUserSubmissions(
        userId: String,
        limit: Int
    ): Result<List<Map<String, Any>>> = commonRepo.getUserSubmissions(userId, limit)

    override suspend fun getUserSubmissionsByTestType(
        userId: String,
        testType: TestType,
        limit: Int
    ): Result<List<Map<String, Any>>> = commonRepo.getUserSubmissionsByTestType(userId, testType, limit)

    override fun observeSubmission(submissionId: String): Flow<Map<String, Any>?> =
        commonRepo.observeSubmission(submissionId)

    override fun observeUserSubmissions(userId: String, limit: Int): Flow<List<Map<String, Any>>> =
        commonRepo.observeUserSubmissions(userId, limit)

    override suspend fun updateSubmissionStatus(
        submissionId: String,
        status: SubmissionStatus
    ): Result<Unit> = commonRepo.updateSubmissionStatus(submissionId, status)

    // Custom methods (not in interface, but kept for compatibility)
    suspend fun deleteSubmission(submissionId: String): Result<Unit> = 
        commonRepo.deleteSubmission(submissionId)

    suspend fun updateWithInstructorGrading(
        submissionId: String,
        instructorId: String,
        status: SubmissionStatus = SubmissionStatus.GRADED
    ): Result<Unit> = commonRepo.updateWithInstructorGrading(submissionId, instructorId, status)

    suspend fun getPendingSubmissionsForInstructor(
        batchId: String? = null,
        limit: Int = 100
    ): Result<List<Map<String, Any>>> = commonRepo.getPendingSubmissionsForInstructor(batchId, limit)

    // ===========================
    // Personal Test Methods (PersonalTestSubmissionRepository)
    // ===========================

    override suspend fun submitPPDT(submission: PPDTSubmission, batchId: String?): Result<String> =
        personalRepo.submitPPDT(submission, batchId)

    override suspend fun submitOIR(submission: OIRSubmission, batchId: String?): Result<String> =
        personalRepo.submitOIR(submission, batchId)

    override suspend fun submitPIQ(submission: PIQSubmission, batchId: String?): Result<String> =
        personalRepo.submitPIQ(submission, batchId)
        
    override suspend fun getLatestPIQSubmission(userId: String): Result<PIQSubmission?> =
        personalRepo.getLatestPIQSubmission(userId)

    override suspend fun getLatestOIRSubmission(userId: String): Result<OIRSubmission?> =
        personalRepo.getLatestOIRSubmission(userId)

    override suspend fun getLatestPPDTSubmission(userId: String): Result<PPDTSubmission?> =
        personalRepo.getLatestPPDTSubmission(userId)

    override suspend fun getPPDTSubmission(submissionId: String): Result<PPDTSubmission?> =
        personalRepo.getPPDTSubmission(submissionId)

    override suspend fun updatePPDTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> =
        personalRepo.updatePPDTAnalysisStatus(submissionId, status)

    override suspend fun updatePPDTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        personalRepo.updatePPDTOLQResult(submissionId, olqResult)

    override fun observePPDTSubmission(submissionId: String): Flow<PPDTSubmission?> =
        personalRepo.observePPDTSubmission(submissionId)

    // ===========================
    // Psych Test Methods (PsychTestSubmissionRepository)
    // ===========================

    // TAT
    override suspend fun submitTAT(submission: TATSubmission, batchId: String?): Result<String> =
        psychRepo.submitTAT(submission, batchId)

    override suspend fun getTATSubmission(submissionId: String): Result<TATSubmission?> =
        psychRepo.getTATSubmission(submissionId)

    override suspend fun updateTATAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> =
        psychRepo.updateTATAnalysisStatus(submissionId, status)

    override suspend fun updateTATOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        psychRepo.updateTATOLQResult(submissionId, olqResult)

    override fun observeTATSubmission(submissionId: String): Flow<TATSubmission?> =
        psychRepo.observeTATSubmission(submissionId)

    // WAT
    override suspend fun submitWAT(submission: WATSubmission, batchId: String?): Result<String> =
        psychRepo.submitWAT(submission, batchId)
        
    override suspend fun getWATSubmission(submissionId: String): Result<WATSubmission?> =
        psychRepo.getWATSubmission(submissionId)

    override suspend fun updateWATAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> =
        psychRepo.updateWATAnalysisStatus(submissionId, status)

    override suspend fun updateWATOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        psychRepo.updateWATOLQResult(submissionId, olqResult)

    override fun observeWATSubmission(submissionId: String): Flow<WATSubmission?> =
        psychRepo.observeWATSubmission(submissionId)

    // SRT
    override suspend fun submitSRT(submission: SRTSubmission, batchId: String?): Result<String> =
        psychRepo.submitSRT(submission, batchId)
        
    override suspend fun getSRTSubmission(submissionId: String): Result<SRTSubmission?> =
        psychRepo.getSRTSubmission(submissionId)

    override suspend fun updateSRTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> =
        psychRepo.updateSRTAnalysisStatus(submissionId, status)

    override suspend fun updateSRTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        psychRepo.updateSRTOLQResult(submissionId, olqResult)

    override fun observeSRTSubmission(submissionId: String): Flow<SRTSubmission?> =
        psychRepo.observeSRTSubmission(submissionId)

    // SDT
    override suspend fun submitSDT(submission: SDTSubmission, batchId: String?): Result<String> =
        psychRepo.submitSDT(submission, batchId)
        
    override suspend fun getSDTSubmission(submissionId: String): Result<SDTSubmission?> =
        psychRepo.getSDTSubmission(submissionId)

    override suspend fun updateSDTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> =
        psychRepo.updateSDTAnalysisStatus(submissionId, status)

    override suspend fun updateSDTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> =
        psychRepo.updateSDTOLQResult(submissionId, olqResult)

    override fun observeSDTSubmission(submissionId: String): Flow<SDTSubmission?> =
        psychRepo.observeSDTSubmission(submissionId)

    // ===========================
    // Archival Methods (SubmissionArchiveRepository)
    // ===========================

    override suspend fun archiveOldSubmissions(beforeTimestamp: Long): Result<Int> =
        archiveRepo.archiveOldSubmissions(beforeTimestamp)
}
