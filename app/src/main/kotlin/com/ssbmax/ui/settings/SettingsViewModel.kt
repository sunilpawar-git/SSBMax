package com.ssbmax.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.utils.ErrorLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Settings Screen
 * Observes user subscription tier and manages shared error state
 */
class SettingsViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSubscriptionTier()
    }

    /**
     * Observe subscription tier from current user
     */
    private fun observeSubscriptionTier() {
        viewModelScope.launch {
            try {
                observeCurrentUser()
                    .catch { error ->
                        ErrorLogger.log(error, "Failed to observe subscription tier")
                        _uiState.update { it.copy(error = "Failed to load subscription") }
                    }
                    .collect { user ->
                        _uiState.update {
                            it.copy(
                                subscriptionTier = user?.subscriptionTier ?: SubscriptionTier.FREE,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Unexpected error in observeSubscriptionTier")
                _uiState.update { it.copy(error = "Failed to load subscription") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for Settings Screen
 * Simplified after removing migration state properties
 */
data class SettingsUiState(
    val subscriptionTier: com.ssbmax.shared.domain.model.SubscriptionTier = com.ssbmax.shared.domain.model.SubscriptionTier.FREE,
    val error: String? = null
)
