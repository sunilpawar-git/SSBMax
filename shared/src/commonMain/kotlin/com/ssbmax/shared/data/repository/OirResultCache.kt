package com.ssbmax.shared.data.repository

import com.ssbmax.shared.db.SharedDatabase
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

/**
 * Read-through/write-through SQLDelight cache for OIR results, keyed by
 * submissionId. Closes the Phase 0 exit report's flagged gap ("SQLDelight
 * unexercised at runtime") by giving GitLiveOirResultRepository a real local
 * cache instead of always hitting Firestore.
 */
class OirResultCache(private val database: SharedDatabase) {

    private val json = Json { ignoreUnknownKeys = true }

    fun get(submissionId: String): OirTestResultDto? {
        val row = database.sharedDatabaseQueries
            .selectBySubmissionId(submissionId)
            .executeAsOneOrNull()
            ?: return null
        return runCatching { json.decodeFromString(OirTestResultDto.serializer(), row.resultJson) }
            .getOrNull()
    }

    fun put(submissionId: String, dto: OirTestResultDto) {
        val resultJson = json.encodeToString(OirTestResultDto.serializer(), dto)
        database.sharedDatabaseQueries.insertOrReplace(
            submissionId = submissionId,
            resultJson = resultJson,
            cachedAtEpochMillis = Clock.System.now().toEpochMilliseconds()
        )
    }
}
