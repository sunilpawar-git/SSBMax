package com.ssbmax.core.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.TestProgressRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Implementation of TestProgressRepository
 * Aggregates test submissions to calculate progress
 */
class TestProgressRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : TestProgressRepository {
    
    private val submissionsCollection = firestore.collection("submissions")
    
    override fun getPhase1Progress(userId: String): Flow<Phase1Progress> = callbackFlow {
        val listener = submissionsCollection
            .whereEqualTo("userId", userId)
            .whereIn("testType", listOf("OIR", "PPDT"))
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val submissions = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapDocumentToSubmission(doc.data ?: emptyMap())
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                // Get latest submission per test type
                val oirSubmission = submissions.firstOrNull { it.testType == "OIR" }
                val ppdtSubmission = submissions.firstOrNull { it.testType == "PPDT" }
                
                val progress = Phase1Progress(
                    oirProgress = createTestProgress(TestType.OIR, oirSubmission),
                    ppdtProgress = createTestProgress(TestType.PPDT, ppdtSubmission)
                )
                
                trySend(progress)
            }
        
        awaitClose { listener.remove() }
    }
    
    override fun getPhase2Progress(userId: String): Flow<Phase2Progress> = callbackFlow {
        val listener = submissionsCollection
            .whereEqualTo("userId", userId)
            .whereIn("testType", listOf("TAT", "WAT", "SRT", "SD", "GTO", "IO"))
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val submissions = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        mapDocumentToSubmission(doc.data ?: emptyMap())
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                // Group psychology tests (TAT, WAT, SRT, SD)
                val psychologySubmissions = submissions.filter { 
                    it.testType in listOf("TAT", "WAT", "SRT", "SD") 
                }
                val mostRecentPsychology = psychologySubmissions.maxByOrNull { it.submittedAt }
                
                val gtoSubmission = submissions.firstOrNull { it.testType == "GTO" }
                val interviewSubmission = submissions.firstOrNull { it.testType == "IO" }
                
                val progress = Phase2Progress(
                    psychologyProgress = createTestProgress(
                        TestType.TAT, // Use TAT as representative
                        mostRecentPsychology,
                        isPsychologyGroup = true
                    ),
                    gtoProgress = createTestProgress(TestType.GTO, gtoSubmission),
                    interviewProgress = createTestProgress(TestType.IO, interviewSubmission)
                )
                
                trySend(progress)
            }
        
        awaitClose { listener.remove() }
    }
    
    private fun createTestProgress(
        testType: TestType,
        submission: SubmissionData?,
        isPsychologyGroup: Boolean = false
    ): TestProgress {
        if (submission == null) {
            return TestProgress(
                testType = testType,
                status = TestStatus.NOT_ATTEMPTED
            )
        }
        
        val status = when (submission.status) {
            "SUBMITTED_PENDING_REVIEW" -> TestStatus.SUBMITTED_PENDING_REVIEW
            "GRADED" -> TestStatus.GRADED
            "COMPLETED" -> TestStatus.COMPLETED
            else -> TestStatus.NOT_ATTEMPTED
        }
        
        val isPending = submission.status == "SUBMITTED_PENDING_REVIEW"
        
        return TestProgress(
            testType = testType,
            status = status,
            lastAttemptDate = submission.submittedAt,
            latestScore = submission.score,
            isPendingReview = isPending
        )
    }
    
    private fun mapDocumentToSubmission(data: Map<String, Any>): SubmissionData {
        return SubmissionData(
            testType = data["testType"] as? String ?: "",
            status = data["status"] as? String ?: "",
            submittedAt = (data["submittedAt"] as? Long) ?: 0L,
            score = (data["score"] as? Number)?.toFloat()
        )
    }
    
    private data class SubmissionData(
        val testType: String,
        val status: String,
        val submittedAt: Long,
        val score: Float?
    )
}

