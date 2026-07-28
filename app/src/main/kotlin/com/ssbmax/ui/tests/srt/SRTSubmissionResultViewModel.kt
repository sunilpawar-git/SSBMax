package com.ssbmax.ui.tests.srt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.SRTInstructorScore
import com.ssbmax.core.domain.model.SRTSituationResponse
import com.ssbmax.core.domain.model.SRTSubmission
import com.ssbmax.core.domain.model.SubmissionStatus
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.scoring.EntryType
import com.ssbmax.core.domain.validation.SSBRecommendationUIModel
import com.ssbmax.core.domain.validation.ValidationIntegration
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SRT Submission Result Screen
 * Fetches submission data from SubmissionRepository
 */
@HiltViewModel
class SRTSubmissionResultViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SRTSubmissionResultUiState())
    val uiState: StateFlow<SRTSubmissionResultUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "SRTSubmissionResultVM"
    }
    
    fun loadSubmission(submissionId: String) {
        android.util.Log.d(TAG, "📥 Loading submission: $submissionId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Track best state seen to prevent regression from conflicting Firestore updates
            var hasSeenCompleteWithOLQ = false

            try {
                // Observe submission for real-time status updates (same pattern as PPDT)
                submissionRepository.observeSubmission(submissionId).collect { data ->
                    android.util.Log.d(TAG, "🔄 Firestore snapshot received for: $submissionId")

                    if (data == null) {
                        android.util.Log.e(TAG, "❌ Submission not found in snapshot")
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Submission not found"
                        ) }
                        return@collect
                    }

                    val submissionData = data["data"] as? Map<*, *>
                    val snapshotState = resolveSnapshotOlqState(submissionId, submissionData)

                    if (snapshotState.isCompleteWithOLQ) {
                        hasSeenCompleteWithOLQ = true
                        android.util.Log.d(TAG, "✅ Marked hasSeenCompleteWithOLQ = true")
                    }

                    // CRITICAL FIX: Prevent regression from COMPLETED+OLQ to incomplete state
                    if (hasSeenCompleteWithOLQ && !snapshotState.isCompleteWithOLQ) {
                        android.util.Log.w(TAG, "⚠️ BLOCKING REGRESSION: Previously saw COMPLETED with OLQ, ignoring incomplete snapshot")
                        return@collect
                    }

                    val submission = buildSubmissionWithResult(data, snapshotState.fetchedOlqResult)

                    if (submission != null) {
                        android.util.Log.d(TAG, "📊 Updating UI state - OLQ scores: ${submission.olqResult?.olqScores?.size ?: 0}")
                        _uiState.update { it.copy(
                            isLoading = false,
                            submission = submission,
                            ssbRecommendation = computeSsbRecommendation(submission)
                        ) }
                    } else {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Failed to parse submission data"
                        ) }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Exception while observing submission", e)
                ErrorLogger.logTestError(e, "Failed to load SRT submission result", "SRT")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load submission"
                ) }
            }
        }
    }

    private data class SnapshotOlqState(
        val isCompleteWithOLQ: Boolean,
        val fetchedOlqResult: OLQAnalysisResult?
    )

    private suspend fun resolveSnapshotOlqState(submissionId: String, submissionData: Map<*, *>?): SnapshotOlqState {
        val analysisStatus = parseAnalysisStatus(submissionData?.get("analysisStatus") as? String)
        var hasOlqResult = submissionData?.get("olqResult") != null
        android.util.Log.d(TAG, "   - analysisStatus: $analysisStatus, olqResult exists: $hasOlqResult")

        // CRITICAL FIX: If COMPLETED but no result in snapshot, fetch it separately (New Architecture)
        var fetchedOlqResult: OLQAnalysisResult? = null
        if (analysisStatus == AnalysisStatus.COMPLETED && !hasOlqResult) {
            android.util.Log.d(TAG, "🔍 Status is COMPLETED but result missing in snapshot. Fetching separately...")
            fetchedOlqResult = submissionRepository.getSRTResult(submissionId).getOrNull()
            if (fetchedOlqResult != null) {
                android.util.Log.d(TAG, "✅ Successfully fetched OLQ result separately!")
                hasOlqResult = true
            }
        }

        val isCompleteWithOLQ = analysisStatus == AnalysisStatus.COMPLETED && hasOlqResult
        return SnapshotOlqState(isCompleteWithOLQ, fetchedOlqResult)
    }

    private fun buildSubmissionWithResult(data: Map<String, Any>, fetchedOlqResult: OLQAnalysisResult?): SRTSubmission? {
        val submission = parseSRTSubmission(data)
        return if (submission != null && fetchedOlqResult != null) {
            submission.copy(olqResult = fetchedOlqResult)
        } else {
            submission
        }
    }

    private fun computeSsbRecommendation(submission: SRTSubmission): SSBRecommendationUIModel? {
        val scores = submission.olqResult?.olqScores ?: return null
        if (scores.isEmpty()) return null
        val validationResult = ValidationIntegration.validateScores(scores = scores, entryType = EntryType.NDA)
        return SSBRecommendationUIModel.fromValidationResult(validationResult, EntryType.NDA)
    }

    /**
     * Parse SRT submission from Firestore document data
     */
    private fun parseSRTSubmission(data: Map<String, Any>): SRTSubmission? {
        return try {
            val submissionData = data["data"] as? Map<*, *> ?: return null
            val responses = parseSRTResponses(submissionData["responses"] as? List<*> ?: emptyList<Any>())
            val instructorScore = parseSRTInstructorScore(submissionData["instructorScore"] as? Map<*, *>)
            val analysisStatus = parseAnalysisStatus(submissionData["analysisStatus"] as? String)
            val olqResultData = submissionData["olqResult"] as? Map<*, *>
            val olqResult = olqResultData?.let { parseOLQResult(it) }

            SRTSubmission(
                id = fallbackStringField(submissionData, data, "id"),
                userId = fallbackStringField(submissionData, data, "userId"),
                testId = fallbackStringField(submissionData, data, "testId"),
                responses = responses,
                totalTimeTakenMinutes = (submissionData["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
                submittedAt = fallbackSubmittedAt(submissionData, data),
                status = SubmissionStatus.valueOf(
                    data["status"] as? String ?: SubmissionStatus.SUBMITTED_PENDING_REVIEW.name
                ),

                instructorScore = instructorScore,
                gradedByInstructorId = data["gradedByInstructorId"] as? String,
                gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
                analysisStatus = analysisStatus,
                olqResult = olqResult
            )
        } catch (e: Exception) {
            ErrorLogger.logTestError(e, "Error parsing SRT submission data", "SRT")
            null
        }
    }

    private fun fallbackStringField(submissionData: Map<*, *>, data: Map<String, Any>, key: String): String {
        return submissionData[key] as? String ?: data[key] as? String ?: ""
    }

    private fun fallbackSubmittedAt(submissionData: Map<*, *>, data: Map<String, Any>): Long {
        return (submissionData["submittedAt"] as? Number)?.toLong()
            ?: (data["submittedAt"] as? Number)?.toLong() ?: 0L
    }

    private fun parseSRTResponses(responsesList: List<*>): List<SRTSituationResponse> {
        return responsesList.mapNotNull { responseData ->
            val response = responseData as? Map<*, *> ?: return@mapNotNull null
            SRTSituationResponse(
                situationId = response["situationId"] as? String ?: "",
                situation = response["situation"] as? String ?: "",
                response = response["response"] as? String ?: "",
                charactersCount = (response["charactersCount"] as? Number)?.toInt() ?: 0,
                timeTakenSeconds = (response["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                submittedAt = (response["submittedAt"] as? Number)?.toLong() ?: 0L,
                isSkipped = response["isSkipped"] as? Boolean ?: false
            )
        }
    }

    private fun parseSRTInstructorScore(instructorScoreData: Map<*, *>?): SRTInstructorScore? {
        return instructorScoreData?.let {
            SRTInstructorScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                leadershipScore = (it["leadershipScore"] as? Number)?.toFloat() ?: 0f,
                decisionMakingScore = (it["decisionMakingScore"] as? Number)?.toFloat() ?: 0f,
                practicalityScore = (it["practicalityScore"] as? Number)?.toFloat() ?: 0f,
                initiativeScore = (it["initiativeScore"] as? Number)?.toFloat() ?: 0f,
                socialResponsibilityScore = (it["socialResponsibilityScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: 0L
            )
        }
    }

    private fun parseAnalysisStatus(raw: String?): AnalysisStatus {
        return try {
            AnalysisStatus.valueOf(raw ?: AnalysisStatus.PENDING_ANALYSIS.name)
        } catch (e: Exception) {
            AnalysisStatus.PENDING_ANALYSIS
        }
    }

    /**
     * Parse OLQ analysis result from Firestore document data
     */
    private fun parseOLQResult(data: Map<*, *>): OLQAnalysisResult? {
        return try {
            val submissionId = data["submissionId"] as? String ?: return null
            val testTypeStr = data["testType"] as? String ?: return null
            val testType = TestType.valueOf(testTypeStr)

            // Parse OLQ scores map
            val olqScoresData = data["olqScores"] as? Map<*, *> ?: return null
            val olqScores = olqScoresData.mapNotNull { (key, value) -> parseSingleOlqScore(key, value) }.toMap()

            if (olqScores.size < 14) return null  // Need at least 14 OLQs

            buildOLQAnalysisResult(submissionId, testType, olqScores, data)
        } catch (e: Exception) {
            ErrorLogger.logTestError(e, "Error parsing OLQ result", "SRT")
            null
        }
    }

    private fun parseSingleOlqScore(key: Any?, value: Any?): Pair<OLQ, OLQScore>? {
        val olqName = key as? String ?: return null
        val olq = parseOlqEnum(olqName) ?: return null
        val scoreData = value as? Map<*, *> ?: return null
        return buildOlqScorePair(olq, scoreData)
    }

    private fun parseOlqEnum(name: String): OLQ? {
        return try {
            OLQ.valueOf(name)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildOlqScorePair(olq: OLQ, scoreData: Map<*, *>): Pair<OLQ, OLQScore>? {
        val scoreValue = (scoreData["score"] as? Number)?.toInt() ?: return null
        return olq to OLQScore(
            score = scoreValue,
            confidence = (scoreData["confidence"] as? Number)?.toInt() ?: 0,
            reasoning = scoreData["reasoning"] as? String ?: ""
        )
    }

    private fun buildOLQAnalysisResult(
        submissionId: String,
        testType: TestType,
        olqScores: Map<OLQ, OLQScore>,
        data: Map<*, *>
    ): OLQAnalysisResult {
        return OLQAnalysisResult(
            submissionId = submissionId,
            testType = testType,
            olqScores = olqScores,
            overallScore = (data["overallScore"] as? Number)?.toFloat() ?: 0f,
            overallRating = data["overallRating"] as? String ?: "",
            strengths = (data["strengths"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            weaknesses = (data["weaknesses"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            recommendations = (data["recommendations"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            analyzedAt = (data["analyzedAt"] as? Number)?.toLong() ?: 0L,
            aiConfidence = (data["aiConfidence"] as? Number)?.toInt() ?: 0
        )
    }
}

/**
 * UI State for SRT Submission Result
 */
data class SRTSubmissionResultUiState(
    override val isLoading: Boolean = true,
    val submission: SRTSubmission? = null,
    override val ssbRecommendation: SSBRecommendationUIModel? = null,
    override val error: String? = null
) : com.ssbmax.ui.components.result.UnifiedResultUiState {
    override val analysisStatus: AnalysisStatus
        get() = submission?.analysisStatus ?: AnalysisStatus.PENDING_ANALYSIS
    override val olqResult: OLQAnalysisResult?
        get() = submission?.olqResult
}

