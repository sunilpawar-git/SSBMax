package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.StudyProgress
import com.ssbmax.core.domain.model.StudySession
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for tracking user's study progress
 * Manages reading progress, bookmarks, time spent, and completion status
 */
interface StudyProgressRepository {

    /**
     * Observe study progress for a material
     * Returns Flow for reactive updates
     */
    fun observeProgress(userId: String, materialId: String): Flow<StudyProgress?>

    /**
     * Save or update study progress
     */
    suspend fun saveProgress(progress: StudyProgress): Result<Unit>

    /**
     * Get study progress for a specific material
     */
    suspend fun getProgress(userId: String, materialId: String): Result<StudyProgress?>

    /**
     * Get all study progress for a user
     */
    suspend fun getAllProgress(userId: String): Result<List<StudyProgress>>

    /**
     * Delete study progress for a material
     */
    suspend fun deleteProgress(userId: String, materialId: String): Result<Unit>

    /**
     * Start a new study session
     */
    suspend fun startSession(userId: String, materialId: String): Result<StudySession>

    /**
     * End a study session and update progress
     */
    suspend fun endSession(sessionId: String, progressIncrement: Float): Result<Unit>

    /**
     * Get active study session
     */
    suspend fun getActiveSession(userId: String): Result<StudySession?>

    /**
     * Get study sessions for a material
     */
    suspend fun getSessionsForMaterial(userId: String, materialId: String): Result<List<StudySession>>
}
