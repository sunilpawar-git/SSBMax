package com.ssbmax.core.domain.usecase.submission

import com.ssbmax.core.domain.model.WATSubmission
import com.ssbmax.core.domain.repository.SubmissionRepository
import javax.inject.Inject

/**
 * Use case for submitting WAT test
 */
class SubmitWATTestUseCase @Inject constructor(
    private val submissionRepository: SubmissionRepository
) {
    suspend operator fun invoke(
        submission: WATSubmission,
        batchId: String? = null
    ): Result<String> {
        return submissionRepository.submitWAT(submission, batchId)
    }
}

