package com.ssbmax.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.core.domain.model.results.HistoricResult
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.usecase.results.GetHistoricResultsUseCase
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Historic Results Screen
 * Displays past test submissions from last 6 months
 */
@HiltViewModel
class HistoricResultsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getHistoricResults: GetHistoricResultsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoricResultsUiState())
    val uiState: StateFlow<HistoricResultsUiState> = _uiState.asStateFlow()

    init {
        loadResults()
    }

    /**
     * Load historic results for current user
     */
    fun loadResults(testTypeFilter: TestType? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val userId = authRepository.currentUser.value?.id
                if (userId == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Please login to view results"
                        )
                    }
                    return@launch
                }

                getHistoricResults(userId, testTypeFilter)
                    .onSuccess { results ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                results = results,
                                selectedFilter = testTypeFilter,
                                error = null
                            )
                        }
                    }
                    .onFailure { error ->
                        ErrorLogger.logWithUser(error, "Failed to load historic results", userId)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load results"
                            )
                        }
                    }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Unexpected error loading historic results")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "An unexpected error occurred"
                    )
                }
            }
        }
    }

    /**
     * Filter results by test type
     */
    fun filterByTestType(testType: TestType?) {
        loadResults(testType)
    }

    /**
     * Refresh results
     */
    fun refresh() {
        loadResults(_uiState.value.selectedFilter)
    }
}

/**
 * UI State for Historic Results Screen
 */
data class HistoricResultsUiState(
    val isLoading: Boolean = false,
    val results: List<HistoricResult> = emptyList(),
    val selectedFilter: TestType? = null,
    val error: String? = null
)
