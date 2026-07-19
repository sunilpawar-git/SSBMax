package com.ssbmax.shared.domain.usecase.submission

import com.ssbmax.shared.domain.model.SRTSubmission
import com.ssbmax.shared.domain.repository.SubmissionRepository

/**
 * Use case for submitting SRT test
 */
class SubmitSRTTestUseCase constructor(
    private val submissionRepository: SubmissionRepository
) {
    suspend operator fun invoke(
        submission: SRTSubmission,
        batchId: String? = null
    ): Result<String> {
        return submissionRepository.submitSRT(submission, batchId)
    }
}

