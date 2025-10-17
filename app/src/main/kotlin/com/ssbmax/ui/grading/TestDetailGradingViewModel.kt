package com.ssbmax.ui.grading

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
 * ViewModel for Test Detail Grading Screen
 */
@HiltViewModel
class TestDetailGradingViewModel @Inject constructor(
    // TODO: Inject GradingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TestDetailGradingUiState())
    val uiState: StateFlow<TestDetailGradingUiState> = _uiState.asStateFlow()
    
    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // TODO: Load from repository
                val submission = generateMockSubmission(submissionId)
                
                _uiState.update { it.copy(
                    isLoading = false,
                    submission = submission,
                    submissionId = submissionId
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }
    
    fun updatePerception(value: Float) {
        _uiState.update { state ->
            val total = value + state.imagination + state.narration + 
                       state.characterDepiction + state.positivity
            state.copy(perception = value, totalScore = total)
        }
    }
    
    fun updateImagination(value: Float) {
        _uiState.update { state ->
            val total = state.perception + value + state.narration + 
                       state.characterDepiction + state.positivity
            state.copy(imagination = value, totalScore = total)
        }
    }
    
    fun updateNarration(value: Float) {
        _uiState.update { state ->
            val total = state.perception + state.imagination + value + 
                       state.characterDepiction + state.positivity
            state.copy(narration = value, totalScore = total)
        }
    }
    
    fun updateCharacterDepiction(value: Float) {
        _uiState.update { state ->
            val total = state.perception + state.imagination + state.narration + 
                       value + state.positivity
            state.copy(characterDepiction = value, totalScore = total)
        }
    }
    
    fun updatePositivity(value: Float) {
        _uiState.update { state ->
            val total = state.perception + state.imagination + state.narration + 
                       state.characterDepiction + value
            state.copy(positivity = value, totalScore = total)
        }
    }
    
    fun updateFeedback(feedback: String) {
        _uiState.update { it.copy(feedback = feedback) }
    }
    
    fun acceptAISuggestions() {
        val aiScore = _uiState.value.submission?.aiPreliminaryScore ?: return
        
        _uiState.update { state ->
            state.copy(
                perception = aiScore.perceptionScore,
                imagination = aiScore.imaginationScore,
                narration = aiScore.narrationScore,
                characterDepiction = aiScore.characterDepictionScore,
                positivity = aiScore.positivityScore,
                totalScore = aiScore.overallScore,
                feedback = aiScore.feedback ?: ""
            )
        }
    }
    
    fun submitGrade() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            
            try {
                // TODO: Submit to repository
                val state = _uiState.value
                
                // Simulate submission delay
                kotlinx.coroutines.delay(1000)
                
                _uiState.update { it.copy(
                    isSubmitting = false,
                    isGraded = true
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isSubmitting = false,
                    error = e.message
                ) }
            }
        }
    }
    
    private fun generateMockSubmission(submissionId: String): PPDTSubmissionWithDetails {
        return PPDTSubmissionWithDetails(
            submissionId = submissionId,
            userId = "student1",
            userName = "Rahul Kumar",
            testId = "ppdt_standard",
            story = """
                The picture shows a young officer standing at the edge of a military base, 
                looking at a helicopter in the distance. He is the base commander who has 
                just received news of an emergency rescue mission. Without hesitation, he 
                gathers his team and briefs them on the situation. Despite the challenging 
                weather conditions, he leads the mission with courage and determination. 
                The team successfully rescues the stranded civilians and returns safely. 
                The officer is commended for his quick thinking and leadership.
            """.trimIndent(),
            charactersCount = 412,
            submittedAt = System.currentTimeMillis() - (2 * 60 * 60 * 1000),
            status = SubmissionStatus.SUBMITTED_PENDING_REVIEW,
            aiPreliminaryScore = PPDTAIScore(
                perceptionScore = 16f,
                imaginationScore = 15f,
                narrationScore = 14f,
                characterDepictionScore = 15f,
                positivityScore = 15f,
                overallScore = 75f,
                feedback = "Good observation skills and positive outlook. Story shows leadership qualities. " +
                          "Could improve on character depth and conflict resolution."
            ),
            batchName = "Batch Alpha"
        )
    }
}

/**
 * UI State for Test Detail Grading Screen
 */
data class TestDetailGradingUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isGraded: Boolean = false,
    val submissionId: String? = null,
    val submission: PPDTSubmissionWithDetails? = null,
    
    // Grading scores (out of 20 each)
    val perception: Float = 0f,
    val imagination: Float = 0f,
    val narration: Float = 0f,
    val characterDepiction: Float = 0f,
    val positivity: Float = 0f,
    
    // Calculated total (out of 100)
    val totalScore: Float = 0f,
    
    // Feedback
    val feedback: String = "",
    
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = totalScore > 0 && feedback.isNotBlank() && !isSubmitting
}

