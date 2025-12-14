package com.ssbmax.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AnalyticsRepository
import com.ssbmax.utils.ErrorLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Analytics Dashboard
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    
    init {
        loadAnalytics()
    }
    
    fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Collect performance overview
                analyticsRepository.getPerformanceOverview().collect { overview ->
                    _uiState.update { it.copy(
                        overview = overview,
                        isLoading = false
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to load analytics: ${e.message}"
                ) }
            }
        }
    }
    
    fun loadTestTypeDetails(testType: String) {
        viewModelScope.launch {
            try {
                analyticsRepository.getTestTypeStats(testType).collect { stats ->
                    _uiState.update { it.copy(
                        selectedTestStats = stats
                    ) }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to load test stats", mapOf("testType" to testType))
                // Silent failure - analytics is non-critical feature
            }
        }
    }
    
    fun loadAllTestStats() {
        viewModelScope.launch {
            try {
                analyticsRepository.getAllTestTypeStats().collect { allStats ->
                    _uiState.update { it.copy(
                        allTestStats = allStats
                    ) }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to load all test stats")
                // Silent failure - analytics is non-critical feature
            }
        }
    }
    
    fun loadRecentProgress(limit: Int = 10) {
        viewModelScope.launch {
            try {
                analyticsRepository.getRecentProgress(limit).collect { progress ->
                    _uiState.update { it.copy(
                        recentProgress = progress
                    ) }
                }
            } catch (e: Exception) {
                ErrorLogger.log(e, "Failed to load recent progress", mapOf("limit" to limit.toString()))
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
