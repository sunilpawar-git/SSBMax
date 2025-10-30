package com.ssbmax.core.data.util

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * Memory leak tracking utility for Android Profiler verification
 *
 * Features:
 * - Tracks ViewModel lifecycle for leak detection
 * - Monitors active coroutines and timers
 * - Logs memory usage patterns for profiler analysis
 * - Provides leak detection callbacks for testing
 */
object MemoryLeakTracker {

    private const val TAG = "MemoryLeakTracker"

    // Track active ViewModels for leak detection
    private val activeViewModels = ConcurrentHashMap<String, ViewModelInfo>()

    // Track active jobs (coroutines, timers)
    private val activeJobs = ConcurrentHashMap<String, JobInfo>()

    // Leak detection events for testing
    private val _leakEvents = MutableSharedFlow<LeakEvent>()
    val leakEvents: SharedFlow<LeakEvent> = _leakEvents

    /**
     * Register a ViewModel for leak tracking
     */
    fun registerViewModel(viewModel: ViewModel, name: String) {
        val info = ViewModelInfo(
            name = name,
            viewModelRef = WeakReference(viewModel),
            createdAt = System.currentTimeMillis(),
            threadId = Thread.currentThread().id,
            threadName = Thread.currentThread().name
        )

        activeViewModels[name] = info

        Log.d(TAG, "📊 ViewModel REGISTERED: $name (Thread: ${info.threadName})")
        Log.d(TAG, "📊 Active ViewModels: ${activeViewModels.size}")
        Log.d(TAG, "📊 Memory usage: ${getMemoryUsageString()}")
    }

    /**
     * Unregister a ViewModel (called in onCleared)
     */
    fun unregisterViewModel(name: String) {
        val info = activeViewModels.remove(name)
        if (info != null) {
            val lifetime = System.currentTimeMillis() - info.createdAt
            Log.d(TAG, "🗑️ ViewModel UNREGISTERED: $name (Lifetime: ${lifetime}ms)")
            Log.d(TAG, "📊 Remaining ViewModels: ${activeViewModels.size}")
        } else {
            Log.w(TAG, "⚠️ ViewModel not found for unregistration: $name")
        }
    }

    /**
     * Register an active job (coroutine/timer)
     */
    fun registerJob(job: Job, owner: String, description: String) {
        val info = JobInfo(
            owner = owner,
            description = description,
            jobRef = WeakReference(job),
            createdAt = System.currentTimeMillis(),
            threadId = Thread.currentThread().id
        )

        val key = "$owner-$description-${System.currentTimeMillis()}"
        activeJobs[key] = info

        Log.d(TAG, "⚙️ Job REGISTERED: $owner -> $description")
        Log.d(TAG, "⚙️ Active Jobs: ${activeJobs.size}")
    }

    /**
     * Unregister a job when cancelled/completed
     */
    fun unregisterJob(owner: String, description: String) {
        val keyToRemove = activeJobs.keys.find { it.startsWith("$owner-$description") }
        if (keyToRemove != null) {
            activeJobs.remove(keyToRemove)
            Log.d(TAG, "🛑 Job UNREGISTERED: $owner -> $description")
            Log.d(TAG, "⚙️ Remaining Jobs: ${activeJobs.size}")
        }
    }

    /**
     * Check for potential memory leaks
     * Call this periodically or before key operations
     */
    fun checkForLeaks() {
        val leakedViewModels = activeViewModels.filter { (_, info) ->
            info.viewModelRef.get() == null
        }

        val leakedJobs = activeJobs.filter { (_, info) ->
            info.jobRef.get()?.isActive != true
        }

        if (leakedViewModels.isNotEmpty()) {
            Log.w(TAG, "🚨 POTENTIAL VIEWMODEL LEAKS: ${leakedViewModels.keys}")
            leakedViewModels.forEach { (name, info) ->
                Log.w(TAG, "  - $name (created ${System.currentTimeMillis() - info.createdAt}ms ago)")
            }
        }

        if (leakedJobs.isNotEmpty()) {
            Log.w(TAG, "🚨 POTENTIAL JOB LEAKS: ${leakedJobs.size} jobs")
            leakedJobs.forEach { (_, info) ->
                Log.w(TAG, "  - ${info.owner}: ${info.description}")
            }
        }

        if (leakedViewModels.isEmpty() && leakedJobs.isEmpty()) {
            Log.d(TAG, "✅ No memory leaks detected")
        }
    }

    /**
     * Get active ViewModel count for debugging
     */
    fun getActiveViewModelCount(): Int = activeViewModels.size

    /**
     * Get active job count for debugging
     */
    fun getActiveJobCount(): Int = activeJobs.size

    /**
     * Get memory usage string for logging
     */
    private fun getMemoryUsageString(): String {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        return "${usedMemory}MB / ${maxMemory}MB"
    }

    /**
     * Log memory dump for profiler analysis
     */
    fun logMemoryDump(label: String = "MemoryDump") {
        val runtime = Runtime.getRuntime()
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val usedMemory = totalMemory - freeMemory

        Log.i(TAG, "📊 MEMORY DUMP [$label]:")
        Log.i(TAG, "  - Used: ${usedMemory}MB")
        Log.i(TAG, "  - Free: ${freeMemory}MB")
        Log.i(TAG, "  - Total: ${totalMemory}MB")
        Log.i(TAG, "  - Max: ${maxMemory}MB")
        Log.i(TAG, "  - Active ViewModels: ${activeViewModels.size}")
        Log.i(TAG, "  - Active Jobs: ${activeJobs.size}")

        activeViewModels.forEach { (name, info) ->
            val lifetime = System.currentTimeMillis() - info.createdAt
            Log.i(TAG, "  - VM: $name (${lifetime}ms old, Thread: ${info.threadName})")
        }

        activeJobs.forEach { (key, info) ->
            val lifetime = System.currentTimeMillis() - info.createdAt
            Log.i(TAG, "  - Job: ${info.owner} -> ${info.description} (${lifetime}ms old)")
        }
    }

    /**
     * Force garbage collection and log results
     */
    fun forceGcAndLog(label: String = "GC") {
        Log.d(TAG, "🗑️ Forcing GC: $label")
        val beforeUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        System.gc()
        System.runFinalization()
        Thread.sleep(100) // Brief pause for GC

        val afterUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val freed = (beforeUsed - afterUsed) / 1024 / 1024

        Log.d(TAG, "🗑️ GC Complete: Freed ${freed}MB")
        logMemoryDump("$label-AfterGC")
    }

    // Data classes for tracking
    private data class ViewModelInfo(
        val name: String,
        val viewModelRef: WeakReference<ViewModel>,
        val createdAt: Long,
        val threadId: Long,
        val threadName: String
    )

    private data class JobInfo(
        val owner: String,
        val description: String,
        val jobRef: WeakReference<Job>,
        val createdAt: Long,
        val threadId: Long
    )

    // Leak event types for testing
    sealed class LeakEvent {
        data class ViewModelLeak(val name: String, val lifetime: Long) : LeakEvent()
        data class JobLeak(val owner: String, val description: String) : LeakEvent()
    }
}

/**
 * Extension function for ViewModel leak tracking
 */
fun ViewModel.trackMemoryLeaks(name: String) {
    MemoryLeakTracker.registerViewModel(this, name)

    // Note: We can't override onCleared() here, so ViewModels must call unregister manually
}

/**
 * Extension function for Job leak tracking
 */
fun Job.trackMemoryLeaks(owner: String, description: String) {
    MemoryLeakTracker.registerJob(this, owner, description)

    // Auto-unregister when completed/cancelled
    this.invokeOnCompletion {
        MemoryLeakTracker.unregisterJob(owner, description)
    }
}
