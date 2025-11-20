package com.ssbmax.core.domain.usecase.study

import com.ssbmax.core.domain.model.StudyProgress
import com.ssbmax.core.domain.repository.StudyProgressRepository
import javax.inject.Inject

/**
 * Use case for getting user's study progress
 * Retrieves progress data for tracking reading completion
 */
class GetStudyProgressUseCase @Inject constructor(
    private val studyProgressRepository: StudyProgressRepository
) {
    /**
     * Get user's progress for a specific material
     * @param userId The user's unique identifier
     * @param materialId The material's unique identifier
     * @return Result containing progress data or null if not started
     */
    suspend operator fun invoke(userId: String, materialId: String): Result<StudyProgress?> {
        return studyProgressRepository.getProgress(userId, materialId)
    }
}
