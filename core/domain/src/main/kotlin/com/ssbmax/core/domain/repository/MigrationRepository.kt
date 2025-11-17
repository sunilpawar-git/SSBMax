package com.ssbmax.core.domain.repository

/**
 * Repository interface for content migration operations
 *
 * Abstracts the migration of study content from local providers to Firestore,
 * following the repository pattern to keep domain layer independent of infrastructure.
 */
interface MigrationRepository {

    /**
     * Migrate topic introduction content to Firestore
     *
     * @param topicType The type of topic to migrate (e.g., "OIR", "PPDT")
     * @return Result indicating success or failure
     */
    suspend fun migrateTopicContent(topicType: String): Result<Unit>

    /**
     * Migrate study materials for a specific topic to Firestore
     *
     * @param topicType The type of topic whose materials to migrate
     * @param onProgress Progress callback with (current, total) material indices
     * @return Result containing number of successfully migrated materials
     */
    suspend fun migrateStudyMaterials(
        topicType: String,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Int>

    /**
     * Clear Firestore offline cache
     *
     * Forces the app to fetch fresh data from server on next access.
     * May fail if there are active listeners.
     *
     * @return Result indicating success or failure
     */
    suspend fun clearFirestoreCache(): Result<Unit>

    /**
     * Force refresh content from Firestore server
     *
     * Bypasses cache and fetches latest data from server.
     *
     * @param topicType The type of topic to refresh
     * @return Result indicating success or failure
     */
    suspend fun forceRefreshContent(topicType: String): Result<Unit>
}
