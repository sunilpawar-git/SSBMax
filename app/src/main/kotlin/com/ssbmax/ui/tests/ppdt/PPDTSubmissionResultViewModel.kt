package com.ssbmax.ui.tests.ppdt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for PPDT Submission Result Screen
 */
@HiltViewModel
class PPDTSubmissionResultViewModel @Inject constructor(
    // TODO: Inject PPDTSubmissionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PPDTSubmissionResultUiState())
    val uiState: StateFlow<PPDTSubmissionResultUiState> = _uiState.asStateFlow()
    
    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // TODO: Load from repository
                val submission = generateMockSubmission(submissionId)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    submission = submission
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    private fun generateMockSubmission(submissionId: String): PPDTSubmission {
        return PPDTSubmission(
            submissionId = submissionId,
            questionId = "ppdt_q1",
            userId = "mock-user-id",
            userName = "SSB Aspirant",
            userEmail = "aspirant@ssbmax.com",
            batchId = null,
            story = "Mock story content that the student wrote. This would be the actual story they submitted during the test...",
            charactersCount = 250,
            viewingTimeTakenSeconds = 30,
            writingTimeTakenMinutes = 3,
            submittedAt = System.currentTimeMillis(),
            status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
            aiPreliminaryScore = PPDTAIScore(
                perceptionScore = 16f,
                imaginationScore = 14f,
                narrationScore = 15f,
                characterDepictionScore = 14f,
                positivityScore = 17f,
                overallScore = 76f,
                feedback = "Good overall story with positive outlook. Could improve imagination.",
                strengths = listOf(
                    "Good character development",
                    "Positive outlook"
                ),
                areasForImprovement = listOf(
                    "Could be more imaginative"
                )
            ),
            instructorReview = null
        )
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

