package com.ssbmax.shared.domain.usecase.submission

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.repository.SubmissionRepository

/**
 * Use case for getting user's submissions
 */
class GetUserSubmissionsUseCase constructor(
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

