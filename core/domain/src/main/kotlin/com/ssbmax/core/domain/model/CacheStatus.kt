package com.ssbmax.core.domain.model

/**
 * Cache status data class
 * Provides statistics about cached OIR questions
 */
data class CacheStatus(
    val cachedQuestions: Int,
    val batchesDownloaded: Int,
    val lastSyncTime: Long?,
    val verbalCount: Int,
    val nonVerbalCount: Int,
    val numericalCount: Int,
    val spatialCount: Int
)
