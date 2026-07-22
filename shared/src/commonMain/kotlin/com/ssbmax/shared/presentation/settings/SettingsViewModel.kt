package com.ssbmax.shared.presentation.settings

import com.ssbmax.shared.domain.model.SubscriptionTier
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.util.DomainLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/settings/SettingsViewModel.kt`.
 *
 * Same plain-class + own-`CoroutineScope` pattern as every other Phase 5
 * ViewModel — NOT `androidx.lifecycle.ViewModel`; the screen uses
 * `koinInject()`. `close()` cancels the scope. `android.util.Log`-free
 * `ErrorLogger` calls replaced with [DomainLogger], same seam every other
 * ported ViewModel in this phase uses.
 */
class SettingsViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "SettingsViewModel"
    }

    init {
        observeSubscriptionTier()
    }

    fun close() {
        scope.cancel()
    }

    private fun observeSubscriptionTier() {
        scope.launch {
            observeCurrentUser()
                .catch { error ->
                    logger.e(TAG, "Failed to observe subscription tier", error)
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
