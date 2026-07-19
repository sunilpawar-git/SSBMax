package com.ssbmax.shared.domain.usecase.submission

import com.ssbmax.shared.domain.repository.SubmissionRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for observing submission changes in real-time
 * Useful for tracking grading updates
 */
class ObserveSubmissionUseCase constructor(
    private val submissionRepository: SubmissionRepository
) {
    operator fun invoke(submissionId: String): Flow<Map<String, Any>?> {
        return submissionRepository.observeSubmission(submissionId)
    }
}

