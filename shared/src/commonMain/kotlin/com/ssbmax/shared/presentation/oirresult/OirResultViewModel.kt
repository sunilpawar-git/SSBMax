package com.ssbmax.shared.presentation.oirresult

import com.ssbmax.shared.domain.model.OIRTestResult
import com.ssbmax.shared.domain.usecase.GetOirResultUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Koin-injected ViewModel for the Phase 0 KMP spike's OIR-result slice.
 * Mirrors OIRSubmissionResultViewModel's UiState shape (data class, not
 * sealed — matches the existing convention for this specific screen) and
 * public method names (loadSubmission/retry) so the eventual UI port in
 * Phase 5 is a near drop-in.
 */
class OirResultViewModel(
    private val getOirResultUseCase: GetOirResultUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(OirResultUiState())
    val uiState: StateFlow<OirResultUiState> = _uiState.asStateFlow()

    fun loadSubmission(submissionId: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getOirResultUseCase(submissionId)
                .onSuccess { result ->
                    if (result == null) {
                        _uiState.update { it.copy(isLoading = false, error = "Submission not found") }
                    } else {
                        _uiState.update { it.copy(isLoading = false, result = result) }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to load submission") }
                }
        }
    }

    fun retry(submissionId: String) = loadSubmission(submissionId)

    fun close() {
        scope.cancel()
    }
}

data class OirResultUiState(
    val isLoading: Boolean = true,
    val result: OIRTestResult? = null,
    val error: String? = null
)
