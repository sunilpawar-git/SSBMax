package com.ssbmax.shared.domain.usecase.submission

import com.ssbmax.shared.domain.model.TATSubmission
import com.ssbmax.shared.domain.repository.SubmissionRepository

/**
 * Use case for submitting TAT test
 */
class SubmitTATTestUseCase constructor(
    private val submissionRepository: SubmissionRepository
) {
    suspend operator fun invoke(
        submission: TATSubmission,
        batchId: String? = null
    ): Result<String> {
        return submissionRepository.submitTAT(submission, batchId)
    }
}

