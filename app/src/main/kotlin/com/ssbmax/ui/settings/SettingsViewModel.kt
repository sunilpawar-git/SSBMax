package com.ssbmax.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings Screen
 * Observes user subscription tier and manages shared error state
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
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
    val subscriptionTier: com.ssbmax.core.domain.model.SubscriptionTier = com.ssbmax.core.domain.model.SubscriptionTier.FREE,
    val error: String? = null
)
