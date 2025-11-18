package com.ssbmax.ui.settings.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.NotificationPreferences
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Notification Settings
 * Handles notification preference management independently from main SettingsViewModel
 */
@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadNotificationPreferences()
    }

    /**
     * Load notification preferences for the current user
     */
    fun loadNotificationPreferences() {
        viewModelScope.launch {
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
                        // Gracefully handle permission errors
                        val errorMessage = if (error.message?.contains("PERMISSION_DENIED") == true) {
                            null // Don't show error for permission issues - use default preferences
                        } else {
                            error.message
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                notificationPreferences = NotificationPreferences(userId = user.id), // Use defaults
                                error = errorMessage
                            )
                        }
                    }
            } ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        notificationPreferences = null, // No user, no preferences
                        error = null
                    )
                }
            }
        }
    }

    /**
     * Toggle push notifications master switch
     */
    fun togglePushNotifications(enabled: Boolean) {
        updatePreferences { it.copy(enablePushNotifications = enabled) }
    }

    /**
     * Toggle grading completion notifications
     */
    fun toggleGradingComplete(enabled: Boolean) {
        updatePreferences { it.copy(enableGradingNotifications = enabled) }
    }

    /**
     * Toggle feedback available notifications
     */
    fun toggleFeedbackAvailable(enabled: Boolean) {
        updatePreferences { it.copy(enableFeedbackNotifications = enabled) }
    }

    /**
     * Toggle batch invitation notifications
     */
    fun toggleBatchInvitation(enabled: Boolean) {
        updatePreferences { it.copy(enableBatchInvitations = enabled) }
    }

    /**
     * Toggle general announcement notifications
     */
    fun toggleGeneralAnnouncement(enabled: Boolean) {
        updatePreferences { it.copy(enableGeneralAnnouncements = enabled) }
    }

    /**
     * Toggle study reminder notifications
     */
    fun toggleStudyReminders(enabled: Boolean) {
        updatePreferences { it.copy(enableStudyReminders = enabled) }
    }

    /**
     * Toggle test reminder notifications
     */
    fun toggleTestReminders(enabled: Boolean) {
        updatePreferences { it.copy(enableTestReminders = enabled) }
    }

    /**
     * Toggle marketplace update notifications
     */
    fun toggleMarketplaceUpdates(enabled: Boolean) {
        updatePreferences { it.copy(enableMarketplaceUpdates = enabled) }
    }

    /**
     * Update quiet hours settings
     */
    fun updateQuietHours(startHour: Int, endHour: Int) {
        updatePreferences { it.copy(quietHoursStart = startHour, quietHoursEnd = endHour) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Update notification preferences with optimistic updates and rollback on failure
     */
    private fun updatePreferences(update: (NotificationPreferences) -> NotificationPreferences) {
        viewModelScope.launch {
            val currentPreferences = _uiState.value.notificationPreferences ?: return@launch
            val updatedPreferences = update(currentPreferences)

            // Optimistic update
            _uiState.update { it.copy(notificationPreferences = updatedPreferences) }

            // Save to repository
            notificationRepository.savePreferences(updatedPreferences)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                    // Revert on failure
                    _uiState.update { it.copy(notificationPreferences = currentPreferences) }
                }
        }
    }
}
