package com.ssbmax.ui.tests.sdt

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

@HiltViewModel
class SDTSubmissionResultViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SDTSubmissionResultUiState())
    val uiState: StateFlow<SDTSubmissionResultUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "SDTResultViewModel"
    }

    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            submissionRepository.getSubmission(submissionId)
                .onSuccess { data ->
                    if (data == null) {
                        ErrorLogger.logTestError(
                            throwable = IllegalStateException("Submission data is null"),
                            description = "SDT submission not found: $submissionId",
                            testType = "SDT"
                        )
                        _uiState.update { it.copy(isLoading = false, submission = null, error = "Submission not found") }
                        return@onSuccess
                    }

                    val submission = parseSDTSubmission(data)

                    if (submission == null) {
                        ErrorLogger.logTestError(
                            throwable = IllegalStateException("Failed to parse submission"),
                            description = "SDT submission parsing failed: $submissionId",
                            testType = "SDT"
                        )
                    }

                    _uiState.update { it.copy(isLoading = false, submission = submission,
                        error = if (submission == null) "Submission not found" else null) }
                }
                .onFailure { error ->
                    ErrorLogger.logTestError(error, "Failed to load SDT submission result", "SDT")
                    _uiState.update { it.copy(isLoading = false,
                        error = error.message ?: "Failed to load submission") }
                }
        }
    }

    private fun parseSDTSubmission(data: Map<String, Any>): SDTSubmission? {
        return try {
            val submissionData = data["data"] as? Map<*, *> ?: return null
            val responsesList = submissionData["responses"] as? List<*> ?: emptyList<Any>()
            val responses = responsesList.mapNotNull { responseData ->
                val response = responseData as? Map<*, *> ?: return@mapNotNull null
                SDTQuestionResponse(
                    questionId = response["questionId"] as? String ?: "",
                    question = response["question"] as? String ?: "",
                    answer = response["answer"] as? String ?: "",
                    wordCount = (response["wordCount"] as? Number)?.toInt() ?: 0,
                    timeTakenSeconds = (response["timeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (response["submittedAt"] as? Number)?.toLong() ?: 0L,
                    isSkipped = response["isSkipped"] as? Boolean ?: false
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

            SDTSubmission(
                id = submissionData["id"] as? String ?: "",
                userId = submissionData["userId"] as? String ?: "",
                testId = submissionData["testId"] as? String ?: "",
                responses = responses,
                totalTimeTakenMinutes = (submissionData["totalTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
                submittedAt = (submissionData["submittedAt"] as? Number)?.toLong() ?: 0L,

                analysisStatus = analysisStatus,
                olqResult = olqResult
            )
        } catch (e: Exception) {
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
            ErrorLogger.logTestError(e, "Error parsing OLQ result", "SDT")
            null
        }
    }
}

data class SDTSubmissionResultUiState(
    val isLoading: Boolean = true,
    val submission: SDTSubmission? = null,
    val error: String? = null
)

