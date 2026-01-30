package com.ssbmax.ui.tests.ppdt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.scoring.AnalysisStatus
import com.ssbmax.core.domain.model.scoring.OLQAnalysisResult
import com.ssbmax.core.domain.repository.SubmissionRepository
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
 * ViewModel for PPDT Submission Result Screen
 * Fetches submission data from SubmissionRepository
 */
@HiltViewModel
class PPDTSubmissionResultViewModel @Inject constructor(
    private val submissionRepository: SubmissionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PPDTSubmissionResultUiState())
    val uiState: StateFlow<PPDTSubmissionResultUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "PPDTSubmissionResultViewModel"
    }
    
    init {
        android.util.Log.d(TAG, "🚀 PPDTSubmissionResultViewModel initialized")
    }
    
    fun loadSubmission(submissionId: String) {
        android.util.Log.d(TAG, "📥 Loading submission: $submissionId")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Observe submission for status changes (GTO dual-fetch pattern)
                submissionRepository.observePPDTSubmission(submissionId).collect { submission ->
                    android.util.Log.d(TAG, "🔄 Firestore snapshot received for: $submissionId")
                    android.util.Log.d(TAG, "   - Submission exists: ${submission != null}")
                    
                    if (submission == null) {
                        android.util.Log.e(TAG, "❌ Submission not found in snapshot")
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Submission not found"
                        ) }
                        return@collect
                    }
                    
                    android.util.Log.d(TAG, "   - analysisStatus: ${submission.analysisStatus}")
                    
                    // Update UI with submission data (story, metadata)
                    _uiState.update { it.copy(
                        isLoading = false,
                        submission = submission
                    ) }
                    
                    // GTO PATTERN: If analysis is completed, fetch results from ppdt_results collection
                    if (submission.analysisStatus == AnalysisStatus.COMPLETED) {
                        android.util.Log.d(TAG, "✅ Status is COMPLETED, loading result from ppdt_results...")
                        loadResult(submissionId)
                    } else {
                        android.util.Log.d(TAG, "⏳ Status is ${submission.analysisStatus}, waiting for completion...")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Exception while observing submission", e)
                ErrorLogger.logTestError(e, "Failed to load PPDT submission result", "PPDT")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load submission"
                ) }
            }
        }
    }
    
    /**
     * Load OLQ result from ppdt_results collection (GTO pattern)
     * This is called when submission status changes to COMPLETED
     */
    private suspend fun loadResult(submissionId: String) {
        try {
            android.util.Log.d(TAG, "📈 Loading OLQ result from ppdt_results...")
            
            val result = submissionRepository.getPPDTResult(submissionId)
            
            if (result.isSuccess) {
                val olqResult = result.getOrNull()
                android.util.Log.d(TAG, "✅ Result loaded: ${olqResult?.olqScores?.size} OLQ scores")
                
                // Compute SSB recommendation if we have scores
                val ssbRecommendation = olqResult?.olqScores?.let { scores ->
                    if (scores.isNotEmpty()) {
                        val validationResult = ValidationIntegration.validateScores(
                            scores = scores,
                            entryType = EntryType.NDA // TODO: Get from user profile
                        )
                        SSBRecommendationUIModel.fromValidationResult(
                            validationResult, 
                            EntryType.NDA
                        )
                    } else null
                }
                
                // Update submission with OLQ result and recommendation
                _uiState.update { currentState ->
                    currentState.submission?.let { submission ->
                        currentState.copy(
                            submission = submission.copy(olqResult = olqResult),
                            ssbRecommendation = ssbRecommendation
                        )
                    } ?: currentState
                }
            } else {
                android.util.Log.w(TAG, "⚠️ Result not yet available", result.exceptionOrNull())
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error loading OLQ result", e)
            ErrorLogger.logTestError(e, "Failed to load PPDT OLQ result", "PPDT")
        }
    }


}

/**
 * UI State for PPDT Submission Result
 */
data class PPDTSubmissionResultUiState(
    override val isLoading: Boolean = true,
    val submission: PPDTSubmission? = null,
    override val ssbRecommendation: SSBRecommendationUIModel? = null,
    override val error: String? = null
) : com.ssbmax.ui.components.result.UnifiedResultUiState {
    override val analysisStatus: AnalysisStatus
        get() = submission?.analysisStatus ?: AnalysisStatus.PENDING_ANALYSIS
    override val olqResult: OLQAnalysisResult?
        get() = submission?.olqResult
}
