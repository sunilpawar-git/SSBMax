package com.ssbmax.ui.tests.gpe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.model.gto.GTOResult
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.core.domain.model.gto.GTOSubmissionStatus
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.core.domain.scoring.EntryType
import com.ssbmax.core.domain.validation.SSBRecommendationUIModel
import com.ssbmax.core.domain.validation.ValidationIntegration
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for GPE Submission Result Screen
 * 
 * Displays:
 * - Submission details (scenario, plan, character count)
 * - Analysis status (PENDING → ANALYZING → COMPLETED)
 * - OLQ scores (15 Officer-Like Qualities)
 * - Overall rating and performance summary
 * - Strengths and areas for improvement
 */
@HiltViewModel
class GPESubmissionResultViewModel @Inject constructor(
    private val gtoRepository: GTORepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GPESubmissionResultUiState())
    val uiState: StateFlow<GPESubmissionResultUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "GPEResultViewModel"
    }
    
    init {
        trackMemoryLeaks("GPESubmissionResultViewModel")
        android.util.Log.d(TAG, "🚀 GPESubmissionResultViewModel initialized")
    }
    
    /**
     * Load submission and observe for real-time updates
     */
    fun loadSubmission(submissionId: String) {
        android.util.Log.d(TAG, "📥 Loading submission: $submissionId")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Observe submission for real-time status updates
                gtoRepository.observeSubmission(submissionId).collect { submission ->
                    android.util.Log.d(TAG, "📊 Submission update received: ${submission?.status}")
                    
                    if (submission == null) {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Submission not found"
                        ) }
                        return@collect
                    }
                    
                    // Update UI with submission data
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            submission = submission as? GTOSubmission.GPESubmission,
                            error = null
                        )
                    }
                    
                    // If analysis is completed, load the result with OLQ scores
                    if (submission.status == GTOSubmissionStatus.COMPLETED) {
                        loadResult(submissionId)
                    }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to load GPE submission")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to load submission. Please try again."
                ) }
            }
        }
    }
    
    /**
     * Load detailed result with OLQ analysis
     */
    private suspend fun loadResult(submissionId: String) {
        try {
            android.util.Log.d(TAG, "📈 Loading result analysis...")
            
            val resultResult = gtoRepository.getTestResult(submissionId)
            
            if (resultResult.isSuccess) {
                val result = resultResult.getOrNull()
                android.util.Log.d(TAG, "✅ Result loaded: ${result?.olqScores?.size} OLQ scores")
                
                // Compute SSB recommendation if OLQ scores are available
                val ssbRecommendation = result?.olqScores?.let { scores ->
                    if (scores.isNotEmpty()) {
                        val validationResult = ValidationIntegration.validateScores(
                            scores = scores,
                            entryType = EntryType.NDA
                        )
                        SSBRecommendationUIModel.fromValidationResult(validationResult, EntryType.NDA)
                    } else null
                }
                
                _uiState.update { it.copy(result = result, ssbRecommendation = ssbRecommendation) }
            } else {
                android.util.Log.w(TAG, "⚠️ Result not yet available", resultResult.exceptionOrNull())
            }
        } catch (e: Exception) {
            ErrorLogger.logTestError(e, "Failed to load GPE result", "GTO")
        }
    }
    
    /**
     * Retry loading if there was an error
     */
    fun retry(submissionId: String) {
        loadSubmission(submissionId)
    }
    
    override fun onCleared() {
        super.onCleared()
        android.util.Log.d(TAG, "🧹 ViewModel cleared")
    }
}

/**
 * GPE Result UI State
 */
data class GPESubmissionResultUiState(
    val isLoading: Boolean = true,
    val submission: GTOSubmission.GPESubmission? = null,
    val result: GTOResult? = null,
    val ssbRecommendation: SSBRecommendationUIModel? = null,
    val error: String? = null
) {
    /**
     * Check if analysis is still in progress
     */
    val isAnalyzing: Boolean
        get() = submission != null && 
                (submission.status == GTOSubmissionStatus.PENDING_ANALYSIS || 
                 submission.status == GTOSubmissionStatus.ANALYZING)
    
    /**
     * Check if analysis is completed
     */
    val isCompleted: Boolean
        get() = submission?.status == GTOSubmissionStatus.COMPLETED && result != null
    
    /**
     * Check if analysis failed
     */
    val isFailed: Boolean
        get() = submission?.status == GTOSubmissionStatus.FAILED
    
    /**
     * Get formatted time spent
     */
    val formattedTimeSpent: String
        get() {
            val timeSpent = submission?.timeSpent ?: 0
            val minutes = timeSpent / 60
            val seconds = timeSpent % 60
            return "${minutes}m ${seconds}s"
        }
}
