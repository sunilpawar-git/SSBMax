package com.ssbmax.ui.interview.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.util.trackMemoryLeaks
import com.ssbmax.core.domain.repository.InterviewRepository
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
 * ViewModel for Interview Result screen
 *
 * Responsibilities:
 * - Load interview result by ID
 * - Display OLQ scores and analysis
 * - Show strengths/weaknesses
 * - Provide performance feedback
 *
 * MEMORY LEAK PREVENTION:
 * - Registers with MemoryLeakTracker
 * - Uses viewModelScope for all coroutines (auto-cancelled)
 * - No static references or context leaks
 */
@HiltViewModel
class InterviewResultViewModel @Inject constructor(
    private val interviewRepository: InterviewRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val resultId: String = checkNotNull(savedStateHandle.get<String>("resultId")) {
        "resultId is required"
    }

    private val _uiState = MutableStateFlow(InterviewResultUiState())
    val uiState: StateFlow<InterviewResultUiState> = _uiState.asStateFlow()

    init {
        // Register for memory leak tracking
        trackMemoryLeaks("InterviewResultViewModel")

        // Load result
        loadResult()
    }

    /**
     * Load interview result
     */
    private fun loadResult() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingMessage = "Loading results..."
                )
            }

            try {
                val resultResult = interviewRepository.getResultById(resultId)

                if (resultResult.isFailure) {
                    ErrorLogger.log(
                        throwable = resultResult.exceptionOrNull() ?: Exception("Unknown error"),
                        description = "Failed to load interview result"
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Failed to load results"
                        )
                    }
                    return@launch
                }

                val result = resultResult.getOrNull()
                if (result == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            error = "Result not found"
                        )
                    }
                    return@launch
                }

                // Compute SSB recommendation if OLQ scores are available
                val ssbRecommendation = if (result.overallOLQScores.isNotEmpty()) {
                    val validationResult = ValidationIntegration.validateScores(
                        scores = result.overallOLQScores,
                        entryType = EntryType.NDA
                    )
                    SSBRecommendationUIModel.fromValidationResult(validationResult, EntryType.NDA)
                } else null

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        result = result,
                        ssbRecommendation = ssbRecommendation
                    )
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Exception loading interview result")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        error = "An error occurred"
                    )
                }
            }
        }
    }

    /**
     * Reload result (pull to refresh)
     */
    fun refresh() {
        loadResult()
    }
}
