package com.ssbmax.core.data.metrics

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks content fetches to estimate Firestore costs
 * Monitors cache hit rates and projects monthly expenses
 * 
 * Usage:
 * - Call recordFirestoreRead() when fetching from Firestore
 * - Call recordCacheHit() when serving from cache
 * - Call getSnapshot() to see cost projections
 */
@Singleton
class ContentMetrics @Inject constructor() {
    
    private val _firestoreReads = AtomicInteger(0)
    private val _storageDownloads = AtomicInteger(0)
    private val _cacheHits = AtomicInteger(0)
    private val _fallbacksToLocal = AtomicInteger(0)
    
    data class MetricsSnapshot(
        val firestoreReads: Int,
        val storageDownloads: Int,
        val cacheHits: Int,
        val fallbacksToLocal: Int,
        val cacheHitRate: Float,
        val estimatedMonthlyFirestoreReads: Int,
        val estimatedMonthlyStorageGB: Float,
        val estimatedMonthlyCost: CostEstimate
    ) {
        fun toDisplayString(): String {
            return """
                Firestore Reads: $firestoreReads
                Cache Hits: $cacheHits (${String.format("%.1f", cacheHitRate * 100)}%)
                Fallbacks to Local: $fallbacksToLocal
                Storage Downloads: $storageDownloads
                
                Estimated Monthly:
                - Firestore Reads: ${estimatedMonthlyFirestoreReads.format()}
                - Storage Transfer: ${String.format("%.2f", estimatedMonthlyStorageGB)} GB
                - Cost: $${String.format("%.2f", estimatedMonthlyCost.totalCost)}
                - ${if (estimatedMonthlyCost.withinFreeTier) "✓ Within Free Tier" else "⚠ Exceeds Free Tier"}
            """.trimIndent()
        }
        
        private fun Int.format(): String {
            return when {
                this >= 1_000_000 -> "${this / 1_000_000}M"
                this >= 1_000 -> "${this / 1_000}K"
                else -> this.toString()
            }
        }
    }
    
    data class CostEstimate(
        val firestoreCost: Float,
        val storageCost: Float,
        val totalCost: Float,
        val withinFreeTier: Boolean
    ) {
        val breakdown: String
            get() = """
                Firestore: $${String.format("%.2f", firestoreCost)}
                Storage: $${String.format("%.2f", storageCost)}
                Total: $${String.format("%.2f", totalCost)}
            """.trimIndent()
    }
    
    fun recordFirestoreRead() {
        _firestoreReads.incrementAndGet()
    }
    
    fun recordStorageDownload() {
        _storageDownloads.incrementAndGet()
    }
    
    fun recordCacheHit() {
        _cacheHits.incrementAndGet()
    }
    
    fun recordFallback() {
        _fallbacksToLocal.incrementAndGet()
    }
    
    /**
     * Get current metrics snapshot with cost projections
     */
    fun getSnapshot(): MetricsSnapshot {
        val reads = _firestoreReads.get()
        val downloads = _storageDownloads.get()
        val cache = _cacheHits.get()
        val total = reads + cache
        
        val hitRate = if (total > 0) cache.toFloat() / total else 0f
        
        // Estimate monthly (assume 30 days)
        val monthlyReads = reads * 30
        val monthlyStorageGB = (downloads * 30 * 0.5f) / 1024f // Assume 500KB avg per image
        
        // Firestore: 50k free reads/day, then $0.06 per 100k reads
        val freeReads = 50_000 * 30 // 1.5M free reads/month
        val billableReads = maxOf(0, monthlyReads - freeReads)
        val firestoreCost = (billableReads / 100_000f) * 0.06f
        
        // Cloud Storage: 1GB free storage, 10GB free egress, then $0.026/GB
        val freeEgress = 10f // 10GB free
        val billableGB = maxOf(0f, monthlyStorageGB - freeEgress)
        val storageCost = billableGB * 0.026f
        
        val totalCost = firestoreCost + storageCost
        val withinFreeTier = monthlyReads < freeReads && monthlyStorageGB < freeEgress
        
        return MetricsSnapshot(
            firestoreReads = reads,
            storageDownloads = downloads,
            cacheHits = cache,
            fallbacksToLocal = _fallbacksToLocal.get(),
            cacheHitRate = hitRate,
            estimatedMonthlyFirestoreReads = monthlyReads,
            estimatedMonthlyStorageGB = monthlyStorageGB,
            estimatedMonthlyCost = CostEstimate(
                firestoreCost, storageCost, totalCost, withinFreeTier
            )
        )
    }
    
    /**
     * Reset all metrics (useful for testing)
     */
    fun reset() {
        _firestoreReads.set(0)
        _storageDownloads.set(0)
        _cacheHits.set(0)
        _fallbacksToLocal.set(0)
    }
    
    /**
     * Get daily average (based on current metrics)
     */
    fun getDailyAverage(): String {
        val snapshot = getSnapshot()
        return """
            Daily Average:
            - Reads: ${snapshot.firestoreReads}
            - Cache Hits: ${snapshot.cacheHits}
            - Fallbacks: ${snapshot.fallbacksToLocal}
        """.trimIndent()
    }
}

