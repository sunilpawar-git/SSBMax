package com.ssbmax.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.health.FirebaseHealthCheck
import com.ssbmax.core.data.preferences.ThemePreferenceManager
import com.ssbmax.core.domain.model.AppTheme
import com.ssbmax.core.domain.usecase.migration.ClearFirestoreCacheUseCase
import com.ssbmax.core.domain.usecase.migration.ForceRefreshContentUseCase
import com.ssbmax.core.domain.usecase.migration.MigrateConferenceUseCase
import com.ssbmax.core.domain.usecase.migration.MigrateGTOUseCase
import com.ssbmax.core.domain.usecase.migration.MigrateInterviewUseCase
import com.ssbmax.core.domain.usecase.migration.MigrateMedicalsUseCase
import com.ssbmax.core.domain.usecase.migration.MigrateOIRUseCase
import com.ssbmax.core.domain.usecase.migration.MigratePIQFormUseCase
import com.ssbmax.core.domain.usecase.migration.MigratePPDTUseCase
import com.ssbmax.core.domain.usecase.migration.MigratePsychologyUseCase
import com.ssbmax.core.domain.usecase.migration.MigrateSSBOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings Screen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
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
        observeThemeChanges()
    }

    private fun observeThemeChanges() {
        viewModelScope.launch {
            themeFlow.collect { theme ->
                _uiState.update { it.copy(appTheme = theme) }
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
    val subscriptionTier: com.ssbmax.core.domain.model.SubscriptionTier = com.ssbmax.core.domain.model.SubscriptionTier.FREE,
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

