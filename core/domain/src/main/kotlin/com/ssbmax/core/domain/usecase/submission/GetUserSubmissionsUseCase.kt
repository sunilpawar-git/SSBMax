package com.ssbmax.core.domain.usecase.submission

import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.repository.SubmissionRepository
import javax.inject.Inject

/**
 * Use case for getting user's submissions
 */
class GetUserSubmissionsUseCase @Inject constructor(
    private val submissionRepository: SubmissionRepository
) {
    /**
     * Get all submissions for a user
     */
    suspend operator fun invoke(
        userId: String,
        limit: Int = 50
    ): Result<List<Map<String, Any>>> {
        return submissionRepository.getUserSubmissions(userId, limit)
    }

    /**
     * Get submissions by test type
     */
    suspend fun byTestType(
        userId: String,
        testType: TestType,
        limit: Int = 20
    ): Result<List<Map<String, Any>>> {
        return submissionRepository.getUserSubmissionsByTestType(userId, testType, limit)
    }
}

