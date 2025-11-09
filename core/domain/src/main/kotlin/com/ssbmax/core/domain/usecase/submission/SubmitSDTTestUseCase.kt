package com.ssbmax.core.domain.usecase.submission

import com.ssbmax.core.domain.model.SDTSubmission
import com.ssbmax.core.domain.repository.SubmissionRepository
import javax.inject.Inject

/**
 * Use case for submitting SDT test
 */
class SubmitSDTTestUseCase @Inject constructor(
    private val submissionRepository: SubmissionRepository
) {
    suspend operator fun invoke(
        submission: SDTSubmission,
        batchId: String? = null
    ): Result<String> {
        return submissionRepository.submitSDT(submission, batchId)
    }
}

