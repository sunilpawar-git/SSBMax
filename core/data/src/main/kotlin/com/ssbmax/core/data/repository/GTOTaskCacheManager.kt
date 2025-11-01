package com.ssbmax.core.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.ssbmax.core.data.local.dao.GTOTaskCacheDao
import com.ssbmax.core.data.local.entity.CachedGTOTaskEntity
import com.ssbmax.core.data.local.entity.GTOBatchMetadataEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for GTO task progressive caching
 * Follows the same architecture as other test types
 */
@Singleton
class GTOTaskCacheManager @Inject constructor(
    private val dao: GTOTaskCacheDao,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "GTOCacheManager"
        private const val COLLECTION_PATH = "test_content/gto/task_batches"
        private const val METADATA_PATH = "test_content/gto/meta"
        private const val TARGET_CACHE_SIZE = 40 // Multiple GTO tasks
        private const val MIN_CACHE_SIZE = 10 // Minimum before resyncing
    }
    
    /**
     * Initialize cache with first batch
     * Called when app starts or cache is empty
     */
    suspend fun initialSync(): Result<Unit> {
        return try {
            Log.d(TAG, "Starting initial sync...")
            
            val currentCount = dao.getTotalTaskCount()
            if (currentCount >= TARGET_CACHE_SIZE) {
                Log.d(TAG, "Cache already initialized ($currentCount tasks)")
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
            val tasksData = doc.get("tasks") as? List<Map<String, Any?>> 
                ?: throw Exception("No tasks found in batch $batchId")
            
            val version = doc.getString("version") ?: "1.0.0"
            
            val tasks = tasksData.mapNotNull { taskMap ->
                try {
                    CachedGTOTaskEntity(
                        id = taskMap["id"] as? String ?: return@mapNotNull null,
                        taskType = taskMap["taskType"] as? String ?: "GD",
                        title = taskMap["title"] as? String ?: return@mapNotNull null,
                        description = taskMap["description"] as? String ?: "",
                        instructions = taskMap["instructions"] as? String ?: "",
                        timeAllowedMinutes = (taskMap["timeAllowedMinutes"] as? Long)?.toInt() ?: 30,
                        difficultyLevel = taskMap["difficultyLevel"] as? String,
                        category = taskMap["category"] as? String,
                        scenario = taskMap["scenario"] as? String,
                        resources = taskMap["resources"] as? String,
                        objectives = taskMap["objectives"] as? String,
                        evaluationCriteria = taskMap["evaluationCriteria"] as? String,
                        batchId = batchId,
                        cachedAt = System.currentTimeMillis(),
                        lastUsed = null,
                        usageCount = 0
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse task: ${taskMap["id"]}", e)
                    null
                }
            }
            
            if (tasks.isEmpty()) {
                throw Exception("No valid tasks parsed from batch $batchId")
            }
            
            // Insert tasks and metadata
            dao.insertTasks(tasks)
            dao.insertBatchMetadata(
                GTOBatchMetadataEntity(
                    batchId = batchId,
                    downloadedAt = System.currentTimeMillis(),
                    taskCount = tasks.size,
                    version = version
                )
            )
            
            Log.d(TAG, "Downloaded batch $batchId: ${tasks.size} tasks")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download batch $batchId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get tasks by specific type (GD, GPE, etc.)
     */
    suspend fun getTasksByType(taskType: String, count: Int = 1): Result<List<GTOTask>> {
        return try {
            Log.d(TAG, "Getting $count tasks of type $taskType")
            
            // Check if cache needs refresh
            val currentCount = dao.getTotalTaskCount()
            if (currentCount < MIN_CACHE_SIZE) {
                Log.w(TAG, "Cache below minimum ($currentCount < $MIN_CACHE_SIZE), syncing...")
                initialSync().getOrThrow()
            }
            
            // Get least-used tasks of this type
            val cachedTasks = dao.getLeastUsedTasksByType(taskType, count)
            
            if (cachedTasks.isEmpty()) {
                throw Exception("No tasks of type $taskType in cache")
            }
            
            // Mark as used
            dao.markTasksAsUsed(cachedTasks.map { it.id })
            
            // Convert to domain model
            val tasks = cachedTasks.map { entity ->
                GTOTask(
                    id = entity.id,
                    taskType = entity.taskType,
                    title = entity.title,
                    description = entity.description,
                    instructions = entity.instructions,
                    timeAllowedMinutes = entity.timeAllowedMinutes,
                    difficultyLevel = entity.difficultyLevel,
                    category = entity.category,
                    scenario = entity.scenario
                )
            }
            
            Log.d(TAG, "Retrieved ${tasks.size} tasks of type $taskType")
            Result.success(tasks)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get tasks of type $taskType", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get random selection of tasks (for mixed exercises)
     */
    suspend fun getRandomTasks(count: Int = 5): Result<List<GTOTask>> {
        return try {
            Log.d(TAG, "Getting $count random tasks")
            
            // Check if cache needs refresh
            val currentCount = dao.getTotalTaskCount()
            if (currentCount < MIN_CACHE_SIZE) {
                Log.w(TAG, "Cache below minimum ($currentCount < $MIN_CACHE_SIZE), syncing...")
                initialSync().getOrThrow()
            }
            
            // Get least-used tasks
            val cachedTasks = dao.getLeastUsedTasks(count)
            
            if (cachedTasks.isEmpty()) {
                throw Exception("No tasks in cache")
            }
            
            // Mark as used
            dao.markTasksAsUsed(cachedTasks.map { it.id })
            
            // Convert to domain model
            val tasks = cachedTasks.map { entity ->
                GTOTask(
                    id = entity.id,
                    taskType = entity.taskType,
                    title = entity.title,
                    description = entity.description,
                    instructions = entity.instructions,
                    timeAllowedMinutes = entity.timeAllowedMinutes,
                    difficultyLevel = entity.difficultyLevel,
                    category = entity.category,
                    scenario = entity.scenario
                )
            }
            
            Log.d(TAG, "Retrieved ${tasks.size} random tasks")
            Result.success(tasks)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get random tasks", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get cache status for diagnostics
     */
    suspend fun getCacheStatus(): GTOCacheStatus {
        return try {
            val totalTasks = dao.getTotalTaskCount()
            val batches = dao.getTotalBatchCount()
            val batchMetadata = dao.getAllBatchMetadata()
            val lastSyncTime = batchMetadata.maxOfOrNull { it.downloadedAt }
            
            // Get task type distribution
            val gdCount = try { dao.getTaskCountByType("GD") } catch (e: Exception) { 0 }
            val gpeCount = try { dao.getTaskCountByType("GPE") } catch (e: Exception) { 0 }
            val pgtCount = try { dao.getTaskCountByType("PGT") } catch (e: Exception) { 0 }
            val commandCount = try { dao.getTaskCountByType("COMMAND") } catch (e: Exception) { 0 }
            
            GTOCacheStatus(
                cachedTasks = totalTasks,
                batchesDownloaded = batches,
                lastSyncTime = lastSyncTime,
                gdTaskCount = gdCount,
                gpeTaskCount = gpeCount,
                pgtTaskCount = pgtCount,
                commandTaskCount = commandCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cache status", e)
            GTOCacheStatus()
        }
    }
    
    /**
     * Clear all cache (for debugging/testing)
     */
    suspend fun clearCache() {
        try {
            Log.d(TAG, "Clearing cache...")
            dao.clearAllTasks()
            Log.d(TAG, "Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }
}

/**
 * GTO cache status for diagnostics
 */
data class GTOCacheStatus(
    val cachedTasks: Int = 0,
    val batchesDownloaded: Int = 0,
    val lastSyncTime: Long? = null,
    val gdTaskCount: Int = 0, // Group Discussion
    val gpeTaskCount: Int = 0, // Group Planning Exercise
    val pgtTaskCount: Int = 0, // Progressive Group Tasks
    val commandTaskCount: Int = 0 // Command Tasks
)

/**
 * GTO Task domain model (lightweight version)
 */
data class GTOTask(
    val id: String,
    val taskType: String,
    val title: String,
    val description: String,
    val instructions: String,
    val timeAllowedMinutes: Int,
    val difficultyLevel: String?,
    val category: String?,
    val scenario: String?
)

