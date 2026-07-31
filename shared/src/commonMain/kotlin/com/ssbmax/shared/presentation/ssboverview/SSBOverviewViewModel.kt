package com.ssbmax.shared.presentation.ssboverview

import com.ssbmax.shared.domain.model.SSBInfoCard
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.domain.util.SSBContentProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/ssboverview/SSBOverviewViewModel.kt`.
 * SSB overview content is static/educational, loaded from
 * [SSBContentProvider] -- no repository needed unless content becomes
 * dynamic/user-specific, same as the Android original's own note.
 *
 * Uses a real `androidx.lifecycle.ViewModel` with `viewModelScope` (Phase 1 of
 * the KMP-convergence plan, see
 * [com.ssbmax.shared.presentation.oir.OIRTestViewModel]'s doc comment for the
 * precedent this mirrors). `ErrorLogger.log` (Android-only, Crashlytics-backed)
 * replaced with [DomainLogger], same seam every other ported ViewModel in
 * this phase uses.
 */
class SSBOverviewViewModel(
    private val logger: DomainLogger
) : ViewModel() {
    private val _uiState = MutableStateFlow(SSBOverviewUiState())
    val uiState: StateFlow<SSBOverviewUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "SSBOverviewViewModel"
    }

    init {
        loadSSBContent()
    }

    private fun loadSSBContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val cards = SSBContentProvider.getInfoCards()

                if (cards.isEmpty()) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "No SSB information available")
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(infoCards = cards, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error loading SSB overview information", e)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load SSB information")
                }
            }
        }
    }

    fun toggleCardExpansion(cardId: String) {
        _uiState.update { state ->
            state.copy(
                expandedCardIds = if (state.expandedCardIds.contains(cardId)) {
                    state.expandedCardIds - cardId
                } else {
                    state.expandedCardIds + cardId
                }
            )
        }
    }

    fun refresh() {
        loadSSBContent()
    }
}

/**
 * UI State for SSB Overview Screen
 */
data class SSBOverviewUiState(
    val infoCards: List<SSBInfoCard> = emptyList(),
    val expandedCardIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)
