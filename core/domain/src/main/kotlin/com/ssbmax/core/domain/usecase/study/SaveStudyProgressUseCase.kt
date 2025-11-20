package com.ssbmax.core.domain.usecase.study

import com.ssbmax.core.domain.model.StudyProgress
import com.ssbmax.core.domain.repository.StudyProgressRepository
import javax.inject.Inject

/**
 * Use case for saving user's study progress
 * Handles progress updates, bookmarks, and completion tracking
 */
class SaveStudyProgressUseCase @Inject constructor(
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
