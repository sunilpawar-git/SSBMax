package com.ssbmax.core.domain.usecase.study

import com.ssbmax.core.domain.model.StudySession
import com.ssbmax.core.domain.repository.StudyProgressRepository
import javax.inject.Inject

/**
 * Use case for tracking study sessions
 * Handles starting, ending, and tracking time spent studying
 */
class TrackStudySessionUseCase @Inject constructor(
    private val studyProgressRepository: StudyProgressRepository
) {
    /**
     * Start a new study session
     * @param userId The user's unique identifier
     * @param materialId The material being studied
     * @return Result containing the created session
     */
    suspend fun startSession(userId: String, materialId: String): Result<StudySession> {
        return studyProgressRepository.startSession(userId, materialId)
    }

    /**
     * End an active study session
     * @param sessionId The session's unique identifier
     * @param progressIncrement Progress made during session (0-100)
     * @return Result indicating success or failure
     */
    suspend fun endSession(sessionId: String, progressIncrement: Float): Result<Unit> {
        return studyProgressRepository.endSession(sessionId, progressIncrement)
    }

    /**
     * Get the user's current active session
     * @param userId The user's unique identifier
     * @return Result containing active session or null
     */
    suspend fun getActiveSession(userId: String): Result<StudySession?> {
        return studyProgressRepository.getActiveSession(userId)
    }
}
