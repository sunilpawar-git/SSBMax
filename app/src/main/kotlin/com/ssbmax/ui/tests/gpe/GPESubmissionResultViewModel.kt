package com.ssbmax.ui.tests.gpe

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

/**
 * ViewModel for GPE Submission Result Screen
 * Fetches submission data from SubmissionRepository
 */
@HiltViewModel
class GPESubmissionResultViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GPESubmissionResultUiState())
    val uiState: StateFlow<GPESubmissionResultUiState> = _uiState.asStateFlow()

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

                    // Parse GPE submission from map
                    val submission = parseGPESubmission(data)
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
                    ErrorLogger.log(error, "Error loading GPE submission")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load submission"
                    ) }
                }
        }
    }

    /**
     * Parse GPE submission from Firestore document data
     */
    private fun parseGPESubmission(data: Map<String, Any>): GPESubmission? {
        return try {
            val submissionData = data["data"] as? Map<*, *> ?: return null

            // Parse AI score if present
            val aiScoreData = submissionData["aiPreliminaryScore"] as? Map<*, *>
            val aiScore = aiScoreData?.let {
                GPEAIScore(
                    situationAnalysisScore = (it["situationAnalysisScore"] as? Number)?.toFloat() ?: 0f,
                    planningQualityScore = (it["planningQualityScore"] as? Number)?.toFloat() ?: 0f,
                    leadershipScore = (it["leadershipScore"] as? Number)?.toFloat() ?: 0f,
                    resourceUtilizationScore = (it["resourceUtilizationScore"] as? Number)?.toFloat() ?: 0f,
                    practicalityScore = (it["practicalityScore"] as? Number)?.toFloat() ?: 0f,
                    overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                    feedback = it["feedback"] as? String ?: "",
                    strengths = (it["strengths"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    areasForImprovement = (it["areasForImprovement"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            }

            // Parse instructor review if present
            val instructorReviewData = submissionData["instructorReview"] as? Map<*, *>
            val instructorReview = instructorReviewData?.let {
                val detailedScoresData = it["detailedScores"] as? Map<*, *>
                val detailedScores = detailedScoresData?.let { scores ->
                    GPEDetailedScores(
                        situationAnalysis = (scores["situationAnalysis"] as? Number)?.toFloat() ?: 0f,
                        planningQuality = (scores["planningQuality"] as? Number)?.toFloat() ?: 0f,
                        leadership = (scores["leadership"] as? Number)?.toFloat() ?: 0f,
                        resourceUtilization = (scores["resourceUtilization"] as? Number)?.toFloat() ?: 0f,
                        practicality = (scores["practicality"] as? Number)?.toFloat() ?: 0f
                    )
                } ?: GPEDetailedScores(0f, 0f, 0f, 0f, 0f)

                GPEInstructorReview(
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

            GPESubmission(
                submissionId = submissionData["submissionId"] as? String ?: data["id"] as? String ?: "",
                questionId = submissionData["questionId"] as? String ?: "",
                userId = submissionData["userId"] as? String ?: data["userId"] as? String ?: "",
                userName = submissionData["userName"] as? String ?: "",
                userEmail = submissionData["userEmail"] as? String ?: "",
                batchId = submissionData["batchId"] as? String,
                planningResponse = submissionData["planningResponse"] as? String ?: "",
                charactersCount = (submissionData["charactersCount"] as? Number)?.toInt() ?: 0,
                viewingTimeTakenSeconds = (submissionData["viewingTimeTakenSeconds"] as? Number)?.toInt() ?: 60,
                planningTimeTakenMinutes = (submissionData["planningTimeTakenMinutes"] as? Number)?.toInt() ?: 29,
                submittedAt = (submissionData["submittedAt"] as? Number)?.toLong()
                    ?: (data["submittedAt"] as? Number)?.toLong() ?: 0L,
                status = SubmissionStatus.valueOf(
                    data["status"] as? String ?: SubmissionStatus.SUBMITTED_PENDING_REVIEW.name
                ),
                aiPreliminaryScore = aiScore,
                instructorReview = instructorReview
            )
        } catch (e: Exception) {
            ErrorLogger.log(e, "Error parsing GPE submission")
            null
        }
    }
}

/**
 * UI State for GPE Submission Result
 */
data class GPESubmissionResultUiState(
    val isLoading: Boolean = true,
    val submission: GPESubmission? = null,
    val error: String? = null
)
