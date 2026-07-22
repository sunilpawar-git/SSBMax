package com.ssbmax.shared.presentation.ssboverview

import com.ssbmax.shared.domain.model.SSBInfoCard
import com.ssbmax.shared.domain.util.DomainLogger
import com.ssbmax.shared.domain.util.SSBContentProvider
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
 * KMP port of the Android `app/.../ui/ssboverview/SSBOverviewViewModel.kt`.
 * SSB overview content is static/educational, loaded from
 * [SSBContentProvider] -- no repository needed unless content becomes
 * dynamic/user-specific, same as the Android original's own note.
 *
 * Follows the plain-class + own-`CoroutineScope` ViewModel pattern this phase
 * already established -- NOT `androidx.lifecycle.ViewModel`.
 * `ErrorLogger.log` (Android-only, Crashlytics-backed) replaced with
 * [DomainLogger], same seam every other ported ViewModel in this phase uses.
 */
class SSBOverviewViewModel(
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(SSBOverviewUiState())
    val uiState: StateFlow<SSBOverviewUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "SSBOverviewViewModel"
    }

    init {
        loadSSBContent()
    }

    fun close() {
        scope.cancel()
    }

    private fun loadSSBContent() {
        scope.launch {
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
