package com.ssbmax.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Collect performance overview
                analyticsRepository.getPerformanceOverview().collect { overview ->
                    _uiState.value = _uiState.value.copy(
                        overview = overview,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load analytics: ${e.message}"
                )
            }
        }
    }
    
    fun loadTestTypeDetails(testType: String) {
        viewModelScope.launch {
            try {
                analyticsRepository.getTestTypeStats(testType).collect { stats ->
                    _uiState.value = _uiState.value.copy(
                        selectedTestStats = stats
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AnalyticsViewModel", "Failed to load test stats", e)
            }
        }
    }
    
    fun loadAllTestStats() {
        viewModelScope.launch {
            try {
                analyticsRepository.getAllTestTypeStats().collect { allStats ->
                    _uiState.value = _uiState.value.copy(
                        allTestStats = allStats
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AnalyticsViewModel", "Failed to load all test stats", e)
            }
        }
    }
    
    fun loadRecentProgress(limit: Int = 10) {
        viewModelScope.launch {
            try {
                analyticsRepository.getRecentProgress(limit).collect { progress ->
                    _uiState.value = _uiState.value.copy(
                        recentProgress = progress
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AnalyticsViewModel", "Failed to load recent progress", e)
            }
        }
    }
    
    fun selectTestType(testType: String?) {
        _uiState.value = _uiState.value.copy(selectedTestType = testType)
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

