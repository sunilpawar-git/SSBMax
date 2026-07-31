package com.ssbmax.shared.presentation.oirresult

import com.ssbmax.shared.domain.model.OIRTestResult
import com.ssbmax.shared.domain.usecase.GetOirResultUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 *
 * A real `androidx.lifecycle.ViewModel` using `viewModelScope` (Phase 1 of
 * the KMP-convergence plan) — the screen uses `koinViewModel()`.
 */
class OirResultViewModel(
    private val getOirResultUseCase: GetOirResultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OirResultUiState())
    val uiState: StateFlow<OirResultUiState> = _uiState.asStateFlow()

    fun loadSubmission(submissionId: String) {
        viewModelScope.launch {
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
}

data class OirResultUiState(
    val isLoading: Boolean = true,
    val result: OIRTestResult? = null,
    val error: String? = null
)
