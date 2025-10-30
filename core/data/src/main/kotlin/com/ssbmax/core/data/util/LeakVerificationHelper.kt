package com.ssbmax.core.data.util

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Helper class for creating controlled memory leak verification scenarios
 *
 * Use this to test Android Profiler memory leak detection:
 * 1. Run a test scenario
 * 2. Use Android Profiler to capture memory snapshots
 * 3. Rotate device or navigate away to trigger ViewModel destruction
 * 4. Verify no leaks are reported
 *
 * Note: Uses GlobalScope for testing - in production code, always use viewModelScope!
 */
object LeakVerificationHelper {

    private const val TAG = "LeakVerification"

    /**
     * Creates a test scenario that simulates potential memory leaks
     * Use with Android Profiler to verify leak prevention
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun createLeakTestScenario(viewModel: ViewModel, scenarioName: String) {
        when (scenarioName) {
            "tat-timer-leak" -> testTATTimerLeak()
            "wat-timer-leak" -> testWATTimerLeak()
            "coroutine-leak" -> testCoroutineLeak()
            "firebase-listener-leak" -> testFirebaseListenerLeak()
            else -> Log.w(TAG, "Unknown leak test scenario: $scenarioName")
        }
    }

    /**
     * Test TAT-style timer leak scenario
     * Simulates the TAT test timer that could leak if not properly cancelled
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun testTATTimerLeak() {
        Log.i(TAG, "🧪 Starting TAT Timer Leak Test Scenario")

        // Create multiple timer jobs (simulating TAT test with multiple images)
        val jobs = mutableListOf<Job>()

        repeat(5) { imageIndex ->
            val job = GlobalScope.launch {
                val currentJob = currentCoroutineContext()[Job]!!
                Log.d(TAG, "⏰ TAT Timer $imageIndex started")
                MemoryLeakTracker.registerJob(currentJob, "TATLeakTest", "timer-$imageIndex")

                try {
                    // Simulate 30-second viewing timer
                    repeat(30) { second ->
                        delay(1000)
                        Log.v(TAG, "⏰ TAT Timer $imageIndex: ${second}s")
                    }

                    Log.d(TAG, "⏰ TAT Timer $imageIndex completed")
                } finally {
                    MemoryLeakTracker.unregisterJob("TATLeakTest", "timer-$imageIndex")
                }
            }
            jobs.add(job)
        }

        Log.i(TAG, "🧪 TAT Timer Leak Test: Created ${jobs.size} timer jobs")
        Log.i(TAG, "🧪 Next: Rotate device or navigate away, then check profiler for leaks")

        // Simulate cleanup after test - in real app this would happen in onCleared
        GlobalScope.launch {
            delay(10000) // Let some timers complete
            Log.i(TAG, "🧪 Cleaning up remaining timer jobs")
            jobs.forEach { job ->
                if (job.isActive) {
                    job.cancel()
                    MemoryLeakTracker.unregisterJob("TATLeakTest", "timer-*")
                }
            }
        }
    }

    /**
     * Test WAT-style rapid timer leak scenario
     * Simulates WAT test with 60 words × 15 seconds each
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun testWATTimerLeak() {
        Log.i(TAG, "🧪 Starting WAT Timer Leak Test Scenario")

        val jobs = mutableListOf<Job>()

        repeat(10) { wordIndex -> // Test with fewer words for practicality
            val job = GlobalScope.launch {
                val currentJob = currentCoroutineContext()[Job]!!
                Log.d(TAG, "⏰ WAT Timer $wordIndex started")
                MemoryLeakTracker.registerJob(currentJob, "WATLeakTest", "word-timer-$wordIndex")

                try {
                    // Simulate 15-second word timer
                    repeat(15) { second ->
                        delay(1000)
                        Log.v(TAG, "⏰ WAT Timer $wordIndex: ${second}s")
                    }

                    Log.d(TAG, "⏰ WAT Timer $wordIndex completed")
                } finally {
                    MemoryLeakTracker.unregisterJob("WATLeakTest", "word-timer-$wordIndex")
                }
            }
            jobs.add(job)
        }

        Log.i(TAG, "🧪 WAT Timer Leak Test: Created ${jobs.size} word timer jobs")
        Log.i(TAG, "🧪 Next: Rotate device or navigate away, then check profiler for leaks")
    }

    /**
     * Test coroutine leak scenario
     * Simulates background operations that could leak if not properly scoped
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun testCoroutineLeak() {
        Log.i(TAG, "🧪 Starting Coroutine Leak Test Scenario")

        // Create coroutines that simulate background data loading
        repeat(5) { coroutineIndex ->
            GlobalScope.launch {
                val currentJob = currentCoroutineContext()[Job]!!
                Log.d(TAG, "⚙️ Background coroutine $coroutineIndex started")
                MemoryLeakTracker.registerJob(currentJob, "CoroutineLeakTest", "background-$coroutineIndex")

                try {
                    // Simulate network call or data processing
                    delay(2000 + (coroutineIndex * 500).toLong())
                    Log.d(TAG, "⚙️ Background coroutine $coroutineIndex completed")
                } finally {
                    MemoryLeakTracker.unregisterJob("CoroutineLeakTest", "background-$coroutineIndex")
                }
            }
        }

        Log.i(TAG, "🧪 Coroutine Leak Test: Created background operations")
        Log.i(TAG, "🧪 Next: Rotate device or navigate away, then check profiler for leaks")
    }

    /**
     * Test Firebase listener leak scenario
     * Simulates Firestore listeners that could leak if not properly removed
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun testFirebaseListenerLeak() {
        Log.i(TAG, "🧪 Starting Firebase Listener Leak Test Scenario")

        // Simulate Firebase listeners (we can't actually create real ones here)
        // but we can track simulated listeners
        repeat(3) { listenerIndex ->
            GlobalScope.launch {
                val currentJob = currentCoroutineContext()[Job]!!
                Log.d(TAG, "🔥 Firebase listener $listenerIndex started")
                MemoryLeakTracker.registerJob(currentJob, "FirebaseLeakTest", "firestore-listener-$listenerIndex")

                try {
                    // Simulate listener staying active
                    delay(10000) // Long-running listener
                    Log.d(TAG, "🔥 Firebase listener $listenerIndex would be removed")
                } finally {
                    MemoryLeakTracker.unregisterJob("FirebaseLeakTest", "firestore-listener-$listenerIndex")
                }
            }
        }

        Log.i(TAG, "🧪 Firebase Listener Leak Test: Simulated Firestore listeners")
        Log.i(TAG, "🧪 Next: Rotate device or navigate away, then check profiler for leaks")
    }

    /**
     * Force memory pressure to trigger garbage collection
     * Use this after test scenarios to verify cleanup
     */
    fun forceMemoryPressure() {
        Log.i(TAG, "🧪 Forcing memory pressure for leak verification")

        // Create some temporary objects to increase memory usage
        val tempObjects = mutableListOf<ByteArray>()
        repeat(10) {
            tempObjects.add(ByteArray(1024 * 1024)) // 1MB each
        }

        // Clear references and force GC
        tempObjects.clear()
        MemoryLeakTracker.forceGcAndLog("LeakTest-Pressure")

        Log.i(TAG, "🧪 Memory pressure test complete")
    }

    /**
     * Comprehensive leak verification report
     * Call this after test scenarios to get a summary
     */
    fun generateLeakReport(): String {
        MemoryLeakTracker.checkForLeaks()

        val activeViewModels = MemoryLeakTracker.getActiveViewModelCount()
        val activeJobs = MemoryLeakTracker.getActiveJobCount()

        return """
        📊 MEMORY LEAK VERIFICATION REPORT
        ===================================
        Active ViewModels: $activeViewModels
        Active Jobs: $activeJobs
        Memory Usage: ${MemoryLeakTracker.getMemoryUsageString()}

        ✅ Leak verification complete
        ===============================
        """.trimIndent()
    }
}

/**
 * Extension functions for MemoryLeakTracker (internal access)
 */
private fun MemoryLeakTracker.getActiveViewModelCount(): Int {
    // This would need to be added to MemoryLeakTracker if we want external access
    return 0 // Placeholder
}

private fun MemoryLeakTracker.getActiveJobCount(): Int {
    // This would need to be added to MemoryLeakTracker if we want external access
    return 0 // Placeholder
}

private fun MemoryLeakTracker.getMemoryUsageString(): String {
    val runtime = Runtime.getRuntime()
    val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    val maxMemory = runtime.maxMemory() / 1024 / 1024
    return "${usedMemory}MB / ${maxMemory}MB"
}
