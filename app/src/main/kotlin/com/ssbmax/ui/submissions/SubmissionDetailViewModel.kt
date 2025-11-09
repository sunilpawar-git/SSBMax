package com.ssbmax.ui.submissions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.usecase.submission.ObserveSubmissionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Submission Detail Screen
 * Shows detailed view of a single submission with scores and feedback
 */
@HiltViewModel
class SubmissionDetailViewModel @Inject constructor(
    private val observeSubmission: ObserveSubmissionUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val submissionId: String = savedStateHandle["submissionId"] ?: ""
    
    private val _uiState = MutableStateFlow(SubmissionDetailUiState())
    val uiState: StateFlow<SubmissionDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadSubmission()
    }
    
    private fun loadSubmission() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                observeSubmission(submissionId)
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5000),
                        initialValue = null
                    )
                    .collect { submissionData ->
                    if (submissionData == null) {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Submission not found"
                        ) }
                        return@collect
                    }
                    
                    // Parse submission data
                    val testType = TestType.valueOf(submissionData["testType"] as? String ?: "TAT")
                    val status = SubmissionStatus.valueOf(submissionData["status"] as? String ?: "DRAFT")
                    val submittedAt = submissionData["submittedAt"] as? Long ?: 0L
                    
                    val data = submissionData["data"] as? Map<*, *>
                    
                    _uiState.update { it.copy(
                        isLoading = false,
                        submissionId = submissionId,
                        testType = testType,
                        status = status,
                        submittedAt = submittedAt,
                        aiScore = parseAIScore(data, testType),
                        instructorScore = parseInstructorScore(data, testType),
                        error = null
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to load submission: ${e.message}"
                ) }
            }
        }
    }
    
    private fun parseAIScore(data: Map<*, *>?, testType: TestType): ScoreDetails? {
        val aiScore = data?.get("aiPreliminaryScore") as? Map<*, *> ?: return null
        
        return ScoreDetails(
            overallScore = (aiScore["overallScore"] as? Number)?.toFloat() ?: 0f,
            feedback = aiScore["feedback"] as? String,
            strengths = (aiScore["strengths"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            areasForImprovement = (aiScore["areasForImprovement"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            isAIGenerated = true
        )
    }
    
    private fun parseInstructorScore(data: Map<*, *>?, testType: TestType): ScoreDetails? {
        val instructorScore = data?.get("instructorScore") as? Map<*, *> ?: return null
        
        return ScoreDetails(
            overallScore = (instructorScore["overallScore"] as? Number)?.toFloat() ?: 0f,
            feedback = instructorScore["feedback"] as? String,
            strengths = emptyList(),
            areasForImprovement = emptyList(),
            isAIGenerated = false,
            gradedBy = instructorScore["gradedByInstructorName"] as? String,
            gradedAt = instructorScore["gradedAt"] as? Long
        )
    }
    
    fun retry() {
        loadSubmission()
    }
}

/**
 * UI State for Submission Detail
 */
data class SubmissionDetailUiState(
    val isLoading: Boolean = true,
    val submissionId: String = "",
    val testType: TestType = TestType.TAT,
    val status: SubmissionStatus = SubmissionStatus.DRAFT,
    val submittedAt: Long = 0L,
    val aiScore: ScoreDetails? = null,
    val instructorScore: ScoreDetails? = null,
    val error: String? = null
) {
    val testName: String
        get() = when (testType) {
            TestType.TAT -> "TAT - Thematic Apperception Test"
            TestType.WAT -> "WAT - Word Association Test"
            TestType.SRT -> "SRT - Situation Reaction Test"
            TestType.PPDT -> "PPDT - Picture Perception Test"
            TestType.PIQ -> "PIQ - Personal Information Questionnaire"
            TestType.SD -> "SD - Self Description"
            TestType.OIR -> "OIR - Officers Intelligence Rating"
            TestType.GTO -> "GTO - Group Testing Officer"
            TestType.IO -> "IO - Interview Officer"
        }
    
    val finalScore: Float?
        get() = instructorScore?.overallScore ?: aiScore?.overallScore
    
    val hasScore: Boolean
        get() = aiScore != null || instructorScore != null
    
    val timeAgo: String
        get() {
            val diff = System.currentTimeMillis() - submittedAt
            val hours = diff / (1000 * 60 * 60)
            val days = hours / 24
            
            return when {
                days > 30 -> "${days / 30}mo ago"
                days > 0 -> "${days}d ago"
                hours > 0 -> "${hours}h ago"
                else -> "Just now"
            }
        }
}

/**
 * Score details for display
 */
data class ScoreDetails(
    val overallScore: Float,
    val feedback: String?,
    val strengths: List<String>,
    val areasForImprovement: List<String>,
    val isAIGenerated: Boolean,
    val gradedBy: String? = null,
    val gradedAt: Long? = null
) {
    val scorePercentage: Int
        get() = overallScore.toInt()
    
    val scoreGrade: String
        get() = when {
            overallScore >= 90 -> "A+"
            overallScore >= 80 -> "A"
            overallScore >= 70 -> "B"
            overallScore >= 60 -> "C"
            overallScore >= 50 -> "D"
            else -> "F"
        }
}

