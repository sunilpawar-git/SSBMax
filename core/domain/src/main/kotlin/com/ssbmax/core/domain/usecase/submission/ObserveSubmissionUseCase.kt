package com.ssbmax.core.domain.usecase.submission

import com.ssbmax.core.domain.repository.SubmissionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for observing submission changes in real-time
 * Useful for tracking grading updates
 */
class ObserveSubmissionUseCase @Inject constructor(
    private val submissionRepository: SubmissionRepository
) {
    operator fun invoke(submissionId: String): Flow<Map<String, Any>?> {
        return submissionRepository.observeSubmission(submissionId)
    }
}

