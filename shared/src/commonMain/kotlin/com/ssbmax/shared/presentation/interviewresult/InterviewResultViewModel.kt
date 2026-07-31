package com.ssbmax.shared.presentation.interviewresult

import com.ssbmax.shared.domain.repository.InterviewRepository
import com.ssbmax.shared.domain.scoring.EntryType
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.domain.validation.SSBRecommendationUIModel
import com.ssbmax.shared.domain.validation.ValidationIntegration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of `app/.../ui/interview/result/InterviewResultViewModel.kt`.
 *
 * Unlike PPDT/TAT/etc's result screens (which observe a live Firestore
 * listener while `analysisStatus` is still pending -- the "GTO pattern"),
 * this screen is reached only after [com.ssbmax.shared.presentation
 * .interviewsession.InterviewSessionViewModel] already shows its own
 * "results pending" dialog and navigates home; the Android original's
 * `InterviewResultScreen` is a plain one-shot `getResultById` fetch with no
 * polling/listener of its own (`isAnalysisPending` exists in the UiState
 * "for future history view" per its own doc comment, but nothing in this
 * ViewModel ever sets it `true` -- carried forward faithfully, not fixed,
 * matching this plan's own "port pre-existing gaps rather than silently
 * resolve them" precedent).
 *
 * Deviations from the Android original: real `androidx.lifecycle.ViewModel` +
 * `viewModelScope` (this phase's established pattern, `SavedStateHandle`
 * dropped -- `resultId` passed to [loadResult] directly, same as
 * [com.ssbmax.shared.presentation.interviewsession.InterviewSessionViewModel]);
 * `android.util.Log`/`ErrorLogger` -> [DomainLogger]; `trackMemoryLeaks`
 * dropped (`viewModelScope` cancels automatically, no manual tracking needed).
 */
class InterviewResultViewModel(
    private val interviewRepository: InterviewRepository,
    private val logger: DomainLogger
) : ViewModel() {
    private val tag = "InterviewResultViewModel"

    private val _uiState = MutableStateFlow(InterviewResultUiState())
    val uiState: StateFlow<InterviewResultUiState> = _uiState.asStateFlow()

    private var resultId: String? = null

    fun loadResult(resultId: String) {
        this.resultId = resultId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Loading results...") }

            try {
                val resultResult = interviewRepository.getResultById(resultId)

                if (resultResult.isFailure) {
                    logger.e(tag, "Failed to load interview result", resultResult.exceptionOrNull())
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = "Failed to load results") }
                    return@launch
                }

                val result = resultResult.getOrNull()
                if (result == null) {
                    _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = "Result not found") }
                    return@launch
                }

                val ssbRecommendation = if (result.overallOLQScores.isNotEmpty()) {
                    val validationResult = ValidationIntegration.validateScores(
                        scores = result.overallOLQScores,
                        entryType = EntryType.NDA
                    )
                    SSBRecommendationUIModel.fromValidationResult(validationResult, EntryType.NDA)
                } else null

                _uiState.update {
                    it.copy(isLoading = false, loadingMessage = null, result = result, ssbRecommendation = ssbRecommendation)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(tag, "Exception loading interview result", e)
                _uiState.update { it.copy(isLoading = false, loadingMessage = null, error = "An error occurred") }
            }
        }
    }

    /** Reload result (pull to refresh). */
    fun refresh() {
        resultId?.let { loadResult(it) }
    }
}
