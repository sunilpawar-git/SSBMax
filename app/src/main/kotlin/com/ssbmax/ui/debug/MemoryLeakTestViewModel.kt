package com.ssbmax.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.data.util.LeakVerificationHelper
import com.ssbmax.core.data.util.MemoryLeakTracker
import com.ssbmax.core.data.util.trackMemoryLeaks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Memory Leak Test Screen
 *
 * Provides controlled test scenarios for verifying memory leak prevention
 * with Android Profiler.
 */
@HiltViewModel
class MemoryLeakTestViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryLeakTestUiState())
    val uiState: StateFlow<MemoryLeakTestUiState> = _uiState.asStateFlow()

    init {
        trackMemoryLeaks("MemoryLeakTestViewModel")
        android.util.Log.d("MemoryLeakTestViewModel", "🚀 Debug ViewModel initialized")

        // Start periodic leak checking
        startLeakMonitoring()
    }

    private fun startLeakMonitoring() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5000) // Check every 5 seconds
                updateMemoryStats()
            }
        }
    }

    fun runLeakTest(scenarioName: String) {
        android.util.Log.i("MemoryLeakTestViewModel", "🧪 Running leak test: $scenarioName")
        addLog("🧪 Starting test scenario: $scenarioName")

        LeakVerificationHelper.createLeakTestScenario(this, scenarioName)
        updateMemoryStats()
    }

    fun forceMemoryPressure() {
        android.util.Log.i("MemoryLeakTestViewModel", "🧪 Forcing memory pressure")
        addLog("🧪 Forcing memory pressure and GC")

        LeakVerificationHelper.forceMemoryPressure()
        updateMemoryStats()
    }

    fun checkForLeaks() {
        android.util.Log.i("MemoryLeakTestViewModel", "🧪 Checking for leaks")
        addLog("🧪 Checking for memory leaks")

        MemoryLeakTracker.checkForLeaks()
        updateMemoryStats()
    }

    fun generateReport() {
        val report = LeakVerificationHelper.generateLeakReport()
        android.util.Log.i("MemoryLeakTestViewModel", "📊 Leak Report:\n$report")

        // Add report to logs
        report.lines().forEach { line ->
            addLog(line)
        }
    }

    private fun updateMemoryStats() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024

        _uiState.update { it.copy(
            activeViewModels = MemoryLeakTracker.getActiveViewModelCount(),
            activeJobs = MemoryLeakTracker.getActiveJobCount(),
            memoryUsage = "${usedMemory}MB / ${maxMemory}MB",
            hasLeaks = false // Leak detection would need more sophisticated logic
        )}
    }

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())

        _uiState.update { it.copy(
            logs = it.logs + "[$timestamp] $message"
        )}
    }

    override fun onCleared() {
        super.onCleared()

        android.util.Log.d("MemoryLeakTestViewModel", "🧹 Debug ViewModel onCleared")
        MemoryLeakTracker.unregisterViewModel("MemoryLeakTestViewModel")
        MemoryLeakTracker.logMemoryDump("MemoryLeakTestViewModel-Cleared")

        addLog("✅ Debug ViewModel cleanup complete")
    }
}

data class MemoryLeakTestUiState(
    val activeViewModels: Int = 0,
    val activeJobs: Int = 0,
    val memoryUsage: String = "0MB / 0MB",
    val hasLeaks: Boolean = false,
    val logs: List<String> = emptyList()
)
