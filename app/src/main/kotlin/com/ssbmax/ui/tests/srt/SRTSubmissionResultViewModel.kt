package com.ssbmax.ui.tests.srt

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
 * ViewModel for SRT Submission Result Screen
 */
@HiltViewModel
class SRTSubmissionResultViewModel @Inject constructor(
    // TODO: Inject SubmissionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SRTSubmissionResultUiState())
    val uiState: StateFlow<SRTSubmissionResultUiState> = _uiState.asStateFlow()
    
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
    
    private fun generateMockSubmission(submissionId: String): SRTSubmission {
        val mockResponses = (1..60).map { index ->
            SRTSituationResponse(
                situationId = "srt_s_$index",
                situation = "Situation $index",
                response = if (index % 10 == 0) "" else "I would take immediate action...",
                charactersCount = if (index % 10 == 0) 0 else 45,
                timeTakenSeconds = 25,
                submittedAt = System.currentTimeMillis(),
                isSkipped = index % 10 == 0
            )
        }
        
        return SRTSubmission(
            id = submissionId,
            userId = "current_user",
            testId = "srt_standard",
            responses = mockResponses,
            totalTimeTakenMinutes = 28,
            submittedAt = System.currentTimeMillis(),
            status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
            aiPreliminaryScore = SRTAIScore(
                overallScore = 76f,
                leadershipScore = 16f,
                decisionMakingScore = 15f,
                practicalityScore = 15f,
                initiativeScore = 15f,
                socialResponsibilityScore = 15f,
                feedback = "Shows good practical judgment and leadership qualities.",
                positiveTraits = listOf("Proactive", "Responsible", "Team-oriented"),
                concerningPatterns = emptyList(),
                responseQuality = ResponseQuality.GOOD,
                strengths = listOf("Leadership", "Practical solutions"),
                areasForImprovement = listOf("Consider more options")
            )
        )
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

