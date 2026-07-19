package com.ssbmax.shared.domain.usecase.study

import com.ssbmax.shared.domain.model.StudyProgress
import com.ssbmax.shared.domain.repository.StudyProgressRepository

/**
 * Use case for saving user's study progress
 * Handles progress updates, bookmarks, and completion tracking
 */
class SaveStudyProgressUseCase constructor(
    private val studyProgressRepository: StudyProgressRepository
) {
    /**
     * Save or update user's study progress
     * @param progress The progress data to save
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(progress: StudyProgress): Result<Unit> {
        return studyProgressRepository.saveProgress(progress)
    }
}
