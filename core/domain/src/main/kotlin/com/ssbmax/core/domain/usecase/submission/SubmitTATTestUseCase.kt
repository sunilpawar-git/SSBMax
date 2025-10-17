package com.ssbmax.core.domain.usecase.submission

import com.ssbmax.core.domain.model.TATSubmission
import com.ssbmax.core.domain.repository.SubmissionRepository
import javax.inject.Inject

/**
 * Use case for submitting TAT test
 */
class SubmitTATTestUseCase @Inject constructor(
    private val submissionRepository: SubmissionRepository
) {
    suspend operator fun invoke(
        submission: TATSubmission,
        batchId: String? = null
    ): Result<String> {
        return submissionRepository.submitTAT(submission, batchId)
    }
}

