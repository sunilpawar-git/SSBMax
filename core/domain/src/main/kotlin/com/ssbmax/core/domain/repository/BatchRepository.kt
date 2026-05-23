package com.ssbmax.core.domain.repository

import com.ssbmax.core.domain.model.Batch
import com.ssbmax.core.domain.model.StudentPerformance
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing student batches/classes.
 * Handles Firestore integrations for instructor groups, invite joining codes, and member listings.
 */
interface BatchRepository {
    /**
     * Creates a new batch.
     */
    suspend fun createBatch(batch: Batch): Result<Unit>

    /**
     * Observes all batches created by a specific instructor.
     */
    fun getBatchesForInstructor(instructorId: String): Flow<Result<List<Batch>>>

    /**
     * Joins a student to a batch using a unique invite code.
     */
    suspend fun joinBatch(inviteCode: String, studentId: String): Result<Unit>

    /**
     * Observes students enrolled in a specific batch.
     */
    fun getStudentsInBatch(batchId: String): Flow<Result<List<StudentPerformance>>>

    /**
     * Observes a single batch by ID.
     */
    fun getBatch(batchId: String): Flow<Result<Batch>>
}
