package com.ssbmax.shared.domain.usecase.submission

import com.ssbmax.shared.domain.model.WATSubmission
import com.ssbmax.shared.domain.repository.SubmissionRepository

/**
 * Use case for submitting WAT test
 */
class SubmitWATTestUseCase constructor(
    private val submissionRepository: SubmissionRepository
) {
    suspend operator fun invoke(
        submission: WATSubmission,
        batchId: String? = null
    ): Result<String> {
        return submissionRepository.submitWAT(submission, batchId)
    }
}

