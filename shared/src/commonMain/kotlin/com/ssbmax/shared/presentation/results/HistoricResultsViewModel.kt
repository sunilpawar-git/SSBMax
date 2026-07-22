package com.ssbmax.shared.presentation.results

import com.ssbmax.shared.domain.model.TestType
import com.ssbmax.shared.domain.model.results.HistoricResult
import com.ssbmax.shared.domain.repository.AuthRepository
import com.ssbmax.shared.domain.usecase.results.GetHistoricResultsUseCase
import com.ssbmax.shared.domain.util.DomainLogger
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
 * KMP port of the Android `app/.../ui/results/HistoricResultsViewModel.kt`.
 *
 * Follows the plain-class + own-`CoroutineScope` ViewModel pattern this phase
 * already established (see [com.ssbmax.shared.presentation.profile.UserProfileViewModel]) --
 * NOT `androidx.lifecycle.ViewModel`; the screen uses `koinInject()`, not `koinViewModel()`.
 * `close()` cancels the scope, called from the screen's `DisposableEffect`.
 *
 * Deviation from the Android original: `ErrorLogger.log`/`logWithUser` (Android-only,
 * Crashlytics-backed) replaced with [DomainLogger], same seam every other ported
 * ViewModel in this phase uses. Behavior (load/filter/refresh) otherwise unchanged.
 *
 * Known pre-existing gap, NOT introduced by this port: [GetHistoricResultsUseCase]
 * (already ported to `shared` in an earlier phase, unchanged here) only actually
 * queries TAT submissions -- its own doc comment says "Similar logic for WAT, SRT, SD,
 * GTO tests, Interview / For brevity, focusing on TAT as example". So this screen will
 * only ever show TAT results regardless of `testTypeFilter`, on both Android and this
 * KMP port -- a real, pre-existing limitation carried forward unchanged, not silently
 * fixed (fixing it is domain-layer scope, not this UI-porting session's).
 */
class HistoricResultsViewModel(
    private val authRepository: AuthRepository,
    private val getHistoricResults: GetHistoricResultsUseCase,
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(HistoricResultsUiState())
    val uiState: StateFlow<HistoricResultsUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "HistoricResultsViewModel"
    }

    init {
        loadResults()
    }

    fun close() {
        scope.cancel()
    }

    /**
     * Load historic results for current user
     */
    fun loadResults(testTypeFilter: TestType? = null) {
        scope.launch {
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
                        logger.e(TAG, "Failed to load historic results for user: $userId", error)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load results"
                            )
                        }
                    }
            } catch (e: Exception) {
                logger.e(TAG, "Unexpected error loading historic results", e)
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
