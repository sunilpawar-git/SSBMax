package com.ssbmax.ui.tests.ppdt

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.SubmissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    
    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            submissionRepository.getSubmission(submissionId)
                .onSuccess { data ->
                    if (data == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Submission not found"
                        )
                        return@onSuccess
                    }

                    // Parse PPDT submission from map
                    val submission = parsePPDTSubmission(data)
                    if (submission != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            submission = submission
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to parse submission data"
                        )
                    }
                }
                .onFailure { error ->
                    Log.e("PPDTSubmissionResult", "Error loading submission", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load submission"
                    )
                }
        }
    }

    /**
     * Parse PPDT submission from Firestore document data
     */
    private fun parsePPDTSubmission(data: Map<String, Any>): PPDTSubmission? {
        return try {
            val submissionData = data["data"] as? Map<*, *> ?: return null

            // Parse AI score if present
            val aiScoreData = submissionData["aiPreliminaryScore"] as? Map<*, *>
            val aiScore = aiScoreData?.let {
                PPDTAIScore(
                    perceptionScore = (it["perceptionScore"] as? Number)?.toFloat() ?: 0f,
                    imaginationScore = (it["imaginationScore"] as? Number)?.toFloat() ?: 0f,
                    narrationScore = (it["narrationScore"] as? Number)?.toFloat() ?: 0f,
                    characterDepictionScore = (it["characterDepictionScore"] as? Number)?.toFloat() ?: 0f,
                    positivityScore = (it["positivityScore"] as? Number)?.toFloat() ?: 0f,
                    overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                    feedback = it["feedback"] as? String,
                    strengths = (it["strengths"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    areasForImprovement = (it["areasForImprovement"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            }

            // Parse instructor review if present
            val instructorReviewData = submissionData["instructorReview"] as? Map<*, *>
            val instructorReview = instructorReviewData?.let {
                // Parse detailed scores
                val detailedScoresData = it["detailedScores"] as? Map<*, *>
                val detailedScores = if (detailedScoresData != null) {
                    PPDTDetailedScores(
                        perception = (detailedScoresData["perception"] as? Number)?.toFloat() ?: 0f,
                        imagination = (detailedScoresData["imagination"] as? Number)?.toFloat() ?: 0f,
                        narration = (detailedScoresData["narration"] as? Number)?.toFloat() ?: 0f,
                        characterDepiction = (detailedScoresData["characterDepiction"] as? Number)?.toFloat() ?: 0f,
                        positivity = (detailedScoresData["positivity"] as? Number)?.toFloat() ?: 0f,
                        notes = (detailedScoresData["notes"] as? Map<*, *>)?.mapNotNull { entry ->
                            val key = entry.key as? String
                            val value = entry.value as? String
                            if (key != null && value != null) key to value else null
                        }?.toMap() ?: emptyMap()
                    )
                } else {
                    // Create default scores if not present
                    PPDTDetailedScores(
                        perception = 0f,
                        imagination = 0f,
                        narration = 0f,
                        characterDepiction = 0f,
                        positivity = 0f
                    )
                }

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

            PPDTSubmission(
                submissionId = submissionData["submissionId"] as? String 
                    ?: data["id"] as? String ?: "",
                questionId = submissionData["questionId"] as? String ?: "",
                userId = submissionData["userId"] as? String 
                    ?: data["userId"] as? String ?: "",
                userName = submissionData["userName"] as? String ?: "",
                userEmail = submissionData["userEmail"] as? String ?: "",
                batchId = submissionData["batchId"] as? String,
                story = submissionData["story"] as? String ?: "",
                charactersCount = (submissionData["charactersCount"] as? Number)?.toInt() ?: 0,
                viewingTimeTakenSeconds = (submissionData["viewingTimeTakenSeconds"] as? Number)?.toInt() ?: 0,
                writingTimeTakenMinutes = (submissionData["writingTimeTakenMinutes"] as? Number)?.toInt() ?: 0,
                submittedAt = (submissionData["submittedAt"] as? Number)?.toLong()
                    ?: (data["submittedAt"] as? Number)?.toLong() ?: 0L,
                status = SubmissionStatus.valueOf(
                    data["status"] as? String ?: SubmissionStatus.SUBMITTED_PENDING_REVIEW.name
                ),
                aiPreliminaryScore = aiScore,
                instructorReview = instructorReview
            )
        } catch (e: Exception) {
            Log.e("PPDTSubmissionResult", "Error parsing PPDT submission", e)
            null
        }
    }
}

/**
 * UI State for PPDT Submission Result Screen
 */
data class PPDTSubmissionResultUiState(
    val isLoading: Boolean = true,
    val submission: PPDTSubmission? = null,
    val error: String? = null
)

