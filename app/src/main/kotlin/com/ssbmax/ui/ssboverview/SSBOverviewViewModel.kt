package com.ssbmax.ui.ssboverview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.SSBInfoCard
import com.ssbmax.core.domain.model.SSBInfoIcon
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SSB Overview Screen
 * Manages informational content about SSB selection process
 * 
 * Note: SSB overview content is currently code-based (SSBContentProvider)
 * This is appropriate for static educational content that rarely changes.
 * TODO: Consider CMS integration if content needs frequent updates
 */
@HiltViewModel
class SSBOverviewViewModel @Inject constructor(
    // SSB overview content is static/educational, loaded from SSBContentProvider
    // No repository needed unless content becomes dynamic/user-specific
) : ViewModel() {

    private val _uiState = MutableStateFlow(SSBOverviewUiState())
    val uiState: StateFlow<SSBOverviewUiState> = _uiState.asStateFlow()

    init {
        loadSSBContent()
    }

    private fun loadSSBContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load static SSB information cards
                val cards = SSBContentProvider.getInfoCards()

                if (cards.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "No SSB information available"
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        infoCards = cards,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Error loading SSB overview information")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load SSB information"
                    )
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

