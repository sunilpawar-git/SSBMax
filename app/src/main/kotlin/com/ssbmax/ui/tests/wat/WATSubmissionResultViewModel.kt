package com.ssbmax.ui.tests.wat

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
 * ViewModel for WAT Submission Result Screen
 */
@HiltViewModel
class WATSubmissionResultViewModel @Inject constructor(
    // TODO: Inject SubmissionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WATSubmissionResultUiState())
    val uiState: StateFlow<WATSubmissionResultUiState> = _uiState.asStateFlow()
    
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
    
    private fun generateMockSubmission(submissionId: String): WATSubmission {
        val mockResponses = (1..60).map { index ->
            WATWordResponse(
                wordId = "wat_w_$index",
                word = "Word$index",
                response = "Response$index",
                timeTakenSeconds = (5..14).random(),
                submittedAt = System.currentTimeMillis(),
                isSkipped = index % 10 == 0 // Every 10th skipped
            )
        }
        
        return WATSubmission(
            id = submissionId,
            userId = "current_user",
            testId = "wat_standard",
            responses = mockResponses,
            totalTimeTakenMinutes = 15,
            submittedAt = System.currentTimeMillis(),
            status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
            aiPreliminaryScore = WATAIScore(
                overallScore = 75f,
                positivityScore = 16f,
                creativityScore = 15f,
                speedScore = 15f,
                relevanceScore = 14f,
                emotionalMaturityScore = 15f,
                feedback = "Good spontaneity and positive associations. Shows emotional maturity.",
                positiveWords = 35,
                negativeWords = 8,
                neutralWords = 11,
                uniqueResponsesCount = 54,
                repeatedPatterns = listOf("Leadership themes", "Action-oriented"),
                strengths = listOf("Positive outlook", "Quick responses"),
                areasForImprovement = listOf("Be more creative")
            )
        )
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

