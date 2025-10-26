package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.GradingQueueRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of GradingQueueRepository.
 * Provides real-time updates of grading queue and statistics.
 */
@Singleton
class GradingQueueRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : GradingQueueRepository {

    private val submissionsCollection = firestore.collection("submissions")
    private val usersCollection = firestore.collection("users")

    companion object {
        private const val TAG = "GradingQueueRepo"
        private const val FIELD_STATUS = "status"
        private const val FIELD_TEST_TYPE = "testType"
        private const val FIELD_BATCH_ID = "batchId"
        private const val FIELD_SUBMITTED_AT = "submittedAt"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_ID = "id"
    }

    override fun observePendingSubmissions(instructorId: String): Flow<List<GradingQueueItem>> = callbackFlow {
        Log.d(TAG, "Starting to observe pending submissions for instructor: $instructorId")
        
        val listener = submissionsCollection
            .whereIn(FIELD_STATUS, listOf(
                SubmissionStatus.SUBMITTED_PENDING_REVIEW.name,
                SubmissionStatus.UNDER_REVIEW.name
            ))
            .orderBy(FIELD_SUBMITTED_AT, Query.Direction.ASCENDING) // Oldest first
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing pending submissions", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val items = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        documentToGradingQueueItem(doc.data ?: return@mapNotNull null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing submission document: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                Log.d(TAG, "Loaded ${items.size} pending submissions")
                trySend(items)
            }
        
        awaitClose {
            Log.d(TAG, "Closing pending submissions observer")
            listener.remove()
        }
    }

    override fun observeSubmissionsByBatch(batchId: String): Flow<List<GradingQueueItem>> = callbackFlow {
        Log.d(TAG, "Observing submissions for batch: $batchId")
        
        val listener = submissionsCollection
            .whereEqualTo(FIELD_STATUS, SubmissionStatus.SUBMITTED_PENDING_REVIEW.name)
            .whereEqualTo(FIELD_BATCH_ID, batchId)
            .orderBy(FIELD_SUBMITTED_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing batch submissions", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val items = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        documentToGradingQueueItem(doc.data ?: return@mapNotNull null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing submission: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(items)
            }
        
        awaitClose { listener.remove() }
    }

    override fun observeSubmissionsByTestType(testType: TestType): Flow<List<GradingQueueItem>> = callbackFlow {
        Log.d(TAG, "Observing submissions for test type: $testType")
        
        val listener = submissionsCollection
            .whereEqualTo(FIELD_STATUS, SubmissionStatus.SUBMITTED_PENDING_REVIEW.name)
            .whereEqualTo(FIELD_TEST_TYPE, testType.name)
            .orderBy(FIELD_SUBMITTED_AT, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing test type submissions", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val items = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        documentToGradingQueueItem(doc.data ?: return@mapNotNull null)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing submission: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(items)
            }
        
        awaitClose { listener.remove() }
    }

    override fun observeGradingStats(instructorId: String): Flow<InstructorGradingStats> = callbackFlow {
        Log.d(TAG, "Observing grading stats for instructor: $instructorId")
        
        // Listen to all submissions to calculate stats
        val listener = submissionsCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing grading stats", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val documents = snapshot?.documents ?: emptyList()
                val now = System.currentTimeMillis()
                val oneDayAgo = now - (24 * 60 * 60 * 1000)
                val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)
                
                // Calculate stats
                val totalPending = documents.count { doc ->
                    val status = doc.getString(FIELD_STATUS)
                    status == SubmissionStatus.SUBMITTED_PENDING_REVIEW.name || 
                    status == SubmissionStatus.UNDER_REVIEW.name
                }
                
                val totalGraded = documents.count { 
                    it.getString(FIELD_STATUS) == SubmissionStatus.GRADED.name 
                }
                
                val todayGraded = documents.count { doc ->
                    val status = doc.getString(FIELD_STATUS)
                    val gradedAt = doc.getLong("gradedAt") ?: 0L
                    status == SubmissionStatus.GRADED.name && gradedAt >= oneDayAgo
                }
                
                val weekGraded = documents.count { doc ->
                    val status = doc.getString(FIELD_STATUS)
                    val gradedAt = doc.getLong("gradedAt") ?: 0L
                    status == SubmissionStatus.GRADED.name && gradedAt >= oneWeekAgo
                }
                
                // Group pending by test type
                val pendingByType = documents
                    .filter { doc ->
                        val status = doc.getString(FIELD_STATUS)
                        status == SubmissionStatus.SUBMITTED_PENDING_REVIEW.name
                    }
                    .groupBy { it.getString(FIELD_TEST_TYPE) ?: "" }
                    .mapNotNull { (typeStr, docs) ->
                        try {
                            TestType.valueOf(typeStr) to docs.size
                        } catch (e: Exception) {
                            null
                        }
                    }
                    .toMap()
                
                // Calculate average score given (simplified)
                val gradedWithScores = documents
                    .filter { it.getString(FIELD_STATUS) == SubmissionStatus.GRADED.name }
                    .mapNotNull { doc ->
                        (doc.get("data") as? Map<*, *>)?.let { data ->
                            (data["instructorScore"] as? Map<*, *>)?.get("overallScore") as? Number
                        }?.toFloat()
                    }
                
                val averageScore = if (gradedWithScores.isNotEmpty()) {
                    gradedWithScores.average().toFloat()
                } else {
                    0f
                }
                
                val stats = InstructorGradingStats(
                    totalPending = totalPending,
                    totalGraded = totalGraded,
                    averageGradingTimeMinutes = 0, // TODO: Calculate from timestamps
                    todayGraded = todayGraded,
                    weekGraded = weekGraded,
                    pendingByTestType = pendingByType,
                    averageScoreGiven = averageScore
                )
                
                trySend(stats)
            }
        
        awaitClose { listener.remove() }
    }

    override suspend fun markSubmissionUnderReview(
        submissionId: String,
        instructorId: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Marking submission $submissionId as under review by $instructorId")
            
            submissionsCollection.document(submissionId).update(
                mapOf(
                    FIELD_STATUS to SubmissionStatus.UNDER_REVIEW.name,
                    "gradedByInstructorId" to instructorId,
                    "gradingTimestamp" to System.currentTimeMillis()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking submission as under review", e)
            Result.failure(Exception("Failed to mark as under review: ${e.message}", e))
        }
    }

    override suspend fun releaseSubmissionFromReview(submissionId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Releasing submission $submissionId from review")
            
            submissionsCollection.document(submissionId).update(
                mapOf(
                    FIELD_STATUS to SubmissionStatus.SUBMITTED_PENDING_REVIEW.name,
                    "gradedByInstructorId" to null,
                    "gradingTimestamp" to null
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing submission from review", e)
            Result.failure(Exception("Failed to release from review: ${e.message}", e))
        }
    }

    /**
     * Convert Firestore document to GradingQueueItem.
     * Note: This is a non-suspend function to be used within snapshot listeners.
     * Student name should ideally be cached or fetched separately for better performance.
     */
    private fun documentToGradingQueueItem(data: Map<String, Any>): GradingQueueItem? {
        return try {
            val submissionId = data[FIELD_ID] as? String ?: return null
            val userId = data[FIELD_USER_ID] as? String ?: return null
            val testTypeStr = data[FIELD_TEST_TYPE] as? String ?: return null
            val statusStr = data[FIELD_STATUS] as? String ?: return null
            val submittedAt = (data[FIELD_SUBMITTED_AT] as? Number)?.toLong() ?: return null
            
            val testType = TestType.valueOf(testTypeStr)
            val status = SubmissionStatus.valueOf(statusStr)
            
            // TODO: Fetch student name from a user cache or separate query
            // For now, use the student ID as the display name
            // In a real app, you'd want to cache user profiles or fetch them separately
            val studentName = "Student $userId"
            
            // Extract AI score if present
            val submissionData = data["data"] as? Map<*, *>
            val aiScoreData = submissionData?.get("aiPreliminaryScore") as? Map<*, *>
            val aiScore = (aiScoreData?.get("overallScore") as? Number)?.toFloat()
            
            // Calculate priority based on time waiting
            val hoursWaiting = (System.currentTimeMillis() - submittedAt) / (1000 * 60 * 60)
            val priority = when {
                hoursWaiting > 72 -> GradingPriority.URGENT
                hoursWaiting > 48 -> GradingPriority.HIGH
                hoursWaiting > 24 -> GradingPriority.NORMAL
                else -> GradingPriority.LOW
            }
            
            GradingQueueItem(
                submissionId = submissionId,
                studentId = userId,
                studentName = studentName,
                testType = testType,
                testName = testType.name,
                submittedAt = submittedAt,
                status = status,
                priority = priority,
                batchName = data[FIELD_BATCH_ID] as? String,
                aiScore = aiScore,
                hasAISuggestions = aiScore != null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting document to GradingQueueItem", e)
            null
        }
    }
}

