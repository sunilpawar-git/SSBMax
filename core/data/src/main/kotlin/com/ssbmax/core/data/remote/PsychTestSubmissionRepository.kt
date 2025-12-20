package com.ssbmax.core.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for psychology test submissions (TAT, WAT, SRT, SDT).
 * Handles submission, retrieval, observation, and OLQ updates for projective tests.
 * 
 * Extracted from FirestoreSubmissionRepository during Phase 9 refactoring.
 */
@Singleton
class PsychTestSubmissionRepository @Inject constructor() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val submissionsCollection = firestore.collection("submissions")
    
    companion object {
        private const val TAG = "PsychTestRepo"
        private const val FIELD_ID = "id"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TEST_ID = "testId"
        private const val FIELD_TEST_TYPE = "testType"
        private const val FIELD_STATUS = "status"
        private const val FIELD_SUBMITTED_AT = "submittedAt"
        private const val FIELD_GRADED_BY_INSTRUCTOR_ID = "gradedByInstructorId"
        private const val FIELD_GRADING_TIMESTAMP = "gradingTimestamp"
        private const val FIELD_BATCH_ID = "batchId"
        private const val FIELD_DATA = "data"
    }
    
    // ===========================
    // TAT Operations
    // ===========================
    
    suspend fun submitTAT(submission: TATSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.TAT.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toFirestoreMap()
            )
            submissionsCollection.document(submission.id).set(submissionMap).await()
            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit TAT: ${e.message}", e))
        }
    }
    
    suspend fun getTATSubmission(submissionId: String): Result<TATSubmission?> {
        return try {
            val document = submissionsCollection.document(submissionId).get().await()
            if (!document.exists()) return Result.success(null)
            val data = document.get(FIELD_DATA) as? Map<*, *> ?: return Result.success(null)
            Result.success(parseTATSubmission(data))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch TAT submission: ${e.message}", e))
        }
    }
    
    suspend fun updateTATAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> {
        return try {
            Log.d(TAG, "📝 Updating TAT analysis status: $submissionId -> $status")
            submissionsCollection.document(submissionId)
                .update("data.analysisStatus", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update TAT analysis status: ${e.message}", e))
        }
    }
    
    suspend fun updateTATOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> {
        return updateOLQResult(submissionId, olqResult)
    }
    
    fun observeTATSubmission(submissionId: String): Flow<TATSubmission?> = callbackFlow {
        val regressionFilter = OLQRegressionFilter()
        val listener = submissionsCollection.document(submissionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                try {
                    val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                    if (data == null || regressionFilter.shouldFilterSnapshot(data, snapshot.metadata, "TAT", submissionId)) {
                        if (data == null) trySend(null)
                        return@addSnapshotListener
                    }
                    trySend(parseTATSubmission(data))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing TAT submission", e)
                }
            }
        awaitClose { listener.remove() }
    }
    
    // ===========================
    // WAT Operations
    // ===========================
    
    suspend fun submitWAT(submission: WATSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.WAT.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toFirestoreMap()
            )
            submissionsCollection.document(submission.id).set(submissionMap).await()
            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit WAT: ${e.message}", e))
        }
    }
    
    suspend fun getWATSubmission(submissionId: String): Result<WATSubmission?> {
        return try {
            val document = submissionsCollection.document(submissionId).get().await()
            if (!document.exists()) return Result.success(null)
            val data = document.get(FIELD_DATA) as? Map<*, *> ?: return Result.success(null)
            Result.success(parseWATSubmission(data))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch WAT submission: ${e.message}", e))
        }
    }
    
    suspend fun updateWATAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId).update("data.analysisStatus", status.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update WAT status: ${e.message}", e))
        }
    }
    
    suspend fun updateWATOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> {
        return updateOLQResult(submissionId, olqResult)
    }
    
    fun observeWATSubmission(submissionId: String): Flow<WATSubmission?> = callbackFlow {
        val regressionFilter = OLQRegressionFilter()
        val listener = submissionsCollection.document(submissionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                try {
                    val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                    if (data == null || regressionFilter.shouldFilterSnapshot(data, snapshot.metadata, "WAT", submissionId)) {
                        if (data == null) trySend(null)
                        return@addSnapshotListener
                    }
                    trySend(parseWATSubmission(data))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing WAT submission", e)
                }
            }
        awaitClose { listener.remove() }
    }
    
    // ===========================
    // SRT Operations
    // ===========================
    
    suspend fun submitSRT(submission: SRTSubmission, batchId: String?): Result<String> {
        return try {
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.SRT.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toFirestoreMap()
            )
            submissionsCollection.document(submission.id).set(submissionMap).await()
            Result.success(submission.id)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit SRT: ${e.message}", e))
        }
    }
    
    suspend fun getSRTSubmission(submissionId: String): Result<SRTSubmission?> {
        return try {
            val document = submissionsCollection.document(submissionId).get().await()
            if (!document.exists()) return Result.success(null)
            val data = document.get(FIELD_DATA) as? Map<*, *> ?: return Result.success(null)
            Result.success(parseSRTSubmission(data))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch SRT submission: ${e.message}", e))
        }
    }
    
    suspend fun updateSRTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId).update("data.analysisStatus", status.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update SRT status: ${e.message}", e))
        }
    }
    
    suspend fun updateSRTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> {
        return updateOLQResult(submissionId, olqResult)
    }
    
    fun observeSRTSubmission(submissionId: String): Flow<SRTSubmission?> = callbackFlow {
        val regressionFilter = OLQRegressionFilter()
        val listener = submissionsCollection.document(submissionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                try {
                    val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                    if (data == null || regressionFilter.shouldFilterSnapshot(data, snapshot.metadata, "SRT", submissionId)) {
                        if (data == null) trySend(null)
                        return@addSnapshotListener
                    }
                    trySend(parseSRTSubmission(data))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing SRT submission", e)
                }
            }
        awaitClose { listener.remove() }
    }
    
    // ===========================
    // SDT Operations
    // ===========================
    
    suspend fun submitSDT(submission: SDTSubmission, batchId: String?): Result<String> {
        return try {
            Log.d(TAG, "☁️ Firestore SDT: Preparing submission...")
            val submissionMap = mapOf(
                FIELD_ID to submission.id,
                FIELD_USER_ID to submission.userId,
                FIELD_TEST_ID to submission.testId,
                FIELD_TEST_TYPE to TestType.SD.name,
                FIELD_STATUS to submission.status.name,
                FIELD_SUBMITTED_AT to submission.submittedAt,
                FIELD_GRADED_BY_INSTRUCTOR_ID to submission.gradedByInstructorId,
                FIELD_GRADING_TIMESTAMP to submission.gradingTimestamp,
                FIELD_BATCH_ID to batchId,
                FIELD_DATA to submission.toFirestoreMap()
            )
            submissionsCollection.document(submission.id).set(submissionMap).await()
            Log.d(TAG, "✅ Firestore SDT: Successfully written!")
            Result.success(submission.id)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firestore SDT: Failed - ${e.message}", e)
            Result.failure(Exception("Failed to submit SDT: ${e.message}", e))
        }
    }
    
    suspend fun getSDTSubmission(submissionId: String): Result<SDTSubmission?> {
        return try {
            val document = submissionsCollection.document(submissionId).get().await()
            if (!document.exists()) return Result.success(null)
            val data = document.get(FIELD_DATA) as? Map<*, *> ?: return Result.success(null)
            Result.success(parseSDTSubmission(data))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch SDT submission: ${e.message}", e))
        }
    }
    
    suspend fun updateSDTAnalysisStatus(submissionId: String, status: AnalysisStatus): Result<Unit> {
        return try {
            submissionsCollection.document(submissionId).update("data.analysisStatus", status.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update SDT status: ${e.message}", e))
        }
    }
    
    suspend fun updateSDTOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> {
        return updateOLQResult(submissionId, olqResult)
    }
    
    fun observeSDTSubmission(submissionId: String): Flow<SDTSubmission?> = callbackFlow {
        val regressionFilter = OLQRegressionFilter()
        val listener = submissionsCollection.document(submissionId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                try {
                    val data = snapshot.get(FIELD_DATA) as? Map<*, *>
                    if (data == null || regressionFilter.shouldFilterSnapshot(data, snapshot.metadata, "SDT", submissionId)) {
                        if (data == null) trySend(null)
                        return@addSnapshotListener
                    }
                    trySend(parseSDTSubmission(data))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing SDT submission", e)
                }
            }
        awaitClose { listener.remove() }
    }
    
    // ===========================
    // Shared Helper Methods
    // ===========================
    
    private suspend fun updateOLQResult(submissionId: String, olqResult: OLQAnalysisResult): Result<Unit> {
        return try {
            val olqResultMap = OLQMapper.toFirestoreMap(olqResult)
            submissionsCollection.document(submissionId)
                .update(
                    mapOf(
                        "$FIELD_DATA.olqResult" to olqResultMap,
                        "$FIELD_DATA.analysisStatus" to SubmissionConstants.ANALYSIS_STATUS_COMPLETED
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to update OLQ result: ${e.message}", e))
        }
    }
    
    // ===========================
    // Parsing Methods
    // ===========================
    
    private fun parseTATSubmission(data: Map<*, *>): TATSubmission {
        val storiesList = data["stories"] as? List<*> ?: emptyList<Any>()
        val stories = storiesList.mapNotNull { storyData ->
            (storyData as? Map<*, *>)?.let {
                TATStoryResponse(
                    questionId = it["questionId"] as? String ?: "",
                    story = it["story"] as? String ?: "",
                    charactersCount = (it["charactersCount"] as? Number)?.toInt() ?: 0,
                    viewingTimeTakenSeconds = (it["viewingTimeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    writingTimeTakenSeconds = (it["writingTimeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (it["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }
        }

        val instructorScoreMap = data["instructorScore"] as? Map<*, *>
        val instructorScore = instructorScoreMap?.let {
            TATInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                thematicPerceptionScore = (it["thematicPerceptionScore"] as? Number)?.toFloat() ?: 0f,
                imaginationScore = (it["imaginationScore"] as? Number)?.toFloat() ?: 0f,
                characterDepictionScore = (it["characterDepictionScore"] as? Number)?.toFloat() ?: 0f,
                emotionalToneScore = (it["emotionalToneScore"] as? Number)?.toFloat() ?: 0f,
                narrativeStructureScore = (it["narrativeStructureScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                storyWiseComments = (it["storyWiseComments"] as? Map<*, *>)?.mapNotNull { (k, v) ->
                    (k as? String)?.let { key -> key to (v as? String ?: "") }
                }?.toMap() ?: emptyMap(),
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
            )
        }

        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)
        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) AnalysisStatus.valueOf(analysisStatusStr)
            else AnalysisStatus.PENDING_ANALYSIS
        } catch (e: Exception) { AnalysisStatus.PENDING_ANALYSIS }

        return TATSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            stories = stories,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = try { SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW") }
                     catch (e: Exception) { SubmissionStatus.SUBMITTED_PENDING_REVIEW },
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    private fun parseWATSubmission(data: Map<*, *>): WATSubmission {
        val responsesList = data["responses"] as? List<*> ?: emptyList<Any>()
        val responses = responsesList.mapNotNull { responseData ->
            (responseData as? Map<*, *>)?.let {
                WATWordResponse(
                    wordId = it["wordId"] as? String ?: "",
                    word = it["word"] as? String ?: "",
                    response = it["response"] as? String ?: "",
                    timeTakenSeconds = (it["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (it["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isSkipped = it["isSkipped"] as? Boolean ?: false
                )
            }
        }

        val instructorScoreMap = data["instructorScore"] as? Map<*, *>
        val instructorScore = instructorScoreMap?.let {
            WATInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                positivityScore = (it["positivityScore"] as? Number)?.toFloat() ?: 0f,
                creativityScore = (it["creativityScore"] as? Number)?.toFloat() ?: 0f,
                speedScore = (it["speedScore"] as? Number)?.toFloat() ?: 0f,
                relevanceScore = (it["relevanceScore"] as? Number)?.toFloat() ?: 0f,
                emotionalMaturityScore = (it["emotionalMaturityScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                flaggedResponses = (it["flaggedResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                notableResponses = (it["notableResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
            )
        }

        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)
        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) AnalysisStatus.valueOf(analysisStatusStr)
            else AnalysisStatus.PENDING_ANALYSIS
        } catch (e: Exception) { AnalysisStatus.PENDING_ANALYSIS }

        return WATSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            responses = responses,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = try { SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW") }
                     catch (e: Exception) { SubmissionStatus.SUBMITTED_PENDING_REVIEW },
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    private fun parseSRTSubmission(data: Map<*, *>): SRTSubmission {
        val responsesList = data["responses"] as? List<*> ?: emptyList<Any>()
        val responses = responsesList.mapNotNull { responseData ->
            (responseData as? Map<*, *>)?.let {
                SRTSituationResponse(
                    situationId = it["situationId"] as? String ?: "",
                    situation = it["situation"] as? String ?: "",
                    response = it["response"] as? String ?: "",
                    charactersCount = (it["charactersCount"] as? Number)?.toInt() ?: 0,
                    timeTakenSeconds = (it["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (it["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isSkipped = it["isSkipped"] as? Boolean ?: false
                )
            }
        }

        val instructorScoreMap = data["instructorScore"] as? Map<*, *>
        val instructorScore = instructorScoreMap?.let {
            val categoryWiseCommentsMap = it["categoryWiseComments"] as? Map<*, *> ?: emptyMap<Any, Any>()
            val categoryWiseComments = categoryWiseCommentsMap.mapNotNull { (k, v) ->
                try {
                    val category = SRTCategory.valueOf(k as? String ?: "GENERAL")
                    category to (v as? String ?: "")
                } catch (e: Exception) { null }
            }.toMap()
            SRTInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                leadershipScore = (it["leadershipScore"] as? Number)?.toFloat() ?: 0f,
                decisionMakingScore = (it["decisionMakingScore"] as? Number)?.toFloat() ?: 0f,
                practicalityScore = (it["practicalityScore"] as? Number)?.toFloat() ?: 0f,
                initiativeScore = (it["initiativeScore"] as? Number)?.toFloat() ?: 0f,
                socialResponsibilityScore = (it["socialResponsibilityScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                categoryWiseComments = categoryWiseComments,
                flaggedResponses = (it["flaggedResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                exemplaryResponses = (it["exemplaryResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
            )
        }

        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)
        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) AnalysisStatus.valueOf(analysisStatusStr)
            else AnalysisStatus.PENDING_ANALYSIS
        } catch (e: Exception) { AnalysisStatus.PENDING_ANALYSIS }

        return SRTSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            responses = responses,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = try { SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW") }
                     catch (e: Exception) { SubmissionStatus.SUBMITTED_PENDING_REVIEW },
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    private fun parseSDTSubmission(data: Map<*, *>): SDTSubmission {
        val responsesList = data["responses"] as? List<*> ?: emptyList<Any>()
        val responses = responsesList.mapNotNull { responseData ->
            (responseData as? Map<*, *>)?.let {
                SDTQuestionResponse(
                    questionId = it["questionId"] as? String ?: "",
                    question = it["question"] as? String ?: "",
                    answer = it["answer"] as? String ?: "",
                    wordCount = (it["wordCount"] as? Number)?.toInt() ?: 0,
                    timeTakenSeconds = (it["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (it["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    isSkipped = it["isSkipped"] as? Boolean ?: false
                )
            }
        }

        val instructorScoreMap = data["instructorScore"] as? Map<*, *>
        val instructorScore = instructorScoreMap?.let {
            SDTInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                selfAwarenessScore = (it["selfAwarenessScore"] as? Number)?.toFloat() ?: 0f,
                emotionalMaturityScore = (it["emotionalMaturityScore"] as? Number)?.toFloat() ?: 0f,
                socialPerceptionScore = (it["socialPerceptionScore"] as? Number)?.toFloat() ?: 0f,
                introspectionScore = (it["introspectionScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                flaggedResponses = (it["flaggedResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                exemplaryResponses = (it["exemplaryResponses"] as? List<*>)?.mapNotNull { r -> r as? String } ?: emptyList(),
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                agreedWithAI = it["agreedWithAI"] as? Boolean ?: false
            )
        }

        val olqResultMap = data["olqResult"] as? Map<*, *>
        val olqResult = parseOLQResult(olqResultMap)
        val analysisStatusStr = data["analysisStatus"] as? String
        val analysisStatus = try {
            if (analysisStatusStr != null) AnalysisStatus.valueOf(analysisStatusStr)
            else AnalysisStatus.PENDING_ANALYSIS
        } catch (e: Exception) { AnalysisStatus.PENDING_ANALYSIS }

        return SDTSubmission(
            id = data["id"] as? String ?: "",
            userId = data["userId"] as? String ?: "",
            testId = data["testId"] as? String ?: "",
            responses = responses,
            totalTimeTakenMinutes = (data["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
            submittedAt = (data["submittedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = try { SubmissionStatus.valueOf(data["status"] as? String ?: "SUBMITTED_PENDING_REVIEW") }
                     catch (e: Exception) { SubmissionStatus.SUBMITTED_PENDING_REVIEW },
            instructorScore = instructorScore,
            gradedByInstructorId = data["gradedByInstructorId"] as? String,
            gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
            analysisStatus = analysisStatus,
            olqResult = olqResult
        )
    }

    private fun parseOLQResult(data: Map<*, *>?): OLQAnalysisResult? {
        if (data == null) return null
        return try {
            val testTypeStr = data["testType"] as? String ?: return null
            val testType = TestType.valueOf(testTypeStr)
            val olqScoresMap = data["olqScores"] as? Map<*, *> ?: emptyMap<Any, Any>()
            val olqScores = olqScoresMap.mapNotNull { (k, v) ->
                try {
                    val olq = com.ssbmax.core.domain.model.interview.OLQ.valueOf(k as? String ?: "")
                    val scoreMap = v as? Map<*, *> ?: return@mapNotNull null
                    olq to com.ssbmax.core.domain.model.interview.OLQScore(
                        score = (scoreMap["score"] as? Number)?.toInt() ?: 5,
                        confidence = (scoreMap["confidence"] as? Number)?.toInt() ?: 0,
                        reasoning = scoreMap["reasoning"] as? String ?: ""
                    )
                } catch (e: Exception) { null }
            }.toMap()
            
            OLQAnalysisResult(
                submissionId = data["submissionId"] as? String ?: "",
                testType = testType,
                olqScores = olqScores,
                overallScore = (data["overallScore"] as? Number)?.toFloat() ?: 5f,
                overallRating = data["overallRating"] as? String ?: "",
                strengths = (data["strengths"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                weaknesses = (data["weaknesses"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                recommendations = (data["recommendations"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                analyzedAt = (data["analyzedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                aiConfidence = (data["aiConfidence"] as? Number)?.toInt() ?: 0
            )
        } catch (e: Exception) { null }
    }
}
