package com.ssbmax.shared.presentation.analytics

import com.ssbmax.shared.domain.model.PerformanceOverview
import com.ssbmax.shared.domain.model.TestPerformancePoint
import com.ssbmax.shared.domain.model.TestTypeStats
import com.ssbmax.shared.domain.repository.AnalyticsRepository
import com.ssbmax.shared.domain.util.DomainLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP port of the Android `app/.../ui/analytics/AnalyticsViewModel.kt`.
 *
 * Follows the plain-class + own-`CoroutineScope` ViewModel pattern this phase
 * already established -- NOT `androidx.lifecycle.ViewModel`; the screen uses
 * `koinInject()`, not `koinViewModel()`.
 *
 * [AnalyticsRepository] was already ported and bound in `sharedModule` (Phase 2's
 * 6th slice, `GitLiveAnalyticsRepository`) -- this session found no
 * `AnalyticsManager`-shaped blocker at all: the Android screen depends only on
 * this already-KMP-safe repository interface, not on the Android-only
 * `AnalyticsManager` flagged as out-of-scope during the earlier home-vertical
 * session (that manager is a different, unrelated class). `ErrorLogger.log`
 * (Android-only, Crashlytics-backed) replaced with [DomainLogger], same seam
 * every other ported ViewModel in this phase uses. Everything else (silent
 * failure for non-critical stat loads) is an unchanged port.
 */
class AnalyticsViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val logger: DomainLogger
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private companion object {
        const val TAG = "AnalyticsViewModel"
    }

    init {
        loadAnalytics()
    }

    fun close() {
        scope.cancel()
    }

    fun loadAnalytics() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                analyticsRepository.getPerformanceOverview().collect { overview ->
                    _uiState.update {
                        it.copy(
                            overview = overview,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load analytics: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadTestTypeDetails(testType: String) {
        scope.launch {
            try {
                analyticsRepository.getTestTypeStats(testType).collect { stats ->
                    _uiState.update { it.copy(selectedTestStats = stats) }
                }
            } catch (e: Exception) {
                logger.e(TAG, "Failed to load test stats for $testType", e)
                // Silent failure - analytics is non-critical feature
            }
        }
    }

    fun loadAllTestStats() {
        scope.launch {
            try {
                analyticsRepository.getAllTestTypeStats().collect { allStats ->
                    _uiState.update { it.copy(allTestStats = allStats) }
                }
            } catch (e: Exception) {
                logger.e(TAG, "Failed to load all test stats", e)
                // Silent failure - analytics is non-critical feature
            }
        }
    }

    fun loadRecentProgress(limit: Int = 10) {
        scope.launch {
            try {
                analyticsRepository.getRecentProgress(limit).collect { progress ->
                    _uiState.update { it.copy(recentProgress = progress) }
                }
            } catch (e: Exception) {
                logger.e(TAG, "Failed to load recent progress (limit=$limit)", e)
                // Silent failure - analytics is non-critical feature
            }
        }
    }

    fun selectTestType(testType: String?) {
        _uiState.update { it.copy(selectedTestType = testType) }
        if (testType != null) {
            loadTestTypeDetails(testType)
        }
    }
}

/**
 * UI State for Analytics Dashboard
 */
data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val overview: PerformanceOverview? = null,
    val selectedTestType: String? = null,
    val selectedTestStats: TestTypeStats? = null,
    val allTestStats: List<TestTypeStats> = emptyList(),
    val recentProgress: List<TestPerformancePoint> = emptyList()
)
