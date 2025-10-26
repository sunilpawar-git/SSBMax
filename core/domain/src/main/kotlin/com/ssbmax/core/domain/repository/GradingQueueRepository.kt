package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.GradingQueueItem
import com.ssbmax.core.domain.model.InstructorGradingStats
import com.ssbmax.core.domain.model.TestType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing the instructor's grading queue and statistics.
 * Provides real-time updates of pending submissions and grading metrics.
 */
interface GradingQueueRepository {

    /**
     * Observes the list of all pending submissions for grading.
     * @param instructorId The ID of the instructor (for filtering if needed).
     * @return A Flow emitting a list of [GradingQueueItem] sorted by priority.
     */
    fun observePendingSubmissions(instructorId: String): Flow<List<GradingQueueItem>>

    /**
     * Observes pending submissions filtered by specific test type.
     * @param instructorId The ID of the instructor.
     * @param testType The type of test to filter by.
     * @return A Flow emitting a filtered list of [GradingQueueItem].
     */
    fun observeSubmissionsByTestType(testType: TestType): Flow<List<GradingQueueItem>>

    /**
     * Observes pending submissions for a specific batch.
     * @param batchId The ID of the batch to filter by.
     * @return A Flow emitting submissions for the specified batch.
     */
    fun observeSubmissionsByBatch(batchId: String): Flow<List<GradingQueueItem>>

    /**
     * Observes the grading statistics for an instructor.
     * @param instructorId The ID of the instructor.
     * @return A Flow emitting [InstructorGradingStats].
     */
    fun observeGradingStats(instructorId: String): Flow<InstructorGradingStats>

    /**
     * Marks a submission as "under review" by an instructor.
     * This prevents other instructors from grading the same submission simultaneously.
     * 
     * @param submissionId The ID of the submission.
     * @param instructorId The ID of the instructor taking the submission for review.
     * @return A Result indicating success or failure.
     */
    suspend fun markSubmissionUnderReview(submissionId: String, instructorId: String): Result<Unit>

    /**
     * Releases a submission from "under review" status.
     * Used when an instructor decides not to grade a submission they previously claimed.
     * 
     * @param submissionId The ID of the submission.
     * @return A Result indicating success or failure.
     */
    suspend fun releaseSubmissionFromReview(submissionId: String): Result<Unit>
}

