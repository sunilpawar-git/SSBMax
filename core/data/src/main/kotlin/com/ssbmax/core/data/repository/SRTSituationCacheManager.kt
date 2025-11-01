package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.SRTSituationCacheDao
import com.ssbmax.core.data.local.entity.CachedSRTSituationEntity
import com.ssbmax.core.data.local.entity.SRTBatchMetadataEntity
import com.ssbmax.core.domain.model.SRTCategory
import com.ssbmax.core.domain.model.SRTSituation
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for SRT situation progressive caching
 * Follows the same architecture as OIR and WAT
 */
@Singleton
class SRTSituationCacheManager @Inject constructor(
    private val dao: SRTSituationCacheDao,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "SRTCacheManager"
        private const val COLLECTION_PATH = "test_content/srt/situation_batches"
        private const val METADATA_PATH = "test_content/srt/meta"
        private const val TARGET_CACHE_SIZE = 60 // Full SRT test size
        private const val MIN_CACHE_SIZE = 20 // Minimum before resyncing
    }
    
    /**
     * Initialize cache with first batch
     * Called when app starts or cache is empty
     */
    suspend fun initialSync(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting initial sync...")
            
            val currentCount = dao.getTotalSituationCount()
            if (currentCount >= TARGET_CACHE_SIZE) {
                Log.d(TAG, "Cache already initialized ($currentCount situations)")
                return Result.success(Unit)
            }
            
            // Download first batch
            downloadBatch("batch_001").getOrThrow()
            
            Log.d(TAG, "Initial sync complete")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Initial sync failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download a specific batch from Firestore
     */
    suspend fun downloadBatch(batchId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Downloading batch: $batchId")
            
            val doc = firestore.document("$COLLECTION_PATH/$batchId").get().await()
            
            if (!doc.exists()) {
                throw Exception("Batch $batchId not found in Firestore")
            }
            
            @Suppress("UNCHECKED_CAST")
            val situationsData = doc.get("situations") as? List<Map<String, Any?>> 
                ?: throw Exception("No situations found in batch $batchId")
            
            val version = doc.getString("version") ?: "1.0.0"
            
            val situations = situationsData.mapNotNull { situationMap ->
                try {
                    CachedSRTSituationEntity(
                        id = situationMap["id"] as? String ?: return@mapNotNull null,
                        situation = situationMap["situation"] as? String ?: return@mapNotNull null,
                        sequenceNumber = (situationMap["sequenceNumber"] as? Long)?.toInt() ?: 0,
                        category = situationMap["category"] as? String ?: "GENERAL",
                        timeAllowedSeconds = (situationMap["timeAllowedSeconds"] as? Long)?.toInt() ?: 30,
                        difficulty = situationMap["difficulty"] as? String,
                        batchId = batchId,
                        cachedAt = System.currentTimeMillis(),
                        lastUsed = null,
                        usageCount = 0
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse situation: ${situationMap["id"]}", e)
                    null
                }
            }
            
            if (situations.isEmpty()) {
                throw Exception("No valid situations parsed from batch $batchId")
            }
            
            // Insert situations and metadata
            dao.insertSituations(situations)
            dao.insertBatchMetadata(
                SRTBatchMetadataEntity(
                    batchId = batchId,
                    downloadedAt = System.currentTimeMillis(),
                    situationCount = situations.size,
                    version = version
                )
            )
            
            Log.d(TAG, "Downloaded batch $batchId: ${situations.size} situations")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download batch $batchId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get situations for a test (60 situations, balanced across categories)
     */
    suspend fun getSituationsForTest(count: Int = 60): Result<List<SRTSituation>> {
        return try {
            Log.d(TAG, "Getting $count situations for test")
            
            // Check if cache needs refresh
            val currentCount = dao.getTotalSituationCount()
            if (currentCount < MIN_CACHE_SIZE) {
                Log.w(TAG, "Cache below minimum ($currentCount < $MIN_CACHE_SIZE), syncing...")
                initialSync().getOrThrow()
            }
            
            // Get balanced selection across categories (if enough situations)
            val cachedSituations = if (currentCount >= count) {
                getBalancedSelection(count)
            } else {
                dao.getLeastUsedSituations(count)
            }
            
            if (cachedSituations.isEmpty()) {
                throw Exception("No situations in cache")
            }
            
            // Mark as used
            dao.markSituationsAsUsed(cachedSituations.map { it.id })
            
            // Convert to domain model
            val situations = cachedSituations.map { entity ->
                SRTSituation(
                    id = entity.id,
                    situation = entity.situation,
                    sequenceNumber = entity.sequenceNumber,
                    category = parseSRTCategory(entity.category),
                    timeAllowedSeconds = entity.timeAllowedSeconds
                )
            }
            
            Log.d(TAG, "Retrieved ${situations.size} situations for test")
            Result.success(situations)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get situations for test", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get balanced selection across categories
     * Ensures variety in test questions
     */
    private suspend fun getBalancedSelection(count: Int): List<CachedSRTSituationEntity> {
        val categories = listOf(
            "LEADERSHIP",
            "DECISION_MAKING",
            "CRISIS_MANAGEMENT",
            "ETHICAL_DILEMMA",
            "RESPONSIBILITY",
            "TEAMWORK",
            "INTERPERSONAL",
            "COURAGE",
            "GENERAL"
        )
        
        val perCategory = count / categories.size
        val remainder = count % categories.size
        
        val selected = mutableListOf<CachedSRTSituationEntity>()
        
        categories.forEachIndexed { index, category ->
            val countForCategory = if (index < remainder) perCategory + 1 else perCategory
            val situations = dao.getBalancedByCategory(category, countForCategory)
            selected.addAll(situations)
        }
        
        // If we don't have enough, fill with least used
        if (selected.size < count) {
            val additionalNeeded = count - selected.size
            val additional = dao.getLeastUsedSituations(additionalNeeded)
                .filter { it.id !in selected.map { s -> s.id } }
            selected.addAll(additional)
        }
        
        return selected.shuffled().take(count)
    }
    
    /**
     * Parse category string to enum
     */
    private fun parseSRTCategory(category: String): SRTCategory {
        return try {
            SRTCategory.valueOf(category.uppercase())
        } catch (e: Exception) {
            Log.w(TAG, "Unknown category: $category, defaulting to GENERAL")
            SRTCategory.GENERAL
        }
    }
    
    /**
     * Get cache status for diagnostics
     */
    suspend fun getCacheStatus(): SRTCacheStatus {
        return try {
            val totalSituations = dao.getTotalSituationCount()
            val batches = dao.getTotalBatchCount()
            val batchMetadata = dao.getAllBatchMetadata()
            val lastSyncTime = batchMetadata.maxOfOrNull { it.downloadedAt }
            
            // Get category distribution
            val leadershipCount = try { dao.getSituationCountByCategory("LEADERSHIP") } catch (e: Exception) { 0 }
            val decisionMakingCount = try { dao.getSituationCountByCategory("DECISION_MAKING") } catch (e: Exception) { 0 }
            val crisisCount = try { dao.getSituationCountByCategory("CRISIS_MANAGEMENT") } catch (e: Exception) { 0 }
            val ethicalCount = try { dao.getSituationCountByCategory("ETHICAL_DILEMMA") } catch (e: Exception) { 0 }
            val responsibilityCount = try { dao.getSituationCountByCategory("RESPONSIBILITY") } catch (e: Exception) { 0 }
            
            SRTCacheStatus(
                cachedSituations = totalSituations,
                batchesDownloaded = batches,
                lastSyncTime = lastSyncTime,
                leadershipCount = leadershipCount,
                decisionMakingCount = decisionMakingCount,
                crisisManagementCount = crisisCount,
                ethicalDilemmaCount = ethicalCount,
                responsibilityCount = responsibilityCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache status", e)
            SRTCacheStatus()
        }
    }
    
    /**
     * Clear all cache (for debugging/testing)
     */
    suspend fun clearCache() {
        try {
            Log.d(TAG, "Clearing cache...")
            dao.clearAllSituations()
            Log.d(TAG, "Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }
}

/**
 * SRT cache status for diagnostics
 */
data class SRTCacheStatus(
    val cachedSituations: Int = 0,
    val batchesDownloaded: Int = 0,
    val lastSyncTime: Long? = null,
    val leadershipCount: Int = 0,
    val decisionMakingCount: Int = 0,
    val crisisManagementCount: Int = 0,
    val ethicalDilemmaCount: Int = 0,
    val responsibilityCount: Int = 0
)

