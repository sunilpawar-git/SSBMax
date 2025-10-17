package com.ssbmax.core.domain.usecase.submission

import com.ssbmax.core.domain.model.SRTSubmission
import com.ssbmax.core.domain.repository.SubmissionRepository
import javax.inject.Inject

/**
 * Use case for submitting SRT test
 */
class SubmitSRTTestUseCase @Inject constructor(
    private val submissionRepository: SubmissionRepository
) {
    suspend operator fun invoke(
        submission: SRTSubmission,
        batchId: String? = null
    ): Result<String> {
        return submissionRepository.submitSRT(submission, batchId)
    }
}

