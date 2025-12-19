package com.ssbmax.ui.tests.ppdt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for PPDT Submission Result Screen
 * Fetches submission data from SubmissionRepository
 */
@HiltViewModel
class PPDTSubmissionResultViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PPDTSubmissionResultUiState())
    val uiState: StateFlow<PPDTSubmissionResultUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "PPDTSubmissionResultViewModel"
    }
    
    init {
        android.util.Log.d(TAG, "🚀 PPDTSubmissionResultViewModel initialized")
    }
    
    fun loadSubmission(submissionId: String) {
        android.util.Log.d(TAG, "📥 Loading submission: $submissionId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Track best state seen to prevent regression from conflicting Firestore updates
            var hasSeenCompleteWithOLQ = false
            
            try {
                // Observe submission for real-time status updates (GTO pattern)
                submissionRepository.observeSubmission(submissionId).collect { data ->
                    android.util.Log.d(TAG, "🔄 Firestore snapshot received for: $submissionId")
                    android.util.Log.d(TAG, "   - Data exists: ${data != null}")
                    
                    if (data == null) {
                        android.util.Log.e(TAG, "❌ Submission not found in snapshot")
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Submission not found"
                        ) }
                        return@collect
                    }

                    val submissionData = data["data"] as? Map<*, *>
                    val analysisStatus = submissionData?.get("analysisStatus") as? String
                    val hasOlqResult = submissionData?.get("olqResult") != null
                    
                    android.util.Log.d(TAG, "   - analysisStatus: $analysisStatus")
                    android.util.Log.d(TAG, "   - olqResult exists: $hasOlqResult")
                    
                    // Check if this is a COMPLETE snapshot with OLQ data
                    val isCompleteWithOLQ = analysisStatus == "COMPLETED" && hasOlqResult
                    
                    // Track best state
                    if (isCompleteWithOLQ) {
                        hasSeenCompleteWithOLQ = true
                        android.util.Log.d(TAG, "✅ Marked hasSeenCompleteWithOLQ = true")
                    }
                    
                    // CRITICAL FIX: Prevent regression from COMPLETED+OLQ to incomplete state
                    // This handles Firestore offline cache conflicts where stale data overwrites
                    if (hasSeenCompleteWithOLQ && !isCompleteWithOLQ) {
                        android.util.Log.w(TAG, "⚠️ BLOCKING REGRESSION: Previously saw COMPLETED with OLQ, ignoring incomplete snapshot")
                        return@collect // Skip this update, keep previous good state
                    }
                    
                    // Parse PPDT submission from map
                    val submission = parsePPDTSubmission(data)
                    if (submission != null) {
                        android.util.Log.d(TAG, "📊 Updating UI state with submission data")
                        android.util.Log.d(TAG, "   - analysisStatus: ${submission.analysisStatus}")
                        android.util.Log.d(TAG, "   - olqResult: ${submission.olqResult != null}")
                        android.util.Log.d(TAG, "   - OLQ scores count: ${submission.olqResult?.olqScores?.size ?: 0}")
                        
                        _uiState.update { it.copy(
                            isLoading = false,
                            submission = submission
                        ) }
                    } else {
                        android.util.Log.e(TAG, "❌ Failed to parse submission data")
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Failed to parse submission data"
                        ) }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Exception while observing submission", e)
                ErrorLogger.logTestError(e, "Failed to load PPDT submission result", "PPDT")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load submission"
                ) }
            }
        }
    }

    /**
     * Parse PPDT submission from Firestore document data
     * Note: Legacy AI scoring removed - now using unified OLQ scoring system
     */
    private fun parsePPDTSubmission(data: Map<String, Any>): PPDTSubmission? {
        return try {
            val submissionData = data["data"] as? Map<*, *> ?: return null

            // Parse instructor review if present
            val instructorReviewData = submissionData["instructorReview"] as? Map<*, *>
            val instructorReview = instructorReviewData?.let {
                val detailedScoresData = it["detailedScores"] as? Map<*, *>
                val detailedScores = detailedScoresData?.let { scores ->
                    PPDTDetailedScores(
                        perception = (scores["perception"] as? Number)?.toFloat() ?: 0f,
                        imagination = (scores["imagination"] as? Number)?.toFloat() ?: 0f,
                        narration = (scores["narration"] as? Number)?.toFloat() ?: 0f,
                        characterDepiction = (scores["characterDepiction"] as? Number)?.toFloat() ?: 0f,
                        positivity = (scores["positivity"] as? Number)?.toFloat() ?: 0f
                    )
                } ?: PPDTDetailedScores(0f, 0f, 0f, 0f, 0f)
                
                PPDTInstructorReview(
                    reviewId = it["reviewId"] as? String ?: "",
                    instructorId = it["instructorId"] as? String ?: "",
                    instructorName = it["instructorName"] as? String ?: "",
                    finalScore = (it["finalScore"] as? Number)?.toFloat() ?: 0f,
                    feedback = it["feedback"] as? String ?: "",
                    detailedScores = detailedScores,
                    agreedWithAI = it["agreedWithAI"] as? Boolean ?: false,
                    reviewedAt = (it["reviewedAt"] as? Number)?.toLong() ?: 0L,
                    timeSpentMinutes = (it["timeSpentMinutes"] as? Number)?.toInt() ?: 0
                )
            }

            // Parse OLQ analysis result if present (from PPDTAnalysisWorker)
            val analysisStatusStr = submissionData["analysisStatus"] as? String
                ?: AnalysisStatus.PENDING_ANALYSIS.name
            val analysisStatus = try {
                AnalysisStatus.valueOf(analysisStatusStr)
            } catch (e: Exception) {
                AnalysisStatus.PENDING_ANALYSIS
            }

            val olqResultData = submissionData["olqResult"] as? Map<*, *>
            val olqResult = olqResultData?.let { parseOLQResult(it) }

            PPDTSubmission(
                submissionId = submissionData["submissionId"] as? String ?: data["id"] as? String ?: "",
                questionId = submissionData["questionId"] as? String ?: "",
                userId = submissionData["userId"] as? String ?: data["userId"] as? String ?: "",
                userName = submissionData["userName"] as? String ?: "",
                userEmail = submissionData["userEmail"] as? String ?: "",
                batchId = submissionData["batchId"] as? String,
                story = submissionData["story"] as? String ?: "",
                charactersCount = (submissionData["charactersCount"] as? Number)?.toInt() ?: 0,
                viewingTimeTakenSeconds = (submissionData["viewingTimeTakenSeconds"] as? Number)?.toInt() ?: 30,
                writingTimeTakenMinutes = (submissionData["writingTimeTakenMinutes"] as? Number)?.toInt() ?: 4,
                submittedAt = (submissionData["submittedAt"] as? Number)?.toLong()
                    ?: (data["submittedAt"] as? Number)?.toLong() ?: 0L,
                status = SubmissionStatus.valueOf(
                    data["status"] as? String ?: SubmissionStatus.SUBMITTED_PENDING_REVIEW.name
                ),
                instructorReview = instructorReview,
                analysisStatus = analysisStatus,
                olqResult = olqResult
            )
        } catch (e: Exception) {
            ErrorLogger.logTestError(e, "Error parsing PPDT submission data", "PPDT")
            null
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
            val olqScores = olqScoresData.mapNotNull { (key, value) ->
                val olqName = key as? String ?: return@mapNotNull null
                val olq = try {
                    OLQ.valueOf(olqName)
                } catch (e: Exception) {
                    return@mapNotNull null
                }

                val scoreData = value as? Map<*, *> ?: return@mapNotNull null
                val score = OLQScore(
                    score = (scoreData["score"] as? Number)?.toInt() ?: return@mapNotNull null,
                    confidence = (scoreData["confidence"] as? Number)?.toInt() ?: 0,
                    reasoning = scoreData["reasoning"] as? String ?: ""
                )

                olq to score
            }.toMap()

            if (olqScores.size < 14) return null  // Need at least 14 OLQs

            OLQAnalysisResult(
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
        } catch (e: Exception) {
            ErrorLogger.logTestError(e, "Error parsing OLQ result", "PPDT")
            null
        }
    }
}

/**
 * UI State for PPDT Submission Result
 */
data class PPDTSubmissionResultUiState(
    val isLoading: Boolean = true,
    val submission: PPDTSubmission? = null,
    val error: String? = null
)
