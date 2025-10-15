package com.ssbmax.ui.tests.oir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.OIRTestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for OIR Test Result Screen
 */
@HiltViewModel
class OIRTestResultViewModel @Inject constructor(
    // TODO: Inject OIRTestRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OIRTestResultUiState())
    val uiState: StateFlow<OIRTestResultUiState> = _uiState.asStateFlow()
    
    fun loadResult(sessionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // TODO: Load from repository
                // For now, use mock result
                val result = generateMockResult(sessionId)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    result = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    private fun generateMockResult(sessionId: String): OIRTestResult {
        // TODO: Replace with repository call
        return OIRTestResult(
            testId = "oir_standard",
            sessionId = sessionId,
            userId = "mock-user-id",
            totalQuestions = 50,
            correctAnswers = 42,
            incorrectAnswers = 6,
            skippedQuestions = 2,
            totalTimeSeconds = 2400,
            timeTakenSeconds = 2100,
            rawScore = 85,
            percentageScore = 84f,
            categoryScores = emptyMap(), // TODO: Add mock category scores
            difficultyBreakdown = emptyMap(), // TODO: Add mock difficulty scores
            answeredQuestions = emptyList(),
            completedAt = System.currentTimeMillis()
        )
    }
}

/**
 * UI State for OIR Test Result Screen
 */
data class OIRTestResultUiState(
    val isLoading: Boolean = true,
    val result: OIRTestResult? = null,
    val error: String? = null
)

