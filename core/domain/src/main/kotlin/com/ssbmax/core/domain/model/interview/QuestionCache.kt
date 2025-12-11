package com.ssbmax.core.domain.model.interview

import java.time.Instant

/**
 * Cached interview question with metadata
 *
 * Questions are cached using a hybrid strategy:
 * - PIQ-based: Generated from candidate's PIQ data (40%)
 * - Generic: Pre-curated question pool from Firestore (40%)
 * - Adaptive: AI-generated based on session progress (20%)
 *
 * @param id Unique cache entry ID
 * @param question The cached interview question
 * @param cacheKey Key used for cache lookup (PIQ ID or "generic")
 * @param cacheType Type of cached question
 * @param createdAt When the question was cached
 * @param usageCount How many times this question has been used
 * @param lastUsedAt When the question was last used (null if never used)
 * @param expiresAt When the cache entry expires (null for generic questions)
 */
data class QuestionCacheEntry(
    val id: String,
    val question: InterviewQuestion,
    val cacheKey: String,
    val cacheType: QuestionCacheType,
    val createdAt: Instant,
    val usageCount: Int = 0,
    val lastUsedAt: Instant? = null,
    val expiresAt: Instant? = null
) {
    init {
        require(id.isNotBlank()) { "Cache entry ID cannot be blank" }
        require(cacheKey.isNotBlank()) { "Cache key cannot be blank" }
        require(usageCount >= 0) { "Usage count cannot be negative" }

        if (cacheType == QuestionCacheType.PIQ_BASED) {
            require(expiresAt != null) { "PIQ-based questions must have expiration" }
        }
    }

    /**
     * Check if cache entry is expired
     */
    fun isExpired(): Boolean {
        return expiresAt != null && Instant.now().isAfter(expiresAt)
    }

    /**
     * Check if question is fresh (used less than 3 times)
     */
    fun isFresh(): Boolean = usageCount < 3

    /**
     * Get age of cache entry in days
     */
    fun getAgeInDays(): Long {
        val now = Instant.now()
        return (now.epochSecond - createdAt.epochSecond) / 86400
    }
}

/**
 * Question cache type classification
 */
enum class QuestionCacheType(val displayName: String, val percentage: Int) {
    /**
     * Questions generated from candidate's PIQ data
     * Personalized to candidate's background, interests, and experiences
     * Expires after 30 days or when PIQ is updated
     */
    PIQ_BASED("PIQ-Based Questions", 40),

    /**
     * Pre-curated generic questions from Firestore pool
     * High-quality questions covering all OLQs
     * Never expire, refreshed periodically
     */
    GENERIC("Generic Questions", 40),

    /**
     * AI-generated adaptive questions during interview
     * Generated based on candidate's responses in real-time
     * Not cached, generated on-demand
     */
    ADAPTIVE("Adaptive Questions", 20);

    companion object {
        /**
         * Calculate number of questions for each type
         */
        fun calculateDistribution(totalQuestions: Int): Map<QuestionCacheType, Int> {
            return mapOf(
                PIQ_BASED to (totalQuestions * PIQ_BASED.percentage / 100),
                GENERIC to (totalQuestions * GENERIC.percentage / 100),
                ADAPTIVE to (totalQuestions * ADAPTIVE.percentage / 100)
            )
        }
    }
}

/**
 * Question cache statistics for monitoring
 *
 * @param totalCached Total number of cached questions
 * @param piqBasedCount Number of PIQ-based questions cached
 * @param genericCount Number of generic questions cached
 * @param expiredCount Number of expired entries
 * @param mostUsedQuestions Top 5 most frequently used questions
 * @param cacheHitRate Percentage of cache hits vs misses (0-100)
 * @param avgGenerationTime Average time to generate new questions (milliseconds)
 */
data class QuestionCacheStats(
    val totalCached: Int,
    val piqBasedCount: Int,
    val genericCount: Int,
    val expiredCount: Int,
    val mostUsedQuestions: List<QuestionCacheEntry>,
    val cacheHitRate: Float,
    val avgGenerationTime: Long
) {
    init {
        require(totalCached >= 0) { "Total cached cannot be negative" }
        require(piqBasedCount >= 0) { "PIQ-based count cannot be negative" }
        require(genericCount >= 0) { "Generic count cannot be negative" }
        require(expiredCount >= 0) { "Expired count cannot be negative" }
        require(cacheHitRate in 0f..100f) { "Cache hit rate must be between 0 and 100" }
        require(avgGenerationTime >= 0) { "Average generation time cannot be negative" }
        require(mostUsedQuestions.size <= 5) { "Most used questions limited to top 5" }
    }
}

/**
 * Question cache repository interface
 */
interface QuestionCacheRepository {

    /**
     * Cache PIQ-based questions for a user
     *
     * @param piqSnapshotId PIQ submission ID to associate with questions
     * @param questions Generated questions to cache
     * @param expirationDays Days until expiration (default 30)
     */
    suspend fun cachePIQQuestions(
        piqSnapshotId: String,
        questions: List<InterviewQuestion>,
        expirationDays: Int = 30
    ): Result<Unit>

    /**
     * Get cached PIQ-based questions
     *
     * @param piqSnapshotId PIQ submission ID
     * @param limit Maximum number of questions to retrieve
     * @param excludeUsed Exclude questions already used in recent sessions
     * @return Cached questions (empty if none found or all expired)
     */
    suspend fun getPIQQuestions(
        piqSnapshotId: String,
        limit: Int,
        excludeUsed: Boolean = true
    ): Result<List<InterviewQuestion>>

    /**
     * Get generic questions from pool
     *
     * @param targetOLQs Specific OLQs to target (null for balanced mix)
     * @param difficulty Difficulty level filter (1-5, null for any)
     * @param limit Maximum number of questions
     * @param excludeUsed Exclude recently used questions
     * @return Generic questions from pool
     */
    suspend fun getGenericQuestions(
        targetOLQs: List<OLQ>? = null,
        difficulty: Int? = null,
        limit: Int,
        excludeUsed: Boolean = true
    ): Result<List<InterviewQuestion>>

    /**
     * Mark question as used
     *
     * Increments usage count and updates last used timestamp
     *
     * @param questionId Question ID
     * @param sessionId Session where question was used
     */
    suspend fun markQuestionUsed(
        questionId: String,
        sessionId: String
    ): Result<Unit>

    /**
     * Clean up expired cache entries
     *
     * Removes expired PIQ-based questions and resets usage counters
     *
     * @return Number of entries removed
     */
    suspend fun cleanupExpired(): Result<Int>

    /**
     * Get cache statistics
     *
     * @param userId User ID for user-specific stats (null for global)
     * @return Cache performance metrics
     */
    suspend fun getCacheStats(userId: String? = null): Result<QuestionCacheStats>

    /**
     * Invalidate PIQ-based cache
     *
     * Call this when PIQ is updated to regenerate personalized questions
     *
     * @param piqSnapshotId PIQ submission ID to invalidate
     */
    suspend fun invalidatePIQCache(piqSnapshotId: String): Result<Unit>
}
