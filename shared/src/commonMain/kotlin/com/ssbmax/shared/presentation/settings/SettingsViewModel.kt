package com.ssbmax.shared.presentation.settings

import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.subscription.GetSubscriptionTierUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/settings/SettingsViewModel.kt`.
 *
 * Phase 1 of the KMP-convergence plan: a real `androidx.lifecycle.ViewModel`
 * using `viewModelScope`, converged with `app`'s existing ViewModel idiom and
 * this module's own DI (`viewModelOf`) / screen (`koinViewModel()`)
 * conventions — no more manual `CoroutineScope` + `close()`. `android.util.Log`-free
 * `ErrorLogger` calls replaced with [DomainLogger], same seam every other
 * ported ViewModel in this phase uses.
 */
class SettingsViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val getSubscriptionTier: GetSubscriptionTierUseCase,
    private val logger: DomainLogger
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "SettingsViewModel"
    }

    init {
        observeSubscriptionTier()
    }

    private fun observeSubscriptionTier() {
        viewModelScope.launch {
            observeCurrentUser()
                .catch { error ->
                    logger.e(TAG, "Failed to observe subscription tier", error)
                    _uiState.update { it.copy(error = "Failed to load subscription") }
                }
                .collect { user ->
                    val userId = user?.id
                    if (userId == null) {
                        _uiState.update { it.copy(subscriptionTier = SubscriptionTier.FREE, error = null) }
                        return@collect
                    }
                    getSubscriptionTier(userId)
                        .onSuccess { tier ->
                            _uiState.update { it.copy(subscriptionTier = tier, error = null) }
                        }
                        .onFailure { error ->
                            logger.e(TAG, "Failed to load subscription tier", error)
                            _uiState.update {
                                it.copy(subscriptionTier = SubscriptionTier.FREE, error = error.message)
                            }
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
 */
data class SettingsUiState(
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val error: String? = null
)
