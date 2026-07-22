package com.ssbmax.shared.presentation.settings.notifications

import com.ssbmax.shared.domain.model.NotificationPreferences
import com.ssbmax.shared.domain.repository.NotificationRepository
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/settings/notifications/NotificationSettingsViewModel.kt`.
 *
 * Same plain-class + own-`CoroutineScope` pattern as every other Phase 5
 * ViewModel. Behavior (load preferences, optimistic-update-with-rollback
 * toggles) is unchanged from the Android original.
 *
 * Deliberately NOT ported: the Android original's separate deprecated
 * parameterized `NotificationSettingsSection` overload (a Phase 3-era
 * migration shim already marked for Phase 5 removal in its own doc comment)
 * — this port only carries forward the current, non-deprecated,
 * self-managed-ViewModel API.
 */
class NotificationSettingsViewModel(
    private val notificationRepository: NotificationRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadNotificationPreferences()
    }

    fun close() {
        scope.cancel()
    }

    fun loadNotificationPreferences() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }

            observeCurrentUser().first()?.let { user ->
                notificationRepository.getPreferences(user.id)
                    .onSuccess { preferences ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                notificationPreferences = preferences,
                                error = null
                            )
                        }
                    }
                    .onFailure { error ->
                        val errorMessage = if (error.message?.contains("PERMISSION_DENIED") == true) {
                            null
                        } else {
                            error.message
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                notificationPreferences = NotificationPreferences(userId = user.id),
                                error = errorMessage
                            )
                        }
                    }
            } ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notificationPreferences = null,
                        error = null
                    )
                }
            }
        }
    }

    fun togglePushNotifications(enabled: Boolean) {
        updatePreferences { it.copy(enablePushNotifications = enabled) }
    }

    fun toggleGradingComplete(enabled: Boolean) {
        updatePreferences { it.copy(enableGradingNotifications = enabled) }
    }

    fun toggleFeedbackAvailable(enabled: Boolean) {
        updatePreferences { it.copy(enableFeedbackNotifications = enabled) }
    }

    fun toggleBatchInvitation(enabled: Boolean) {
        updatePreferences { it.copy(enableBatchInvitations = enabled) }
    }

    fun toggleGeneralAnnouncement(enabled: Boolean) {
        updatePreferences { it.copy(enableGeneralAnnouncements = enabled) }
    }

    fun toggleStudyReminders(enabled: Boolean) {
        updatePreferences { it.copy(enableStudyReminders = enabled) }
    }

    fun toggleTestReminders(enabled: Boolean) {
        updatePreferences { it.copy(enableTestReminders = enabled) }
    }

    fun toggleMarketplaceUpdates(enabled: Boolean) {
        updatePreferences { it.copy(enableMarketplaceUpdates = enabled) }
    }

    fun updateQuietHours(startHour: Int, endHour: Int) {
        updatePreferences { it.copy(quietHoursStart = startHour, quietHoursEnd = endHour) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun updatePreferences(update: (NotificationPreferences) -> NotificationPreferences) {
        scope.launch {
            val currentPreferences = _uiState.value.notificationPreferences ?: return@launch
            val updatedPreferences = update(currentPreferences)

            _uiState.update { it.copy(notificationPreferences = updatedPreferences) }

            notificationRepository.savePreferences(updatedPreferences)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                    _uiState.update { it.copy(notificationPreferences = currentPreferences) }
                }
        }
    }
}
