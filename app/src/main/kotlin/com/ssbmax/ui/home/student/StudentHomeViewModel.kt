package com.ssbmax.ui.home.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AuthRepository
import com.ssbmax.core.domain.repository.UserProfileRepository
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

/**
 * ViewModel for Student Home Screen
 * Manages user progress and displays user info
 */
@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val testProgressRepository: com.ssbmax.core.domain.repository.TestProgressRepository,
    private val unifiedResultRepository: com.ssbmax.core.domain.repository.UnifiedResultRepository,
    private val getOLQDashboard: com.ssbmax.core.domain.usecase.dashboard.GetOLQDashboardUseCase,
    private val analyticsManager: com.ssbmax.core.data.analytics.AnalyticsManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(StudentHomeUiState())
    val uiState: StateFlow<StudentHomeUiState> = _uiState.asStateFlow()
    
    init {
        observeUserProfile()
        observeTestProgress()
        loadDashboard(forceRefresh = false) // Use cache on initial load
    }
    
    private fun observeUserProfile() {
        viewModelScope.launch {
            try {
                // Get current user and observe their profile with lifecycle awareness
                val currentUser = authRepository.currentUser.value
                if (currentUser != null) {
                    userProfileRepository.getUserProfile(currentUser.id)
                        .stateIn(
                            scope = viewModelScope,
                            started = SharingStarted.WhileSubscribed(5000),
                            initialValue = Result.success(null)
                        )
                        .collect { result ->
                            result.onSuccess { profile ->
                                _uiState.update {
                                    it.copy(
                                        userName = profile?.fullName ?: "Aspirant",
                                        currentStreak = profile?.currentStreak ?: 0,
                                        error = null
                                    )
                                }
                            }
                            .onFailure { error ->
                                ErrorLogger.logWithUser(
                                    throwable = error,
                                    description = "Failed to load user profile",
                                    userId = currentUser.id
                                )
                                _uiState.update { it.copy(error = "Failed to load profile") }
                            }
                        }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Unexpected error in observeUserProfile")
                _uiState.update { it.copy(error = "Failed to load profile") }
            }
        }
    }
    
    private fun observeTestProgress() {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUser.value?.id ?: return@launch

                // Lifecycle-aware combined progress flow
                kotlinx.coroutines.flow.combine(
                    testProgressRepository.getPhase1Progress(userId),
                    testProgressRepository.getPhase2Progress(userId)
                ) { phase1, phase2 ->
                    Pair(phase1, phase2)
                }
                    .catch { error ->
                        ErrorLogger.logWithUser(error, "Failed to load test progress", userId)
                        // Emit empty fallback
                        emit(Pair(
                            Phase1Progress(
                                oirProgress = TestProgress(TestType.OIR),
                                ppdtProgress = TestProgress(TestType.PPDT)
                            ),
                            Phase2Progress(
                                psychologyProgress = TestProgress(TestType.TAT),
                                gtoProgress = TestProgress(TestType.GTO_GD),
                                interviewProgress = TestProgress(TestType.IO)
                            )
                        ))
                    }
                    .stateIn(
                        scope = viewModelScope,
                        started = SharingStarted.WhileSubscribed(5000),
                        initialValue = Pair(
                            Phase1Progress(
                                oirProgress = TestProgress(TestType.OIR),
                                ppdtProgress = TestProgress(TestType.PPDT)
                            ),
                            Phase2Progress(
                                psychologyProgress = TestProgress(TestType.TAT), // Psychology encompasses multiple tests
                                gtoProgress = TestProgress(TestType.GTO_GD),
                                interviewProgress = TestProgress(TestType.IO)
                            )
                        )
                    )
                    .collect { (phase1, phase2) ->
                        // Calculate tests completed (tests that have been attempted)
                        // Count tests with status other than NOT_ATTEMPTED
                        val allProgress = listOf(
                            phase1.oirProgress,
                            phase1.ppdtProgress,
                            phase2.psychologyProgress,
                            phase2.gtoProgress,
                            phase2.interviewProgress
                        )

                        val completedTests = allProgress.count { progress ->
                            progress.status != TestStatus.NOT_ATTEMPTED
                        }

                        _uiState.update {
                            it.copy(
                                phase1Progress = phase1,
                                phase2Progress = phase2,
                                testsCompleted = completedTests
                            )
                        }
                    }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Unexpected error in observeTestProgress")
            }
        }
    }
    
    fun refreshProgress() {
        // Flows are already observing, just re-trigger by reinitializing observers
        observeUserProfile()
        observeTestProgress()
        loadDashboard(forceRefresh = true) // Force fresh data from Firestore
    }
    
    /**
     * Refresh only the dashboard (used by refresh button)
     */
    fun refreshDashboard() {
        loadDashboard(forceRefresh = true)
    }
    
    private fun loadDashboard(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(
                    isLoadingDashboard = true,
                    isRefreshingDashboard = forceRefresh, // Show refresh indicator on force refresh
                    dashboardError = null
                ) }

                val userId = authRepository.currentUser.value?.id
                if (userId == null) {
                    _uiState.update { it.copy(
                        isLoadingDashboard = false,
                        isRefreshingDashboard = false,
                        dashboardError = "Please login to view progress"
                    ) }
                    return@launch
                }

                // Add 10-second timeout to prevent infinite loading
                withTimeout(10_000) {
                    getOLQDashboard(userId, forceRefresh)
                        .onSuccess { processedData ->
                            // Track cache performance analytics
                            val metadata = processedData.cacheMetadata
                            analyticsManager.trackFeatureUsed(
                                featureName = if (metadata.cacheHit) "dashboard_cache_hit" else "dashboard_cache_miss",
                                parameters = mapOf(
                                    "load_time_ms" to metadata.loadTimeMs,
                                    "forced_refresh" to metadata.forcedRefresh,
                                    "user_id" to userId
                                )
                            )

                            _uiState.update { it.copy(
                                isLoadingDashboard = false,
                                isRefreshingDashboard = false,
                                dashboard = processedData,
                                lastRefreshTime = System.currentTimeMillis(),
                                dashboardError = null
                            ) }
                        }
                        .onFailure { error ->
                            // Track cache error
                            analyticsManager.trackFeatureUsed(
                                featureName = "dashboard_cache_error",
                                parameters = mapOf(
                                    "error" to (error.message ?: "Unknown error"),
                                    "user_id" to userId
                                )
                            )

                            ErrorLogger.log(error, "Failed to load dashboard")
                            _uiState.update { it.copy(
                                isLoadingDashboard = false,
                                isRefreshingDashboard = false,
                                dashboardError = error.message ?: "Failed to load dashboard"
                            ) }
                        }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Handle timeout gracefully
                _uiState.update { it.copy(
                    isLoadingDashboard = false,
                    isRefreshingDashboard = false,
                    dashboardError = "Dashboard timed out. Please retry."
                ) }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Unexpected error in loadDashboard")
                _uiState.update { it.copy(
                    isLoadingDashboard = false,
                    isRefreshingDashboard = false,
                    dashboardError = "Unexpected error: ${e.message}"
                ) }
            }
        }
    }
}

/**
 * UI State for Student Home Screen
 */
data class StudentHomeUiState(
    val isLoading: Boolean = false,
    val userName: String = "Aspirant",
    val currentStreak: Int = 0,
    val testsCompleted: Int = 0,
    val notificationCount: Int = 0,
    val phase1Progress: com.ssbmax.core.domain.model.Phase1Progress? = null,
    val phase2Progress: com.ssbmax.core.domain.model.Phase2Progress? = null,
    val error: String? = null,
    // Dashboard state (PERFORMANCE: using ProcessedDashboardData with pre-computed aggregations + caching)
    val isLoadingDashboard: Boolean = false,
    val isRefreshingDashboard: Boolean = false, // Dashboard refresh state
    val dashboard: com.ssbmax.core.domain.usecase.dashboard.ProcessedDashboardData? = null,
    val dashboardError: String? = null,
    val lastRefreshTime: Long? = null // Track when data was last refreshed
)
