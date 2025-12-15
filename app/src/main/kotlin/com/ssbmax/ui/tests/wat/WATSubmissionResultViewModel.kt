package com.ssbmax.ui.tests.wat

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
 * ViewModel for WAT Submission Result Screen
 * Fetches submission data from SubmissionRepository
 */
@HiltViewModel
class WATSubmissionResultViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WATSubmissionResultUiState())
    val uiState: StateFlow<WATSubmissionResultUiState> = _uiState.asStateFlow()
    
    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            submissionRepository.getSubmission(submissionId)
                .onSuccess { data ->
                    if (data == null) {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Submission not found"
                        ) }
                        return@onSuccess
                    }

                    // Parse WAT submission from map
                    val submission = parseWATSubmission(data)
                    if (submission != null) {
                        _uiState.update { it.copy(
                            isLoading = false,
                            submission = submission
                        ) }
                    } else {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Failed to parse submission data"
                        ) }
                    }
                }
                .onFailure { error ->
                    ErrorLogger.logTestError(error, "Failed to load WAT submission result", "WAT")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load submission"
                    ) }
                }
        }
    }

    /**
     * Parse WAT submission from Firestore document data
     */
    private fun parseWATSubmission(data: Map<String, Any>): WATSubmission? {
        return try {
            val submissionData = data["data"] as? Map<*, *> ?: return null

            // Parse responses
            val responsesList = submissionData["responses"] as? List<*> ?: emptyList<Any>()
            val responses = responsesList.mapNotNull { responseData ->
                val response = responseData as? Map<*, *> ?: return@mapNotNull null
                WATWordResponse(
                    wordId = response["wordId"] as? String ?: "",
                    word = response["word"] as? String ?: "",
                    response = response["response"] as? String ?: "",
                    timeTakenSeconds = (response["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (response["submittedAt"] as? Number)?.toLong() ?: 0L,
                    isSkipped = response["isSkipped"] as? Boolean ?: false
                )
            }

            // Parse AI score if present
            val aiScoreData = submissionData["aiPreliminaryScore"] as? Map<*, *>
            val aiScore = aiScoreData?.let {
                WATAIScore(
                    overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                    positivityScore = (it["positivityScore"] as? Number)?.toFloat() ?: 0f,
                    creativityScore = (it["creativityScore"] as? Number)?.toFloat() ?: 0f,
                    speedScore = (it["speedScore"] as? Number)?.toFloat() ?: 0f,
                    relevanceScore = (it["relevanceScore"] as? Number)?.toFloat() ?: 0f,
                    emotionalMaturityScore = (it["emotionalMaturityScore"] as? Number)?.toFloat() ?: 0f,
                    feedback = it["feedback"] as? String,
                    positiveWords = (it["positiveWords"] as? Number)?.toInt() ?: 0,
                    negativeWords = (it["negativeWords"] as? Number)?.toInt() ?: 0,
                    neutralWords = (it["neutralWords"] as? Number)?.toInt() ?: 0,
                    uniqueResponsesCount = (it["uniqueResponsesCount"] as? Number)?.toInt() ?: 0,
                    repeatedPatterns = (it["repeatedPatterns"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    strengths = (it["strengths"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    areasForImprovement = (it["areasForImprovement"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            }

            // Parse instructor score if present
            val instructorScoreData = submissionData["instructorScore"] as? Map<*, *>
            val instructorScore = instructorScoreData?.let {
                WATInstructorScore(
                    overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                    positivityScore = (it["positivityScore"] as? Number)?.toFloat() ?: 0f,
                    creativityScore = (it["creativityScore"] as? Number)?.toFloat() ?: 0f,
                    speedScore = (it["speedScore"] as? Number)?.toFloat() ?: 0f,
                    relevanceScore = (it["relevanceScore"] as? Number)?.toFloat() ?: 0f,
                    emotionalMaturityScore = (it["emotionalMaturityScore"] as? Number)?.toFloat() ?: 0f,
                    feedback = it["feedback"] as? String ?: "",
                    gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                    gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                    gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: 0L
                )
            }

            // Parse OLQ analysis result if present (Phase 3)
            val analysisStatusStr = submissionData["analysisStatus"] as? String
                ?: AnalysisStatus.PENDING_ANALYSIS.name
            val analysisStatus = try {
                AnalysisStatus.valueOf(analysisStatusStr)
            } catch (e: Exception) {
                AnalysisStatus.PENDING_ANALYSIS
            }

            val olqResultData = submissionData["olqResult"] as? Map<*, *>
            val olqResult = olqResultData?.let { parseOLQResult(it) }

            WATSubmission(
                id = submissionData["id"] as? String ?: data["id"] as? String ?: "",
                userId = submissionData["userId"] as? String ?: data["userId"] as? String ?: "",
                testId = submissionData["testId"] as? String ?: data["testId"] as? String ?: "",
                responses = responses,
                totalTimeTakenMinutes = (submissionData["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
                submittedAt = (submissionData["submittedAt"] as? Number)?.toLong()
                    ?: (data["submittedAt"] as? Number)?.toLong() ?: 0L,
                status = SubmissionStatus.valueOf(
                    data["status"] as? String ?: SubmissionStatus.SUBMITTED_PENDING_REVIEW.name
                ),
                aiPreliminaryScore = aiScore,
                instructorScore = instructorScore,
                gradedByInstructorId = data["gradedByInstructorId"] as? String,
                gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong(),
                analysisStatus = analysisStatus,
                olqResult = olqResult
            )
        } catch (e: Exception) {
            ErrorLogger.logTestError(e, "Error parsing WAT submission data", "WAT")
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
            ErrorLogger.logTestError(e, "Error parsing OLQ result", "WAT")
            null
        }
    }
}

/**
 * UI State for WAT Submission Result
 */
data class WATSubmissionResultUiState(
    val isLoading: Boolean = true,
    val submission: WATSubmission? = null,
    val error: String? = null
)

