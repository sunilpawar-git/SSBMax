package com.ssbmax.ui.tests.tat

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
 * ViewModel for TAT Submission Result Screen
 * Fetches submission data from SubmissionRepository
 */
@HiltViewModel
class TATSubmissionResultViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TATSubmissionResultUiState())
    val uiState: StateFlow<TATSubmissionResultUiState> = _uiState.asStateFlow()
    
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

                    // Parse TAT submission from map
                    val submission = parseTATSubmission(data)
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
                    ErrorLogger.logTestError(error, "Error loading TAT submission", "TAT")
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load submission"
                    ) }
                }
        }
    }

    /**
     * Parse TAT submission from Firestore document data
     */
    private fun parseTATSubmission(data: Map<String, Any>): TATSubmission? {
        return try {
            val submissionData = data["data"] as? Map<*, *> ?: return null

            // Parse stories
            val storiesList = submissionData["stories"] as? List<*> ?: emptyList<Any>()
            val stories = storiesList.mapNotNull { storyData ->
                val story = storyData as? Map<*, *> ?: return@mapNotNull null
                TATStoryResponse(
                    questionId = story["questionId"] as? String ?: "",
                    story = story["story"] as? String ?: "",
                    charactersCount = (story["charactersCount"] as? Number)?.toInt() ?: 0,
                    viewingTimeTakenSeconds = (story["viewingTimeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    writingTimeTakenSeconds = (story["writingTimeTakenSeconds"] as? Number)?.toInt() ?: 0,
                    submittedAt = (story["submittedAt"] as? Number)?.toLong() ?: 0L
                )
            }

            // Parse AI score if present
            val aiScoreData = submissionData["aiPreliminaryScore"] as? Map<*, *>
            val aiScore = aiScoreData?.let {
                TATAIScore(
                    overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                    thematicPerceptionScore = (it["thematicPerceptionScore"] as? Number)?.toFloat() ?: 0f,
                    imaginationScore = (it["imaginationScore"] as? Number)?.toFloat() ?: 0f,
                    characterDepictionScore = (it["characterDepictionScore"] as? Number)?.toFloat() ?: 0f,
                    emotionalToneScore = (it["emotionalToneScore"] as? Number)?.toFloat() ?: 0f,
                    narrativeStructureScore = (it["narrativeStructureScore"] as? Number)?.toFloat() ?: 0f,
                    feedback = it["feedback"] as? String,
                    strengths = (it["strengths"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                    areasForImprovement = (it["areasForImprovement"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                )
            }

            // Parse instructor score if present
            val instructorScoreData = submissionData["instructorScore"] as? Map<*, *>
            val instructorScore = instructorScoreData?.let {
                TATInstructorScore(
                    overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                    thematicPerceptionScore = (it["thematicPerceptionScore"] as? Number)?.toFloat() ?: 0f,
                    imaginationScore = (it["imaginationScore"] as? Number)?.toFloat() ?: 0f,
                    characterDepictionScore = (it["characterDepictionScore"] as? Number)?.toFloat() ?: 0f,
                    emotionalToneScore = (it["emotionalToneScore"] as? Number)?.toFloat() ?: 0f,
                    narrativeStructureScore = (it["narrativeStructureScore"] as? Number)?.toFloat() ?: 0f,
                    feedback = it["feedback"] as? String ?: "",
                    gradedByInstructorId = it["gradedByInstructorId"] as? String ?: "",
                    gradedByInstructorName = it["gradedByInstructorName"] as? String ?: "",
                    gradedAt = (it["gradedAt"] as? Number)?.toLong() ?: 0L
                )
            }

            TATSubmission(
                id = submissionData["id"] as? String ?: data["id"] as? String ?: "",
                userId = submissionData["userId"] as? String ?: data["userId"] as? String ?: "",
                testId = submissionData["testId"] as? String ?: data["testId"] as? String ?: "",
                stories = stories,
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
            ErrorLogger.logTestError(e, "Error parsing TAT submission data", "TAT")
            null
        }
    }
}

/**
 * UI State for TAT Submission Result
 */
data class TATSubmissionResultUiState(
    val isLoading: Boolean = true,
    val submission: TATSubmission? = null,
    val error: String? = null
)

