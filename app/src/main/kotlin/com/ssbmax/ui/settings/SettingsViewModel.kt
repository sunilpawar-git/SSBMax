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
    private val migratePPDTUseCase: MigratePPDTUseCase,
    private val migratePsychologyUseCase: MigratePsychologyUseCase,
    private val migratePIQFormUseCase: MigratePIQFormUseCase,
    private val migrateGTOUseCase: MigrateGTOUseCase,
    private val migrateInterviewUseCase: MigrateInterviewUseCase,
    private val migrateSSBOverviewUseCase: MigrateSSBOverviewUseCase,
    private val migrateMedicalsUseCase: MigrateMedicalsUseCase,
    private val migrateConferenceUseCase: MigrateConferenceUseCase,
    private val clearCacheUseCase: ClearFirestoreCacheUseCase,
    private val forceRefreshContentUseCase: ForceRefreshContentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Lifecycle-aware theme Flow - automatically starts/stops with collectors
    private val themeFlow = themePreferenceManager.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    init {
        loadNotificationPreferences()
        observeThemeChanges()
    }

    private fun observeThemeChanges() {
        viewModelScope.launch {
            themeFlow.collect { theme ->
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
    
    /**
     * Migrate Psychology topic and materials to Firestore
     */
    fun migratePsychology() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMigrating = true, psychologyMigrationResult = null) }
            
            try {
                val result = migratePsychologyUseCase.execute()
                result.onSuccess { migrationResult ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            psychologyMigrationResult = migrationResult
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            error = "Psychology migration failed: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isMigrating = false,
                        error = "Psychology migration failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearPsychologyMigrationResult() {
        _uiState.update { it.copy(psychologyMigrationResult = null) }
    }
    
    /**
     * Migrate PIQ Form topic and materials to Firestore
     */
    fun migratePIQForm() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMigrating = true, piqFormMigrationResult = null) }
            
            try {
                val result = migratePIQFormUseCase.execute()
                result.onSuccess { migrationResult ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            piqFormMigrationResult = migrationResult
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            error = "PIQ Form migration failed: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isMigrating = false,
                        error = "PIQ Form migration failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearPIQFormMigrationResult() {
        _uiState.update { it.copy(piqFormMigrationResult = null) }
    }
    
    /**
     * Migrate GTO topic and materials to Firestore
     */
    fun migrateGTO() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMigrating = true, gtoMigrationResult = null) }
            
            try {
                val result = migrateGTOUseCase.execute()
                result.onSuccess { migrationResult ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            gtoMigrationResult = migrationResult
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            error = "GTO migration failed: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isMigrating = false,
                        error = "GTO migration failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearGTOMigrationResult() {
        _uiState.update { it.copy(gtoMigrationResult = null) }
    }
    
    /**
     * Migrate Interview topic and materials to Firestore
     */
    fun migrateInterview() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMigrating = true, interviewMigrationResult = null) }
            
            try {
                val result = migrateInterviewUseCase.execute()
                result.onSuccess { migrationResult ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            interviewMigrationResult = migrationResult
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            error = "Interview migration failed: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isMigrating = false,
                        error = "Interview migration failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearInterviewMigrationResult() {
        _uiState.update { it.copy(interviewMigrationResult = null) }
    }
    
    /**
     * Migrate SSB Overview topic and materials to Firestore
     */
    fun migrateSSBOverview() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMigrating = true, ssbOverviewMigrationResult = null) }
            
            try {
                val result = migrateSSBOverviewUseCase.execute()
                result.onSuccess { migrationResult ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            ssbOverviewMigrationResult = migrationResult
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            error = "SSB Overview migration failed: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isMigrating = false,
                        error = "SSB Overview migration failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearSSBOverviewMigrationResult() {
        _uiState.update { it.copy(ssbOverviewMigrationResult = null) }
    }
    
    /**
     * Migrate Medicals topic and materials to Firestore
     */
    fun migrateMedicals() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMigrating = true, medicalsMigrationResult = null) }
            
            try {
                val result = migrateMedicalsUseCase.execute()
                result.onSuccess { migrationResult ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            medicalsMigrationResult = migrationResult
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            error = "Medicals migration failed: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isMigrating = false,
                        error = "Medicals migration failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearMedicalsMigrationResult() {
        _uiState.update { it.copy(medicalsMigrationResult = null) }
    }
    
    /**
     * Migrate Conference topic and materials to Firestore
     * THE FINAL MIGRATION - Completing 100%! 🎉
     */
    fun migrateConference() {
        viewModelScope.launch {
            _uiState.update { it.copy(isMigrating = true, conferenceMigrationResult = null) }
            
            try {
                val result = migrateConferenceUseCase.execute()
                result.onSuccess { migrationResult ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            conferenceMigrationResult = migrationResult
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isMigrating = false,
                            error = "Conference migration failed: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isMigrating = false,
                        error = "Conference migration failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearConferenceMigrationResult() {
        _uiState.update { it.copy(conferenceMigrationResult = null) }
    }
    
    /**
     * Force refresh content from server (bypasses cache)
     * This is better than clearPersistence because it works while app is running
     * Useful for developers when editing content in Firebase Console
     */
    fun clearFirestoreCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingCache = true, cacheCleared = false, refreshResult = null) }
            
            try {
                val result = forceRefreshContentUseCase.execute()
                result.onSuccess { refreshResult ->
                    _uiState.update { 
                        it.copy(
                            isClearingCache = false,
                            cacheCleared = true,
                            refreshResult = refreshResult,
                            error = null
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isClearingCache = false,
                            cacheCleared = false,
                            error = "Failed to refresh content: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isClearingCache = false,
                        cacheCleared = false,
                        error = "Failed to refresh content: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun clearCacheResult() {
        _uiState.update { it.copy(cacheCleared = false) }
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
    val ppdtMigrationResult: MigratePPDTUseCase.MigrationResult? = null,
    val psychologyMigrationResult: MigratePsychologyUseCase.MigrationResult? = null,
    val piqFormMigrationResult: MigratePIQFormUseCase.MigrationResult? = null,
    val gtoMigrationResult: MigrateGTOUseCase.MigrationResult? = null,
    val interviewMigrationResult: MigrateInterviewUseCase.MigrationResult? = null,
    val ssbOverviewMigrationResult: MigrateSSBOverviewUseCase.MigrationResult? = null,
    val medicalsMigrationResult: MigrateMedicalsUseCase.MigrationResult? = null,
    val conferenceMigrationResult: MigrateConferenceUseCase.MigrationResult? = null,
    val isClearingCache: Boolean = false,
    val cacheCleared: Boolean = false,
    val refreshResult: ForceRefreshContentUseCase.RefreshResult? = null
)

