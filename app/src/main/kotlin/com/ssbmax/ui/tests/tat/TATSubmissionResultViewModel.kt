package com.ssbmax.ui.tests.tat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for TAT Submission Result Screen
 */
@HiltViewModel
class TATSubmissionResultViewModel @Inject constructor(
    // TODO: Inject SubmissionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TATSubmissionResultUiState())
    val uiState: StateFlow<TATSubmissionResultUiState> = _uiState.asStateFlow()
    
    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // TODO: Load from repository
                val submission = generateMockSubmission(submissionId)
                
                _uiState.update { it.copy(
                    isLoading = false,
                    submission = submission
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }
    
    private fun generateMockSubmission(submissionId: String): TATSubmission {
        val mockStories = (1..12).map { index ->
            TATStoryResponse(
                questionId = "tat_q_$index",
                story = "Mock story for picture $index",
                charactersCount = 250,
                viewingTimeTakenSeconds = 30,
                writingTimeTakenSeconds = 180,
                submittedAt = System.currentTimeMillis()
            )
        }
        
        return TATSubmission(
            id = submissionId,
            userId = "current_user",
            testId = "tat_standard",
            stories = mockStories,
            totalTimeTakenMinutes = 50,
            submittedAt = System.currentTimeMillis(),
            status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
            aiPreliminaryScore = TATAIScore(
                overallScore = 78f,
                thematicPerceptionScore = 16f,
                imaginationScore = 15f,
                characterDepictionScore = 16f,
                emotionalToneScore = 16f,
                narrativeStructureScore = 15f,
                feedback = "Good storytelling with positive themes. Shows leadership qualities.",
                strengths = listOf("Creative", "Positive outlook"),
                areasForImprovement = listOf("Add more detail")
            )
        )
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

