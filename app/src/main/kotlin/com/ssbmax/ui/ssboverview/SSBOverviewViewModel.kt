package com.ssbmax.ui.ssboverview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.SSBInfoCard
import com.ssbmax.core.domain.model.SSBInfoIcon
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
 */
@HiltViewModel
class SSBOverviewViewModel @Inject constructor(
    // TODO: Inject SSBContentRepository when backend is ready
) : ViewModel() {

    private val _uiState = MutableStateFlow(SSBOverviewUiState())
    val uiState: StateFlow<SSBOverviewUiState> = _uiState.asStateFlow()

    init {
        loadSSBContent()
    }

    private fun loadSSBContent() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val cards = SSBContentProvider.getInfoCards()
                _uiState.update {
                    it.copy(
                        infoCards = cards,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
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

