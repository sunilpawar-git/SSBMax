package com.ssbmax.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.SubscriptionTier
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
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
            observeCurrentUser().collect { user ->
                _uiState.update { 
                    it.copy(subscriptionTier = user?.subscriptionTier ?: SubscriptionTier.FREE) 
                }
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
