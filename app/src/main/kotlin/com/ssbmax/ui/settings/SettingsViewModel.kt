package com.ssbmax.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.health.FirebaseHealthCheck
import com.ssbmax.core.data.preferences.ThemePreferenceManager
import com.ssbmax.core.domain.model.AppTheme
import com.ssbmax.core.domain.model.NotificationPreferences
import com.ssbmax.core.domain.repository.NotificationRepository
import com.ssbmax.core.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings Screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val themePreferenceManager: ThemePreferenceManager,
    private val firebaseHealthCheck: FirebaseHealthCheck,
    private val migrateOIRUseCase: MigrateOIRUseCase,
    private val migratePPDTUseCase: MigratePPDTUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadNotificationPreferences()
        loadThemePreference()
    }

    private fun loadThemePreference() {
        viewModelScope.launch {
            themePreferenceManager.themeFlow.collect { theme ->
                _uiState.update { it.copy(appTheme = theme) }
            }
        }
    }

    private fun loadNotificationPreferences() {
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

    private fun updatePreferences(update: (NotificationPreferences) -> NotificationPreferences) {
        viewModelScope.launch {
            val currentPreferences = _uiState.value.notificationPreferences ?: return@launch
            val updatedPreferences = update(currentPreferences)
            
            _uiState.update { it.copy(notificationPreferences = updatedPreferences) }
            
            notificationRepository.savePreferences(updatedPreferences)
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                    // Revert on failure
                    _uiState.update { it.copy(notificationPreferences = currentPreferences) }
                }
        }
    }

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            themePreferenceManager.setTheme(theme)
            _uiState.update { it.copy(appTheme = theme) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Run Firebase health check and update UI state
     */
    fun runHealthCheck() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingHealth = true, healthCheckResult = null) }
            
            try {
                val result = firebaseHealthCheck.checkHealth()
                _uiState.update { 
                    it.copy(
                        isCheckingHealth = false,
                        healthCheckResult = result
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isCheckingHealth = false,
                        error = "Health check failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearHealthCheckResult() {
        _uiState.update { it.copy(healthCheckResult = null) }
    }
    
    /**
     * Migrate OIR topic and materials to Firestore
     */
    fun migrateOIR() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMigrating = true, migrationResult = null) }
            
            try {
                val result = migrateOIRUseCase.execute()
                result.onSuccess { migrationResult ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            migrationResult = migrationResult
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            error = "Migration failed: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isMigrating = false,
                        error = "Migration failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Migrate PPDT topic and materials to Firestore
     */
    fun migratePPDT() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMigrating = true, ppdtMigrationResult = null) }
            
            try {
                val result = migratePPDTUseCase.execute()
                result.onSuccess { migrationResult ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            ppdtMigrationResult = migrationResult
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            error = "PPDT migration failed: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isMigrating = false,
                        error = "PPDT migration failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearMigrationResult() {
        _uiState.update { it.copy(migrationResult = null) }
    }
    
    fun clearPPDTMigrationResult() {
        _uiState.update { it.copy(ppdtMigrationResult = null) }
    }
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val notificationPreferences: NotificationPreferences? = null,
    val subscriptionTier: com.ssbmax.ui.settings.SubscriptionTier = com.ssbmax.ui.settings.SubscriptionTier.BASIC,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val error: String? = null,
    val isCheckingHealth: Boolean = false,
    val healthCheckResult: FirebaseHealthCheck.HealthStatus? = null,
    val isMigrating: Boolean = false,
    val migrationResult: MigrateOIRUseCase.MigrationResult? = null,
    val ppdtMigrationResult: MigratePPDTUseCase.MigrationResult? = null
)

