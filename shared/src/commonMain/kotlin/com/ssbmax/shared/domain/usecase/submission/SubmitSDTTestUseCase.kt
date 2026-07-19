package com.ssbmax.shared.domain.usecase.submission

import com.ssbmax.shared.domain.model.SDTSubmission
import com.ssbmax.shared.domain.repository.SubmissionRepository

/**
 * Use case for submitting SDT test
 */
class SubmitSDTTestUseCase constructor(
    private val submissionRepository: SubmissionRepository
) {
    suspend operator fun invoke(
        submission: SDTSubmission,
        batchId: String? = null
    ): Result<String> {
        return submissionRepository.submitSDT(submission, batchId)
    }
}

