package com.ssbmax.shared.data.repository

import com.ssbmax.shared.domain.model.gto.*
import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import com.ssbmax.shared.domain.repository.GTORepository
import com.ssbmax.shared.domain.repository.TestContentRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * GitLive-Firebase-backed port of the Android `FirestoreGTORepository`. Same collections
 * ("submissions", "gto_results", "user_progress") and `test_content/gto/...` batch paths as the
 * Android original; uses `Query.snapshots`/`DocumentReference.snapshots` (native Flows) in place
 * of the Android SDK's `addSnapshotListener`/`callbackFlow`.
 *
 * One real, documented deviation from the Android original (not silently dropped):
 *
 * 1. **`updateProgress`/`recordTestUsage` are read-then-write instead of the Android original's
 *    `firestore.runTransaction`.** No other `GitLive*Repository` port in this codebase has
 *    exercised GitLive's `Transaction` API yet, and its `set(documentRef, data: Any, merge)`
 *    overload (unlike `WriteBatch.set`, which is `<reified T : Any>`) takes a plain `Any` — i.e.
 *    it can't reliably encode a concrete `@Serializable` DTO without an explicit
 *    `SerializationStrategy`. Rather than risk an unverified API path in a slice this size, this
 *    port uses the same proven `DocumentReference.get()` + `DocumentReference.set()` pattern as
 *    every other port here, accepting a small non-atomic race window under concurrent writes to
 *    the same user's progress doc (same category of documented simplification as
 *    `GitLiveUserProfileRepository`'s UTC-vs-local-timezone gap).
 *
 * A second finding, not a deviation but a correction: the Android original's private
 * `mapToSubmission` only recognizes GD/LECTURETTE/GPE test-type strings — it throws
 * `IllegalArgumentException` for any other value *before* reaching the `when (testType)` branches
 * for PGT/HGT/GOR/IO/CT, making those five branches dead code (unreachable in the original, since
 * `testType` can never resolve to them at that point). This port only implements the three
 * reachable branches ([parseGtoSubmissionTestType]) rather than porting unreachable code forward.
 *
 * **Gap closed (8th slice):** `cacheTestContent`/`clearCache`/`isContentCached` for GD/LECTURETTE
 * now delegate to [GitLiveGTOTaskCacheManager] (the SQLDelight-backed port of the Android
 * `GTOTaskCacheManager`/`GTOTaskCacheDao`), instead of the always-"not cached" defaults the 5th
 * slice left here. Same behavior as the Android original: GPE and the animation-obstacle test
 * types remain no-op placeholders (that was true in the Android original too, not a gap this port
 * introduced).
 */
class GitLiveGTORepository(
    private val testContentRepository: TestContentRepository,
    private val taskCacheManager: GitLiveGTOTaskCacheManager
) : GTORepository {

    private val submissionsCollection = Firebase.firestore.collection(COLLECTION_GTO_SUBMISSIONS)
    private val resultsCollection = Firebase.firestore.collection(COLLECTION_GTO_RESULTS)
    private val progressCollection = Firebase.firestore.collection(COLLECTION_USER_GTO_PROGRESS)

    // ==================== Test Content Management ====================

    override suspend fun getRandomTest(testType: GTOTestType): Result<GTOTest> = try {
        when (testType) {
            GTOTestType.GROUP_DISCUSSION -> {
                val topic = testContentRepository.getRandomGDTopic().getOrThrow()
                Result.success(GTOTest.GDTest(id = randomId(), topic = topic))
            }

            GTOTestType.GROUP_PLANNING_EXERCISE -> getRandomGPEScenario()

            GTOTestType.LECTURETTE -> {
                val topics = testContentRepository.getRandomLecturetteTopics(4).getOrThrow()
                Result.success(GTOTest.LecturetteTest(id = randomId(), topicChoices = topics))
            }

            else -> {
                val obstacles = getObstaclesForTest(testType).getOrThrow()
                Result.success(createGtoAnimationTest(testType, obstacles))
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Same simplification as the Android original: always resolves a random Group Discussion
     * test rather than looking up [testId] specifically. Not a real lookup — not fixed here,
     * documented instead of silently "improved" beyond this slice's scope.
     */
    override suspend fun getTestById(testId: String): Result<GTOTest> =
        getRandomTest(GTOTestType.GROUP_DISCUSSION)

    override suspend fun getRandomGPEScenario(): Result<GTOTest.GPETest> = try {
        val path = "$COLLECTION_TEST_CONTENT/$PATH_GTO/$PATH_SCENARIOS/gpe/$PATH_BATCHES/$DEFAULT_BATCH_ID"
        val doc = Firebase.firestore.document(path).get()
        val batch = if (doc.exists) {
            runCatching { doc.data(GPEScenarioBatchDto.serializer()) }.getOrDefault(GPEScenarioBatchDto())
        } else {
            GPEScenarioBatchDto()
        }

        val scenario = batch.scenarios.randomOrNull()
        if (scenario == null) {
            Result.failure(Exception("No GPE scenarios available"))
        } else {
            Result.success(
                GTOTest.GPETest(
                    id = scenario.id ?: randomId(),
                    imageUrl = scenario.imageUrl ?: "",
                    scenario = scenario.scenario ?: "",
                    solution = scenario.solution,
                    resources = scenario.resources ?: emptyList(),
                    difficulty = scenario.difficulty ?: "Medium"
                )
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getObstaclesForTest(testType: GTOTestType): Result<List<ObstacleConfig>> = try {
        val testTypePath = obstacleTestTypePath(testType)
        if (testTypePath == null) {
            Result.failure(Exception("Test type does not have obstacles"))
        } else {
            val path = "$COLLECTION_TEST_CONTENT/$PATH_GTO/$PATH_OBSTACLES/$testTypePath/obstacle_set_001"
            val doc = Firebase.firestore.document(path).get()
            val set = if (doc.exists) {
                runCatching { doc.data(ObstacleSetDto.serializer()) }.getOrDefault(ObstacleSetDto())
            } else {
                ObstacleSetDto()
            }

            Result.success(set.obstacles.map { it.toDomain() })
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== Submission Management ====================

    override suspend fun getSubmission(submissionId: String): Result<GTOSubmission> = try {
        val doc = submissionsCollection.document(submissionId).get()
        if (!doc.exists) {
            Result.failure(Exception("Submission not found: $submissionId"))
        } else {
            Result.success(doc.data(GTOSubmissionDocDto.serializer()).toDomain())
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeSubmission(submissionId: String): Flow<GTOSubmission?> =
        submissionsCollection.document(submissionId).snapshots
            .map { snap ->
                if (snap.exists) {
                    runCatching { snap.data(GTOSubmissionDocDto.serializer()).toDomain() }.getOrNull()
                } else {
                    null
                }
            }
            .catch { emit(null) }

    override suspend fun updateSubmissionStatus(
        submissionId: String,
        status: GTOSubmissionStatus
    ): Result<Unit> = try {
        submissionsCollection.document(submissionId).update(FIELD_STATUS to status.name)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateSubmissionOLQScores(
        submissionId: String,
        olqScores: Map<OLQ, OLQScore>
    ): Result<Unit> = try {
        val submissionDoc = submissionsCollection.document(submissionId).get()
        if (!submissionDoc.exists) {
            Result.failure(Exception("Submission not found: $submissionId"))
        } else {
            val submissionDto = submissionDoc.data(GTOSubmissionDocDto.serializer())
            val testType = runCatching { GTOTestType.valueOf(submissionDto.testType) }
                .getOrElse { parseGtoSubmissionTestType(submissionDto.testType) ?: GTOTestType.GROUP_DISCUSSION }
            val overallScore = olqScores.values.map { it.score }.average().toFloat()
            val aiConfidence = olqScores.values.map { it.confidence }.average().toInt()

            val resultDto = GTOResultDto(
                submissionId = submissionId,
                userId = submissionDto.userId,
                testType = testType.name,
                olqScores = olqScores.entries.associate { (olq, score) -> olq.name to score.toDto() },
                overallScore = overallScore,
                overallRating = calculateGtoRating(overallScore),
                aiConfidence = aiConfidence,
                analyzedAt = Clock.System.now().toEpochMilliseconds()
            )

            val batch = Firebase.firestore.batch()
            batch.set(resultsCollection.document(submissionId), resultDto)
            batch.update(submissionsCollection.document(submissionId), FIELD_STATUS to GTOSubmissionStatus.COMPLETED.name)
            batch.commit()

            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserSubmissions(
        userId: String,
        testType: GTOTestType?
    ): Result<List<GTOSubmission>> = try {
        var query: Query = submissionsCollection.where { FIELD_USER_ID equalTo userId }
        if (testType != null) {
            query = query.where { FIELD_TEST_TYPE equalTo gtoTestTypeToTestType(testType).name }
        }
        query = query.orderBy(FIELD_SUBMITTED_AT, Direction.DESCENDING)

        val snapshot = query.get()
        val submissions = snapshot.documents.mapNotNull { doc ->
            runCatching { doc.data(GTOSubmissionDocDto.serializer()).toDomain() }.getOrNull()
        }

        Result.success(submissions)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getPendingSubmissions(limit: Int): Result<List<GTOSubmission>> = try {
        val snapshot = submissionsCollection
            .where { FIELD_STATUS equalTo GTOSubmissionStatus.PENDING_ANALYSIS.name }
            .orderBy(FIELD_SUBMITTED_AT, Direction.ASCENDING)
            .limit(limit)
            .get()

        val submissions = snapshot.documents.mapNotNull { doc ->
            runCatching { doc.data(GTOSubmissionDocDto.serializer()).toDomain() }.getOrNull()
        }

        Result.success(submissions)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== Progress Tracking ====================

    override suspend fun getUserProgress(userId: String): Result<GTOProgress> = try {
        val doc = progressCollection.document(userId).get()
        if (!doc.exists) {
            Result.success(GTOProgress(userId = userId))
        } else {
            Result.success(doc.data(GTOProgressDto.serializer()).toDomain(userId))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun observeUserProgress(userId: String): Flow<GTOProgress?> =
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
    override suspend fun updateProgress(
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

    override suspend fun canUserTakeTest(userId: String, testType: GTOTestType): Result<Boolean> =
        getUserProgress(userId).map { it.isTestUnlocked(testType) }

    override suspend fun getCompletedTests(userId: String): Result<List<GTOTestType>> =
        getUserProgress(userId).map { it.completedTests }

    override suspend fun getNextAvailableTest(userId: String): Result<GTOTestType?> =
        getUserProgress(userId).map { it.getNextTest() }

    // ==================== Test Usage Tracking ====================

    /**
     * Read-then-write (not the Android original's `runTransaction`) — see the class doc for why.
     * `submissionId` is unused, matching the Android original's own signature.
     */
    override suspend fun recordTestUsage(
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

    override suspend fun getTestUsageCount(userId: String, testType: GTOTestType): Result<Int> =
        getUserProgress(userId).map { it.testsUsedThisMonth[testType] ?: 0 }

    override suspend fun resetMonthlyUsage(userId: String): Result<Unit> = try {
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

    // ==================== Results & Analysis ====================

    override suspend fun getTestResult(submissionId: String): Result<GTOResult> = try {
        val resultDoc = resultsCollection.document(submissionId).get()

        if (resultDoc.exists) {
            Result.success(resultDoc.data(GTOResultDto.serializer()).toDomain())
        } else {
            // Fallback: legacy data — scores embedded on the submission doc, never migrated to
            // gto_results. (New submissions store scores ONLY in gto_results, so a miss here
            // means either a legacy row or an unscored submission.)
            val submission = getSubmission(submissionId).getOrThrow()

            if (submission.olqScores.isEmpty()) {
                Result.failure(GTOAnalysisUnavailableException(submissionId))
            } else {
                val overallScore = submission.olqScores.values.map { it.score }.average().toFloat()
                val result = GTOResult(
                    submissionId = submissionId,
                    userId = submission.userId,
                    testType = submission.testType,
                    olqScores = submission.olqScores,
                    overallScore = overallScore,
                    overallRating = calculateGtoRating(overallScore),
                    aiConfidence = submission.olqScores.values.map { it.confidence }.average().toInt()
                )

                // Lazy self-heal: migrate the legacy scores into gto_results so the next read
                // hits the primary collection. Best-effort — never fail the read.
                runCatching { resultsCollection.document(submissionId).set(result.toDto()) }

                Result.success(result)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getLatestResult(
        userId: String,
        testType: GTOTestType
    ): Result<GTOResultStatus> = try {
        // Lean path for the dashboard: fetch only the most-recent submission for this type (one
        // ordered query), instead of fetching+joining every completed submission.
        val snapshot = submissionsCollection
            .where { FIELD_USER_ID equalTo userId }
            .where { FIELD_TEST_TYPE equalTo gtoTestTypeToTestType(testType).name }
            .orderBy(FIELD_SUBMITTED_AT, Direction.DESCENDING)
            .limit(1)
            .get()

        val doc = snapshot.documents.firstOrNull()
        if (doc == null) {
            Result.success(GTOResultStatus.NotAttempted)
        } else {
            val submission = doc.data(GTOSubmissionDocDto.serializer()).toDomain()

            getTestResult(submission.id).fold(
                onSuccess = { Result.success(GTOResultStatus.Available(it)) },
                onFailure = { error ->
                    if (error is GTOAnalysisUnavailableException) {
                        Result.success(GTOResultStatus.AnalysisPending)
                    } else {
                        Result.failure(error)
                    }
                }
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getUserResults(
        userId: String,
        testType: GTOTestType?
    ): Result<List<GTOResult>> = try {
        val submissions = getUserSubmissions(userId, testType).getOrThrow()
        // Resolve each submission's result concurrently instead of serially — the Android
        // original does this same async/awaitAll batching (previously a serial mapNotNull).
        val results = coroutineScope {
            submissions
                .filter { it.status == GTOSubmissionStatus.COMPLETED }
                .map { submission -> async { getTestResult(submission.id).getOrNull() } }
                .awaitAll()
                .filterNotNull()
        }

        Result.success(results)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getAverageOLQScores(userId: String): Result<Map<OLQ, Float>> = try {
        val results = getUserResults(userId).getOrThrow()

        if (results.isEmpty()) {
            Result.success(emptyMap())
        } else {
            val olqAverages = OLQ.entries.associateWith { olq ->
                val scores = results.mapNotNull { it.olqScores[olq]?.score }
                if (scores.isNotEmpty()) scores.average().toFloat() else 0f
            }
            Result.success(olqAverages)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ==================== Cache Management ====================
    // See the class doc: GD/LECTURETTE now delegate to GitLiveGTOTaskCacheManager. GPE and the
    // animation-obstacle test types remain no-op / not-yet-implemented, same as the Android
    // original (its own GPEImageCacheManager and Phase-4-deferred obstacle-asset caching).

    override suspend fun cacheTestContent(testType: GTOTestType): Result<Unit> = try {
        when (testType) {
            GTOTestType.GROUP_DISCUSSION, GTOTestType.LECTURETTE -> taskCacheManager.initialSync()
            GTOTestType.GROUP_PLANNING_EXERCISE -> Result.success(Unit)
            else -> Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun clearCache(testType: GTOTestType?): Result<Unit> = try {
        if (testType == null || testType == GTOTestType.GROUP_DISCUSSION || testType == GTOTestType.LECTURETTE) {
            taskCacheManager.clearCache()
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun isContentCached(testType: GTOTestType): Result<Boolean> = try {
        val isCached = when (testType) {
            GTOTestType.GROUP_DISCUSSION, GTOTestType.LECTURETTE ->
                taskCacheManager.getCacheStatus().cachedTasks >= MIN_CACHED_TASKS_THRESHOLD
            GTOTestType.GROUP_PLANNING_EXERCISE -> true
            else -> false
        }
        Result.success(isCached)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private companion object {
        const val COLLECTION_TEST_CONTENT = "test_content"
        const val COLLECTION_GTO_SUBMISSIONS = "submissions"
        const val COLLECTION_GTO_RESULTS = "gto_results"
        const val COLLECTION_USER_GTO_PROGRESS = "user_progress"

        const val PATH_GTO = "gto"
        const val PATH_SCENARIOS = "scenarios"
        const val PATH_OBSTACLES = "obstacles"
        const val PATH_BATCHES = "batches"

        const val FIELD_USER_ID = "userId"
        const val FIELD_TEST_TYPE = "testType"
        const val FIELD_STATUS = "status"
        const val FIELD_SUBMITTED_AT = "submittedAt"

        const val DEFAULT_BATCH_ID = "batch_001"

        /** Same threshold as the Android original's isContentCached (>= 5 cached tasks). */
        const val MIN_CACHED_TASKS_THRESHOLD = 5
    }
}

/** Maps an animation [GTOTestType] to its Firestore path segment; null if it has no obstacles. */
internal fun obstacleTestTypePath(testType: GTOTestType): String? = when (testType) {
    GTOTestType.PROGRESSIVE_GROUP_TASK -> "pgt"
    GTOTestType.HALF_GROUP_TASK -> "hgt"
    GTOTestType.GROUP_OBSTACLE_RACE -> "gor"
    GTOTestType.INDIVIDUAL_OBSTACLES -> "io"
    GTOTestType.COMMAND_TASK -> "ct"
    else -> null
}

/** Same rating thresholds as the Android original's `calculateRating`; extracted for direct unit testing. */
internal fun calculateGtoRating(score: Float): String = when {
    score <= 3f -> "Exceptional"
    score <= 4f -> "Excellent"
    score <= 5f -> "Very Good"
    score <= 6f -> "Good"
    score <= 7f -> "Average"
    score <= 8f -> "Below Average"
    else -> "Poor"
}

/**
 * Robustness fix carried over from the Android original: if OLQ scores are present but status
 * still says PENDING_ANALYSIS/ANALYZING, infer COMPLETED so the UI doesn't get stuck.
 */
internal fun inferCompletedGtoStatus(status: GTOSubmissionStatus, hasScores: Boolean): GTOSubmissionStatus =
    if (hasScores && (status == GTOSubmissionStatus.PENDING_ANALYSIS || status == GTOSubmissionStatus.ANALYZING)) {
        GTOSubmissionStatus.COMPLETED
    } else {
        status
    }

/**
 * Only recognizes the three test types the Android original's `mapToSubmission` could ever reach
 * (see class doc: PGT/HGT/GOR/IO/CT were dead code there, since this same parse threw first).
 */
internal fun parseGtoSubmissionTestType(raw: String): GTOTestType? = when (raw) {
    "GTO_GD", "GROUP_DISCUSSION" -> GTOTestType.GROUP_DISCUSSION
    "GTO_LECTURETTE", "LECTURETTE" -> GTOTestType.LECTURETTE
    "GTO_GPE", "GROUP_PLANNING_EXERCISE" -> GTOTestType.GROUP_PLANNING_EXERCISE
    else -> null
}

internal fun createGtoAnimationTest(testType: GTOTestType, obstacles: List<ObstacleConfig>): GTOTest = when (testType) {
    GTOTestType.PROGRESSIVE_GROUP_TASK -> GTOTest.PGTTest(id = randomId(), obstacles = obstacles)
    GTOTestType.HALF_GROUP_TASK -> GTOTest.HGTTest(
        id = randomId(),
        obstacle = obstacles.firstOrNull() ?: defaultObstacleConfig()
    )
    GTOTestType.GROUP_OBSTACLE_RACE -> GTOTest.GORTest(id = randomId(), obstacles = obstacles)
    GTOTestType.INDIVIDUAL_OBSTACLES -> GTOTest.IOTest(id = randomId(), obstacles = obstacles)
    GTOTestType.COMMAND_TASK -> GTOTest.CTTest(
        id = randomId(),
        scenario = "Default command scenario",
        obstacle = obstacles.firstOrNull() ?: defaultObstacleConfig()
    )
    else -> throw IllegalArgumentException("Invalid animation test type: $testType")
}

private fun defaultObstacleConfig(): ObstacleConfig = ObstacleConfig(
    id = "default",
    name = "Default Obstacle",
    description = "Default obstacle",
    difficulty = 1,
    animationAsset = ""
)

internal fun GTOSubmissionDocDto.toDomain(): GTOSubmission {
    val parsedTestType = parseGtoSubmissionTestType(testType)
        ?: throw IllegalArgumentException("Unknown or unsupported GTO submission test type: $testType")

    val status = runCatching { GTOSubmissionStatus.valueOf(status) }.getOrDefault(GTOSubmissionStatus.PENDING_ANALYSIS)
    val parsedScores = olqScores.mapNotNull { (key, dto) ->
        runCatching { OLQ.valueOf(key) }.getOrNull()?.let { it to dto.toDomain() }
    }.toMap()
    val finalStatus = inferCompletedGtoStatus(status, parsedScores.isNotEmpty())

    val hasNestedData = data != null
    val fields = data ?: GTOSubmissionFieldsDto()
    fun <T> pick(nestedVal: T?, rootVal: T?, default: T): T =
        if (hasNestedData) nestedVal ?: default else rootVal ?: default

    return when (parsedTestType) {
        GTOTestType.GROUP_DISCUSSION -> GTOSubmission.GDSubmission(
            id = id,
            userId = userId,
            testId = testId,
            topic = pick(fields.topic, topic, ""),
            response = pick(fields.response, response, ""),
            charCount = pick(fields.charCount, charCount, 0),
            submittedAt = submittedAt,
            timeSpent = pick(fields.timeSpent, timeSpent, 0),
            status = finalStatus,
            olqScores = parsedScores
        )

        GTOTestType.GROUP_PLANNING_EXERCISE -> GTOSubmission.GPESubmission(
            id = id,
            userId = userId,
            testId = testId,
            imageUrl = pick(fields.imageUrl, imageUrl, ""),
            scenario = pick(fields.scenario, scenario, ""),
            solution = if (hasNestedData) fields.solution else solution,
            plan = pick(fields.plan, plan, ""),
            characterCount = pick(fields.characterCount, characterCount, 0),
            submittedAt = submittedAt,
            timeSpent = pick(fields.timeSpent, timeSpent, 0),
            status = finalStatus,
            olqScores = parsedScores
        )

        GTOTestType.LECTURETTE -> GTOSubmission.LecturetteSubmission(
            id = id,
            userId = userId,
            testId = testId,
            topicChoices = pick(fields.topicChoices, topicChoices, emptyList()),
            selectedTopic = pick(fields.selectedTopic, selectedTopic, ""),
            speechTranscript = pick(fields.speechTranscript, speechTranscript, ""),
            charCount = pick(fields.charCount, charCount, 0),
            submittedAt = submittedAt,
            timeSpent = pick(fields.timeSpent, timeSpent, 0),
            status = finalStatus,
            olqScores = parsedScores
        )

        else -> error("Unreachable: parseGtoSubmissionTestType never returns $parsedTestType")
    }
}

internal fun GTOResultDto.toDomain(): GTOResult {
    val testTypeEnum = runCatching { GTOTestType.valueOf(testType) }.getOrDefault(GTOTestType.GROUP_DISCUSSION)
    val parsedScores = olqScores.mapNotNull { (key, dto) ->
        runCatching { OLQ.valueOf(key) }.getOrNull()?.let { it to dto.toDomain() }
    }.toMap()
    // Fill missing OLQs with default zero values, same as the Android original.
    val filledScores = OLQ.entries.associateWith { olq ->
        parsedScores[olq] ?: OLQScore(score = 1, confidence = 0, reasoning = "Not analyzed")
    }

    return GTOResult(
        submissionId = submissionId,
        userId = userId,
        testType = testTypeEnum,
        olqScores = filledScores,
        overallScore = overallScore.coerceIn(1f, 10f),
        overallRating = overallRating,
        aiConfidence = aiConfidence.coerceIn(0, 100),
        analyzedAt = analyzedAt
    )
}

internal fun GTOResult.toDto(): GTOResultDto = GTOResultDto(
    submissionId = submissionId,
    userId = userId,
    testType = testType.name,
    olqScores = olqScores.entries.associate { (olq, score) -> olq.name to score.toDto() },
    overallScore = overallScore,
    overallRating = overallRating,
    aiConfidence = aiConfidence,
    analyzedAt = analyzedAt
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

internal fun ObstacleConfigDto.toDomain(): ObstacleConfig = ObstacleConfig(
    id = id ?: randomId(),
    name = name ?: "",
    description = description ?: "",
    difficulty = difficulty ?: 1,
    animationAsset = animationAsset ?: "",
    resources = resources ?: emptyList(),
    height = height,
    width = width,
    depth = depth
)

internal fun OLQScore.toDto(): OLQScoreDto = OLQScoreDto(score, confidence, reasoning)

internal fun OLQScoreDto.toDomain(): OLQScore = OLQScore(
    score = score.coerceIn(1, 10),
    confidence = confidence.coerceIn(0, 100),
    reasoning = reasoning
)

@Serializable
internal data class OLQScoreDto(
    val score: Int = 6,
    val confidence: Int = 0,
    val reasoning: String = ""
)

@Serializable
internal data class GTOResultDto(
    val submissionId: String = "",
    val userId: String = "",
    val testType: String = "",
    val olqScores: Map<String, OLQScoreDto> = emptyMap(),
    val overallScore: Float = 1f,
    val overallRating: String = "",
    val aiConfidence: Int = 0,
    val analyzedAt: Long = 0L
)

@Serializable
internal data class GTOProgressDto(
    val completedTests: List<String> = emptyList(),
    val testsUsedThisMonth: Map<String, Int> = emptyMap(),
    val lastResetDate: Long = 0L,
    val currentSequentialOrder: Int = 1,
    val lastCompletedAt: Long? = null
)

/** Mirrors the nested `data` object the Android original writes/reads for GD/GPE/Lecturette fields. */
@Serializable
internal data class GTOSubmissionFieldsDto(
    val topic: String? = null,
    val response: String? = null,
    val charCount: Int? = null,
    val imageUrl: String? = null,
    val scenario: String? = null,
    val solution: String? = null,
    val plan: String? = null,
    val characterCount: Int? = null,
    val topicChoices: List<String>? = null,
    val selectedTopic: String? = null,
    val speechTranscript: String? = null,
    val timeSpent: Int? = null
)

/**
 * Submission document shape. Carries both the nested `data` object (current write format) and the
 * flattened root-level fields (legacy format, pre-dating the nested `data` convention) — the
 * Android original reads whichever is present, preferring nested when it exists; see
 * [GTOSubmissionDocDto.toDomain].
 */
@Serializable
internal data class GTOSubmissionDocDto(
    val id: String = "",
    val userId: String = "",
    val testId: String = "",
    val testType: String = "",
    val submittedAt: Long = 0L,
    val timeSpent: Int = 0,
    val status: String = "PENDING_ANALYSIS",
    val olqScores: Map<String, OLQScoreDto> = emptyMap(),
    val data: GTOSubmissionFieldsDto? = null,
    val topic: String? = null,
    val response: String? = null,
    val charCount: Int? = null,
    val imageUrl: String? = null,
    val scenario: String? = null,
    val solution: String? = null,
    val plan: String? = null,
    val characterCount: Int? = null,
    val topicChoices: List<String>? = null,
    val selectedTopic: String? = null,
    val speechTranscript: String? = null
)

@Serializable
internal data class ObstacleConfigDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val difficulty: Int? = null,
    val animationAsset: String? = null,
    val resources: List<String>? = null,
    val height: Float? = null,
    val width: Float? = null,
    val depth: Float? = null
)

@Serializable
internal data class ObstacleSetDto(val obstacles: List<ObstacleConfigDto> = emptyList())

@Serializable
internal data class GPEScenarioDto(
    val id: String? = null,
    val imageUrl: String? = null,
    val scenario: String? = null,
    val solution: String? = null,
    val resources: List<String>? = null,
    val difficulty: String? = null
)

@Serializable
internal data class GPEScenarioBatchDto(val scenarios: List<GPEScenarioDto> = emptyList())
