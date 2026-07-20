package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.gto.GTOProgress
import com.ssbmax.shared.domain.model.gto.GTOTestType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * GTO progress/usage-tracking cluster (`getUserProgress`/`observeUserProgress`/`updateProgress`/
 * `canUserTakeTest`/`getCompletedTests`/`getNextAvailableTest`/`recordTestUsage`/
 * `getTestUsageCount`/`resetMonthlyUsage`), split out of the former single `GitLiveGTORepository`
 * god-class (300-line-file limit). Pure structural split — no behavior change from the original
 * merged class.
 *
 * **`updateProgress`/`recordTestUsage` are read-then-write instead of the Android original's
 * `firestore.runTransaction`** — see [GitLiveGTORepository]'s class doc for why (no other
 * `GitLive*Repository` port in this codebase has exercised GitLive's `Transaction` API yet).
 */
internal class GitLiveGTOProgressDelegate(private val collections: GitLiveGTOCollections) {

    private val progressCollection get() = collections.progress

    suspend fun getUserProgress(userId: String): Result<GTOProgress> = try {
        val doc = progressCollection.document(userId).get()
        if (!doc.exists) {
            Result.success(GTOProgress(userId = userId))
        } else {
            Result.success(doc.data(GTOProgressDto.serializer()).toDomain(userId))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun observeUserProgress(userId: String): Flow<GTOProgress?> =
        progressCollection.document(userId).snapshots
            .map<_, GTOProgress?> { snap ->
                if (snap.exists) {
                    runCatching { snap.data(GTOProgressDto.serializer()).toDomain(userId) }
                        .getOrDefault(GTOProgress(userId = userId))
                } else {
                    GTOProgress(userId = userId)
                }
            }
            .catch { emit(null) }

    /**
     * Read-then-write (not the Android original's `runTransaction`) — see the class doc for why.
     */
    suspend fun updateProgress(
        userId: String,
        completedTestType: GTOTestType
    ): Result<Unit> = try {
        val ref = progressCollection.document(userId)
        val snapshot = ref.get()
        val current = if (snapshot.exists) {
            runCatching { snapshot.data(GTOProgressDto.serializer()) }.getOrDefault(GTOProgressDto())
        } else {
            GTOProgressDto()
        }

        val completedTests = current.completedTests.toMutableList()
        if (completedTestType.name !in completedTests) {
            completedTests.add(completedTestType.name)
        }
        val nextOrder = completedTests
            .mapNotNull { runCatching { GTOTestType.valueOf(it) }.getOrNull()?.order }
            .maxOrNull()
            ?.plus(1) ?: 1

        ref.set(
            current.copy(
                completedTests = completedTests,
                currentSequentialOrder = nextOrder,
                lastCompletedAt = Clock.System.now().toEpochMilliseconds()
            )
        )

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun canUserTakeTest(userId: String, testType: GTOTestType): Result<Boolean> =
        getUserProgress(userId).map { it.isTestUnlocked(testType) }

    suspend fun getCompletedTests(userId: String): Result<List<GTOTestType>> =
        getUserProgress(userId).map { it.completedTests }

    suspend fun getNextAvailableTest(userId: String): Result<GTOTestType?> =
        getUserProgress(userId).map { it.getNextTest() }

    /**
     * Read-then-write (not the Android original's `runTransaction`) — see the class doc for why.
     * `submissionId` is unused, matching the Android original's own signature.
     */
    suspend fun recordTestUsage(
        userId: String,
        testType: GTOTestType,
        submissionId: String
    ): Result<Unit> = try {
        val ref = progressCollection.document(userId)
        val snapshot = ref.get()
        val current = if (snapshot.exists) {
            runCatching { snapshot.data(GTOProgressDto.serializer()) }.getOrDefault(GTOProgressDto())
        } else {
            GTOProgressDto()
        }

        val testsUsed = current.testsUsedThisMonth.toMutableMap()
        testsUsed[testType.name] = (testsUsed[testType.name] ?: 0) + 1

        ref.set(current.copy(testsUsedThisMonth = testsUsed))

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getTestUsageCount(userId: String, testType: GTOTestType): Result<Int> =
        getUserProgress(userId).map { it.testsUsedThisMonth[testType] ?: 0 }

    suspend fun resetMonthlyUsage(userId: String): Result<Unit> = try {
        val ref = progressCollection.document(userId)
        val snapshot = ref.get()
        val current = if (snapshot.exists) {
            runCatching { snapshot.data(GTOProgressDto.serializer()) }.getOrDefault(GTOProgressDto())
        } else {
            GTOProgressDto()
        }

        ref.set(
            current.copy(
                testsUsedThisMonth = emptyMap(),
                lastResetDate = Clock.System.now().toEpochMilliseconds()
            )
        )

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

@Serializable
internal data class GTOProgressDto(
    val completedTests: List<String> = emptyList(),
    val testsUsedThisMonth: Map<String, Int> = emptyMap(),
    val lastResetDate: Long = 0L,
    val currentSequentialOrder: Int = 1,
    val lastCompletedAt: Long? = null
)

internal fun GTOProgressDto.toDomain(userId: String): GTOProgress = GTOProgress(
    userId = userId,
    completedTests = completedTests.mapNotNull { runCatching { GTOTestType.valueOf(it) }.getOrNull() },
    testsUsedThisMonth = testsUsedThisMonth.mapNotNull { (key, value) ->
        runCatching { GTOTestType.valueOf(key) }.getOrNull()?.let { it to value }
    }.toMap(),
    lastResetDate = lastResetDate,
    currentSequentialOrder = currentSequentialOrder,
    lastCompletedAt = lastCompletedAt
)
