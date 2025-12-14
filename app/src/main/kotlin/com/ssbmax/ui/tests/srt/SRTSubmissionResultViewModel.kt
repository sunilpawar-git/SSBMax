package com.ssbmax.ui.tests.srt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

                    // Parse SRT submission from map
                    val submission = parseSRTSubmission(data)
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
                    ErrorLogger.logTestError(error, "Failed to load SRT submission result", "SRT")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load submission"
                    ) }
                }
        }
    }

    /**
     * Parse SRT submission from Firestore document data
     */
    private fun parseSRTSubmission(data: Map<String, Any>): SRTSubmission? {
        return try {
            val submissionData = data["data"] as? Map<*, *> ?: return null

            // Parse responses
            val responsesList = submissionData["responses"] as? List<*> ?: emptyList<Any>()
            val responses = responsesList.mapNotNull { responseData ->
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

            // Parse AI score if present
            val aiScoreData = submissionData["aiPreliminaryScore"] as? Map<*, *>
            val aiScore = aiScoreData?.let {
                // Parse ResponseQuality enum
                val responseQualityStr = it["responseQuality"] as? String
                val responseQuality = try {
                    if (responseQualityStr != null) {
                        ResponseQuality.valueOf(responseQualityStr)
                    } else {
                        ResponseQuality.AVERAGE
                    }
                } catch (e: Exception) {
                    ResponseQuality.AVERAGE
                }

                SRTAIScore(
                    overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                    leadershipScore = (it["leadershipScore"] as? Number)?.toFloat() ?: 0f,
                    decisionMakingScore = (it["decisionMakingScore"] as? Number)?.toFloat() ?: 0f,
                    practicalityScore = (it["practicalityScore"] as? Number)?.toFloat() ?: 0f,
                    initiativeScore = (it["initiativeScore"] as? Number)?.toFloat() ?: 0f,
                    socialResponsibilityScore = (it["socialResponsibilityScore"] as? Number)?.toFloat() ?: 0f,
                    feedback = it["feedback"] as? String,
                    positiveTraits = (it["positiveTraits"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    concerningPatterns = (it["concerningPatterns"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    responseQuality = responseQuality,
                    strengths = (it["strengths"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    areasForImprovement = (it["areasForImprovement"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            }

            // Parse instructor score if present
            val instructorScoreData = submissionData["instructorScore"] as? Map<*, *>
            val instructorScore = instructorScoreData?.let {
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

            SRTSubmission(
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
                gradingTimestamp = (data["gradingTimestamp"] as? Number)?.toLong()
            )
        } catch (e: Exception) {
            ErrorLogger.logTestError(e, "Error parsing SRT submission data", "SRT")
            null
        }
    }
}

/**
 * UI State for SRT Submission Result
 */
data class SRTSubmissionResultUiState(
    val isLoading: Boolean = true,
    val submission: SRTSubmission? = null,
    val error: String? = null
)

