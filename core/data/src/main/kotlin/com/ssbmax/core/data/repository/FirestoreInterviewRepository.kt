package com.ssbmax.core.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ssbmax.core.domain.model.interview.InterviewLimits
import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.InterviewResult
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.interview.PrerequisiteCheckResult
import com.ssbmax.core.domain.model.interview.QuestionCacheRepository
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.model.PIQSubmission
import com.ssbmax.core.domain.repository.InterviewRepository
import com.ssbmax.core.domain.repository.SubmissionRepository
import com.ssbmax.core.domain.repository.SubscriptionRepository
import com.ssbmax.core.domain.service.AIService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of InterviewRepository
 *
 * **Collections**:
 * - `interview_sessions`: Active and historical interview sessions
 * - `interview_responses`: Candidate responses with OLQ scores
 * - `interview_results`: Final assessment results
 * - `interview_questions`: Temporary question storage during session
 *
 * **Indexes Required** (create in Firebase Console):
 * - interview_sessions: (userId ASC, status ASC, startedAt DESC)
 * - interview_responses: (sessionId ASC, respondedAt ASC)
 * - interview_results: (userId ASC, completedAt DESC)
 */
@Singleton
class FirestoreInterviewRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val questionCacheRepository: QuestionCacheRepository,
    private val aiService: AIService,
    private val submissionRepository: SubmissionRepository,
    private val subscriptionRepository: SubscriptionRepository
) : InterviewRepository {

    private val gson = Gson()

    companion object {
        private const val TAG = "InterviewRepository"
        private const val COLLECTION_SESSIONS = "interview_sessions"
        private const val COLLECTION_RESPONSES = "interview_responses"
        private const val COLLECTION_RESULTS = "interview_results"
        private const val COLLECTION_QUESTIONS = "interview_questions"

        // Field names
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_STATUS = "status"
        private const val FIELD_STARTED_AT = "startedAt"
        private const val FIELD_COMPLETED_AT = "completedAt"
        private const val FIELD_SESSION_ID = "sessionId"
        private const val FIELD_RESPONDED_AT = "respondedAt"
    }

    // Prerequisites validation
    // NOTE: These methods will be called through use cases to avoid circular dependencies

    override suspend fun checkPrerequisites(userId: String): Result<PrerequisiteCheckResult> {
        // Implemented by CheckInterviewPrerequisitesUseCase
        return Result.failure(UnsupportedOperationException("Use CheckInterviewPrerequisitesUseCase instead"))
    }

    override suspend fun checkInterviewLimits(userId: String, mode: InterviewMode): Result<Boolean> {
        // Implemented by CheckInterviewLimitsUseCase
        return Result.failure(UnsupportedOperationException("Use CheckInterviewLimitsUseCase instead"))
    }

    // Session management

    override suspend fun createSession(
        userId: String,
        mode: InterviewMode,
        piqSnapshotId: String,
        consentGiven: Boolean
    ): Result<InterviewSession> {
        return try {
            // Generate questions using internal method
            val questionsResult = internalGenerateQuestions(piqSnapshotId, 10)

            if (questionsResult.isFailure) {
                return Result.failure(
                    questionsResult.exceptionOrNull() ?: Exception("Failed to generate questions")
                )
            }

            val questions = questionsResult.getOrNull() ?: emptyList()

            // Create session
            val session = InterviewSession(
                id = UUID.randomUUID().toString(),
                userId = userId,
                mode = mode,
                status = InterviewStatus.IN_PROGRESS,
                startedAt = Instant.now(),
                completedAt = null,
                piqSnapshotId = piqSnapshotId,
                consentGiven = consentGiven,
                questionIds = questions.map { it.id },
                currentQuestionIndex = 0,
                estimatedDuration = 30
            )

            // Store questions for this session
            val batch = firestore.batch()
            questions.forEach { question ->
                val questionDoc = firestore
                    .collection(COLLECTION_QUESTIONS)
                    .document(question.id)
                batch.set(questionDoc, questionToMap(question))
            }

            // Store session
            val sessionDoc = firestore
                .collection(COLLECTION_SESSIONS)
                .document(session.id)
            batch.set(sessionDoc, sessionToMap(session))

            batch.commit().await()

            Log.d(TAG, "Created interview session: ${session.id} with ${questions.size} questions")
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create interview session", e)
            Result.failure(e)
        }
    }

    override suspend fun getActiveSession(userId: String): Result<InterviewSession?> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SESSIONS)
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_STATUS, InterviewStatus.IN_PROGRESS.name)
                .orderBy(FIELD_STARTED_AT, Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            val session = snapshot.documents.firstOrNull()?.let { doc ->
                mapToSession(doc.data ?: return@let null)
            }

            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active session for user: $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun getSession(sessionId: String): Result<InterviewSession> {
        return try {
            val doc = firestore.collection(COLLECTION_SESSIONS)
                .document(sessionId)
                .get()
                .await()

            val session = mapToSession(doc.data ?: return Result.failure(
                IllegalStateException("Session not found: $sessionId")
            ))

            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get session: $sessionId", e)
            Result.failure(e)
        }
    }

    override suspend fun updateSession(session: InterviewSession): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_SESSIONS)
                .document(session.id)
                .set(sessionToMap(session))
                .await()

            Log.d(TAG, "Updated session: ${session.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update session: ${session.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun abandonSession(sessionId: String): Result<Unit> {
        return try {
            val sessionResult = getSession(sessionId)
            if (sessionResult.isFailure) {
                return Result.failure(sessionResult.exceptionOrNull() ?: Exception("Session not found"))
            }

            val session = sessionResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Session is null despite successful result"))

            val abandonedSession = session.copy(
                status = InterviewStatus.ABANDONED,
                completedAt = Instant.now()
            )

            updateSession(abandonedSession)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to abandon session: $sessionId", e)
            Result.failure(e)
        }
    }

    // Question management

    override suspend fun generateQuestions(
        sessionId: String,
        piqSnapshotId: String,
        count: Int
    ): Result<List<InterviewQuestion>> {
        return internalGenerateQuestions(piqSnapshotId, count)
    }

    /**
     * Internal method to generate questions using the hybrid caching strategy
     *
     * Strategy:
     * 1. Try to get questions from cache (fast, no API cost)
     *    - 70% PIQ-based questions (pre-generated by background worker after PIQ submission)
     *    - 25% Generic questions (from permanent curated question pool)
     *    - 5% reserved for future adaptive AI questions based on interview progress
     * 2. If cache empty, use AI to generate personalized questions from PIQ
     * 3. Cache the AI-generated questions for future use (30-day expiration)
     * 4. Fall back to mock questions only if AI fails (development only)
     */
    private suspend fun internalGenerateQuestions(
        piqSnapshotId: String,
        count: Int
    ): Result<List<InterviewQuestion>> {
        return try {
            // Calculate 70/25/5 distribution (5% reserved for future adaptive questions)
            val piqCount = (count * 0.7f).toInt()
            val genericCount = (count * 0.25f).toInt()

            // STEP 1: Try to get questions from cache
            val piqQuestions = questionCacheRepository.getPIQQuestions(
                piqSnapshotId = piqSnapshotId,
                limit = piqCount,
                excludeUsed = true
            ).getOrDefault(emptyList())

            val genericQuestions = questionCacheRepository.getGenericQuestions(
                targetOLQs = null,
                difficulty = 3,
                limit = genericCount,
                excludeUsed = true
            ).getOrDefault(emptyList())

            // Combine and shuffle cached questions
            var allQuestions = (piqQuestions + genericQuestions).shuffled()

            // STEP 2: If cache is empty, use AI to generate personalized questions
            if (allQuestions.isEmpty()) {
                Log.i(TAG, "📝 Question cache empty. Generating $count AI-powered questions from PIQ data...")

                // Get PIQ submission data
                val piqResult = submissionRepository.getSubmission(piqSnapshotId)

                if (piqResult.isSuccess) {
                    val piqSubmissionMap = piqResult.getOrNull()

                    if (piqSubmissionMap != null) {
                        // Convert PIQ Map to JSON for AI (safer than deserialization)
                        val piqJson = convertPIQMapToJson(piqSubmissionMap)

                        if (piqJson != "{}") {
                            // Generate AI questions
                            val aiQuestionsResult = aiService.generatePIQBasedQuestions(
                                piqData = piqJson,
                                targetOLQs = null, // Generate balanced set
                                count = count,
                                difficulty = 3
                            )

                            if (aiQuestionsResult.isSuccess) {
                                val aiQuestions = aiQuestionsResult.getOrNull() ?: emptyList()

                                if (aiQuestions.isNotEmpty()) {
                                    Log.i(TAG, "✅ AI generated ${aiQuestions.size} personalized questions!")

                                    // Cache the AI-generated questions for future use
                                    questionCacheRepository.cachePIQQuestions(
                                        piqSnapshotId = piqSnapshotId,
                                        questions = aiQuestions,
                                        expirationDays = 30
                                    )

                                    allQuestions = aiQuestions
                                } else {
                                    Log.e(TAG, "AI question generation returned empty list for PIQ: $piqSnapshotId",
                                        Exception("AI returned empty questions list"))
                                }
                            } else {
                                Log.e(TAG, "AI question generation failed for PIQ: $piqSnapshotId",
                                    aiQuestionsResult.exceptionOrNull() ?: Exception("Unknown AI error"))
                            }
                        } else {
                            Log.e(TAG, "PIQ Map conversion to JSON failed for ID: $piqSnapshotId",
                                Exception("Could not convert PIQ Map to JSON"))
                        }
                    } else {
                        Log.e(TAG, "PIQ submission map is null for ID: $piqSnapshotId",
                            Exception("PIQ submission map is null"))
                    }
                } else {
                    Log.e(TAG, 
                        // piqResult.exceptionOrNull() ?: Exception("Unknown error"),
                        "Could not fetch PIQ submission: $piqSnapshotId"
                    )
                }
            }

            // STEP 3: Final fallback to mock questions if AI failed
            if (allQuestions.isEmpty()) {
                Log.w(TAG, "⚠️ AI generation failed. Using $count mock questions for development")
                allQuestions = generateMockQuestions(count)
            }

            Result.success(allQuestions.take(count))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate interview questions", e)
            Result.failure(e)
        }
    }

    /**
     * Convert PIQ submission to JSON string for AI processing
     *
     * FIXED: Replaced reflection-based field extraction with type-safe data class
     * Uses Gson for proper JSON serialization
     */
    private fun convertPIQToJson(piqSubmission: Any): String {
        return try {
            // Type-safe cast with validation
            if (piqSubmission !is PIQSubmission) {
                Log.e(TAG, 
                    // IllegalArgumentException("Invalid PIQ submission type: ${piqSubmission::class.java.name}"),
                    "PIQ submission is not of expected type"
                )
                return "{}"
            }

            // Create a structured DTO for AI processing
            val piqForAI = PIQForAI(
                candidateInfo = CandidateInfo(
                    name = piqSubmission.fullName,
                    age = piqSubmission.age.toIntOrNull() ?: 0,
                    education = "${piqSubmission.educationGraduation.level} - ${piqSubmission.educationGraduation.institution}".trim(),
                    occupation = piqSubmission.presentOccupation
                ),
                family = FamilyInfo(
                    fatherOccupation = piqSubmission.fatherOccupation,
                    motherOccupation = piqSubmission.motherOccupation,
                    siblings = piqSubmission.siblings.map { "${it.name} (${it.age}) - ${it.occupation}" }
                ),
                interests = InterestsInfo(
                    hobbies = piqSubmission.hobbies,
                    sports = piqSubmission.sportsParticipation.map { it.sport }.joinToString(", ").ifBlank { piqSubmission.sports },
                    extraCurricular = piqSubmission.extraCurricularActivities.map { it.activityName }.joinToString(", ")
                ),
                aspirations = AspirationsInfo(
                    whyDefense = piqSubmission.whyDefenseForces,
                    serviceChoice = piqSubmission.choiceOfService,
                    strengths = piqSubmission.strengths,
                    weaknesses = piqSubmission.weaknesses
                ),
                experience = ExperienceInfo(
                    hasNccTraining = piqSubmission.nccTraining.hasTraining,
                    nccDetails = if (piqSubmission.nccTraining.hasTraining) {
                        "${piqSubmission.nccTraining.wing} - ${piqSubmission.nccTraining.certificateObtained}"
                    } else "",
                    workExperience = piqSubmission.workExperience.map { "${it.role} at ${it.company} (${it.duration})" },
                    previousInterviews = piqSubmission.previousInterviews.size
                )
            )

            // Serialize to JSON using Gson
            gson.toJson(piqForAI)
        } catch (e: Exception) {
            Log.e(TAG, 
                // e,
                "Error converting PIQ to JSON for AI processing"
            )
            "{}" // Return empty JSON on error
        }
    }

    /**
     * Convert PIQ Map directly to JSON for AI (avoids complex deserialization)
     * Safely extracts fields with null checks and type casting
     */
    private fun convertPIQMapToJson(piqMap: Map<String, Any>): String {
        return try {
            // Safely extract string fields
            fun getString(key: String): String = (piqMap[key] as? String) ?: ""

            // Safely extract list of maps
            @Suppress("UNCHECKED_CAST")
            fun getListOfMaps(key: String): List<Map<String, Any>> =
                (piqMap[key] as? List<*>)?.filterIsInstance<Map<String, Any>>() ?: emptyList()

            // Safely extract nested map
            @Suppress("UNCHECKED_CAST")
            fun getMap(key: String): Map<String, Any> = (piqMap[key] as? Map<String, Any>) ?: emptyMap()

            // Build simplified PIQ JSON for AI (only essential fields to reduce token usage)
            val simplifiedPIQ = mapOf(
                "name" to getString("fullName"),
                "age" to getString("age"),
                "education" to getString("educationGraduation").ifBlank {
                    getMap("educationGraduation").let { "${it["level"]} - ${it["institution"]}" }
                },
                "occupation" to getString("presentOccupation"),
                "hobbies" to getString("hobbies"),
                "sports" to getListOfMaps("sportsParticipation").joinToString(", ") {
                    (it["sport"] as? String) ?: ""
                }.ifBlank { getString("sports") },
                "whyDefense" to getString("whyDefenseForces"),
                "serviceChoice" to getString("choiceOfService"),
                "strengths" to getString("strengths"),
                "weaknesses" to getString("weaknesses")
            )

            gson.toJson(simplifiedPIQ)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting PIQ Map to JSON", e)
            "{}"
        }
    }

    /**
     * Data classes for AI-optimized PIQ representation
     * Structured for interview question generation
     */
    private data class PIQForAI(
        val candidateInfo: CandidateInfo,
        val family: FamilyInfo,
        val interests: InterestsInfo,
        val aspirations: AspirationsInfo,
        val experience: ExperienceInfo
    )

    private data class CandidateInfo(
        val name: String,
        val age: Int,
        val education: String,
        val occupation: String
    )

    private data class FamilyInfo(
        val fatherOccupation: String,
        val motherOccupation: String,
        val siblings: List<String>
    )

    private data class InterestsInfo(
        val hobbies: String,
        val sports: String,
        val extraCurricular: String
    )

    private data class AspirationsInfo(
        val whyDefense: String,
        val serviceChoice: String,
        val strengths: String,
        val weaknesses: String
    )

    private data class ExperienceInfo(
        val hasNccTraining: Boolean,
        val nccDetails: String,
        val workExperience: List<String>,
        val previousInterviews: Int
    )

    /**
     * Generate mock questions for development when cache is empty
     *
     * **DEVELOPMENT ONLY**: This fallback ensures interview can be tested
     * even when question cache is not populated.
     *
     * In production, questions should come from:
     * 1. Pre-populated Firestore cache (generic questions)
     * 2. AI-generated PIQ-based questions (cached after generation)
     *
     * FIXED: Now reads from JSON asset file instead of hardcoded list
     */
    private fun generateMockQuestions(count: Int): List<InterviewQuestion> {
        return try {
            // Read JSON from assets
            val jsonString = context.assets.open("fallback_interview_questions.json")
                .bufferedReader()
                .use { it.readText() }

            // Parse JSON
            val type = object : TypeToken<FallbackQuestionsFile>() {}.type
            val questionsFile: FallbackQuestionsFile = gson.fromJson(jsonString, type)

            // Convert to InterviewQuestion objects
            val questions = questionsFile.questions.map { dto ->
                InterviewQuestion(
                    id = UUID.randomUUID().toString(),
                    questionText = dto.questionText,
                    expectedOLQs = dto.expectedOLQs.mapNotNull { olqName ->
                        try {
                            OLQ.valueOf(olqName)
                        } catch (e: IllegalArgumentException) {
                            Log.e(TAG, 
                                // e,
                                "Invalid OLQ name in fallback questions: $olqName"
                            )
                            null
                        }
                    },
                    context = dto.context,
                    source = QuestionSource.GENERIC_POOL
                )
            }

            Log.i(TAG, "Loaded ${questions.size} fallback questions from JSON asset")
            questions.take(count)
        } catch (e: Exception) {
            Log.e(TAG, 
                // e,
                "Failed to load fallback questions from JSON, using emergency fallback"
            )

            // Emergency fallback: minimal hardcoded questions if JSON fails to load
            listOf(
                InterviewQuestion(
                    id = UUID.randomUUID().toString(),
                    questionText = "Tell me about yourself and your background.",
                    expectedOLQs = listOf(OLQ.SELF_CONFIDENCE, OLQ.POWER_OF_EXPRESSION),
                    context = "Emergency fallback question",
                    source = QuestionSource.GENERIC_POOL
                ),
                InterviewQuestion(
                    id = UUID.randomUUID().toString(),
                    questionText = "Why do you want to join the armed forces?",
                    expectedOLQs = listOf(OLQ.DETERMINATION, OLQ.SENSE_OF_RESPONSIBILITY),
                    context = "Emergency fallback question",
                    source = QuestionSource.GENERIC_POOL
                )
            ).take(count)
        }
    }

    /**
     * DTOs for parsing fallback questions JSON
     */
    private data class FallbackQuestionsFile(
        val version: String,
        val description: String,
        val lastUpdated: String,
        val questions: List<FallbackQuestionDTO>
    )

    private data class FallbackQuestionDTO(
        val questionText: String,
        val expectedOLQs: List<String>,
        val context: String
    )

    override suspend fun getQuestion(questionId: String): Result<InterviewQuestion> {
        return try {
            val doc = firestore.collection(COLLECTION_QUESTIONS)
                .document(questionId)
                .get()
                .await()

            val question = mapToQuestion(doc.data ?: return Result.failure(
                IllegalStateException("Question not found: $questionId")
            ))

            Result.success(question)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get question: $questionId", e)
            Result.failure(e)
        }
    }

    override suspend fun cacheQuestions(
        piqSnapshotId: String,
        questions: List<InterviewQuestion>
    ): Result<Unit> {
        // Delegate to question cache repository
        return questionCacheRepository.cachePIQQuestions(
            piqSnapshotId = piqSnapshotId,
            questions = questions,
            expirationDays = 30
        )
    }

    override suspend fun getCachedQuestions(
        piqSnapshotId: String,
        limit: Int
    ): Result<List<InterviewQuestion>> {
        // Delegate to question cache repository
        return questionCacheRepository.getPIQQuestions(
            piqSnapshotId = piqSnapshotId,
            limit = limit,
            excludeUsed = true
        )
    }

    // Response management

    override suspend fun submitResponse(response: InterviewResponse): Result<InterviewResponse> {
        return try {
            // Get session to extract userId
            val sessionResult = getSession(response.sessionId)
            if (sessionResult.isFailure) {
                return Result.failure(
                    sessionResult.exceptionOrNull() ?: Exception("Failed to get session")
                )
            }
            val session = sessionResult.getOrNull() ?: return Result.failure(
                Exception("Session not found")
            )

            // Add userId to response map
            val responseMap = responseToMap(response).toMutableMap()
            responseMap["userId"] = session.userId

            firestore.collection(COLLECTION_RESPONSES)
                .document(response.id)
                .set(responseMap)
                .await()

            Log.d(TAG, "Submitted response: ${response.id}")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit response: ${response.id}", e)
            Result.failure(e)
        }
    }

    override suspend fun getResponses(sessionId: String): Result<List<InterviewResponse>> {
        return try {
            // Get session to extract userId (required for Firestore security rule)
            val sessionResult = getSession(sessionId)
            if (sessionResult.isFailure) {
                return Result.failure(
                    sessionResult.exceptionOrNull() ?: Exception("Failed to get session")
                )
            }
            val session = sessionResult.getOrNull() ?: return Result.failure(
                Exception("Session not found")
            )

            // Query with both sessionId AND userId to satisfy security rule
            val snapshot = firestore.collection(COLLECTION_RESPONSES)
                .whereEqualTo(FIELD_SESSION_ID, sessionId)
                .whereEqualTo(FIELD_USER_ID, session.userId)  // CRITICAL: Added for Firestore rules
                .orderBy(FIELD_RESPONDED_AT, Query.Direction.ASCENDING)
                .get()
                .await()

            val responses = snapshot.documents.mapNotNull { doc ->
                mapToResponse(doc.data ?: return@mapNotNull null)
            }

            Log.d(TAG, "Retrieved ${responses.size} responses for session: $sessionId")
            Result.success(responses)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get responses for session: $sessionId", e)
            Result.failure(e)
        }
    }

    override suspend fun getResponse(responseId: String): Result<InterviewResponse> {
        return try {
            val doc = firestore.collection(COLLECTION_RESPONSES)
                .document(responseId)
                .get()
                .await()

            val response = mapToResponse(doc.data ?: return Result.failure(
                IllegalStateException("Response not found: $responseId")
            ))

            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get response: $responseId", e)
            Result.failure(e)
        }
    }

    // Result management

    override suspend fun completeInterview(sessionId: String): Result<InterviewResult> {
        return try {
            // Get session
            val sessionResult = getSession(sessionId)
            if (sessionResult.isFailure) {
                return Result.failure(sessionResult.exceptionOrNull() ?: Exception("Session not found"))
            }
            val session = sessionResult.getOrNull()
                ?: return Result.failure(IllegalStateException("Session is null despite successful result"))

            // Get all responses
            val responsesResult = getResponses(sessionId)
            if (responsesResult.isFailure) {
                return Result.failure(responsesResult.exceptionOrNull() ?: Exception("Failed to get responses"))
            }
            val responses = responsesResult.getOrNull() ?: emptyList()

            // Aggregate OLQ scores
            val olqScoresMap = mutableMapOf<OLQ, MutableList<OLQScore>>()
            responses.forEach { response ->
                response.olqScores.forEach { (olq, score) ->
                    olqScoresMap.getOrPut(olq) { mutableListOf() }.add(score)
                }
            }

            // Calculate average scores per OLQ (SSB 1-10 scale, lower is better)
            val overallOLQScores = olqScoresMap.mapValues { (_, scores) ->
                val avgScore = scores.map { it.score }.average().toInt().coerceIn(1, 10)
                val avgConfidence = scores.map { it.confidence }.average().toInt()
                OLQScore(
                    score = avgScore,
                    confidence = avgConfidence,
                    reasoning = "Aggregated from ${scores.size} responses"
                )
            }

            // Calculate category scores
            val categoryScores = OLQCategory.entries.associateWith { category ->
                val categoryOLQs = OLQ.entries.filter { it.category == category }
                val categoryScoresList = categoryOLQs.mapNotNull { overallOLQScores[it]?.score }
                if (categoryScoresList.isEmpty()) 0f else categoryScoresList.average().toFloat()
            }

            // Identify strengths and weaknesses (SSB: lower scores = better performance)
            val sortedOLQs = overallOLQScores.entries.sortedBy { it.value.score }  // Ascending: lowest = best
            val strengths = sortedOLQs.take(3).map { it.key }      // Top 3 (lowest scores)
            val weaknesses = sortedOLQs.takeLast(3).map { it.key }  // Bottom 3 (highest scores)

            // Calculate overall metrics (SSB scale: 1-10, lower is better)
            val overallConfidence = responses.map { it.confidenceScore }.average().toInt()
            val avgScore = overallOLQScores.values.map { it.score }.average().toFloat()
            val overallRating = avgScore.toInt().coerceIn(1, 10)

            // Create result
            val result = InterviewResult(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                userId = session.userId,
                mode = session.mode,
                completedAt = Instant.now(),
                durationSec = session.getDurationSeconds(),
                totalQuestions = session.questionIds.size,
                totalResponses = responses.size,
                overallOLQScores = overallOLQScores,
                categoryScores = categoryScores,
                overallConfidence = overallConfidence,
                strengths = strengths,
                weaknesses = weaknesses,
                feedback = "Comprehensive feedback will be generated by AI", // Placeholder
                overallRating = overallRating
            )

            // Save result
            firestore.collection(COLLECTION_RESULTS)
                .document(result.id)
                .set(resultToMap(result))
                .await()

            // Update session status
            val completedSession = session.copy(
                status = InterviewStatus.COMPLETED,
                completedAt = Instant.now()
            )
            updateSession(completedSession)

            Log.d(TAG, "Completed interview: $sessionId, result: ${result.id}")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to complete interview: $sessionId", e)
            Result.failure(e)
        }
    }

    override suspend fun getResult(sessionId: String): Result<InterviewResult> {
        return try {
            val snapshot = firestore.collection(COLLECTION_RESULTS)
                .whereEqualTo(FIELD_SESSION_ID, sessionId)
                .limit(1)
                .get()
                .await()

            val result = snapshot.documents.firstOrNull()?.let { doc ->
                mapToResult(doc.data ?: return@let null)
            } ?: return Result.failure(IllegalStateException("Result not found for session: $sessionId"))

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get result for session: $sessionId", e)
            Result.failure(e)
        }
    }

    override suspend fun getResultById(resultId: String): Result<InterviewResult> {
        return try {
            val doc = firestore.collection(COLLECTION_RESULTS)
                .document(resultId)
                .get()
                .await()

            val result = mapToResult(doc.data ?: return Result.failure(
                IllegalStateException("Result not found: $resultId")
            ))

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get result: $resultId", e)
            Result.failure(e)
        }
    }

    override fun getUserResults(userId: String): Flow<List<InterviewResult>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_RESULTS)
            .whereEqualTo(FIELD_USER_ID, userId)
            .orderBy(FIELD_COMPLETED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, 
                        // error,
                        "Error listening to user results for user: $userId"
                    )
                    return@addSnapshotListener
                }

                val results = snapshot?.documents?.mapNotNull { doc ->
                    mapToResult(doc.data ?: return@mapNotNull null)
                } ?: emptyList()

                trySend(results)
            }

        awaitClose {
            listener.remove()
        }
    }

    // Analytics

    override suspend fun getInterviewStats(userId: String): Result<Map<InterviewMode, Int>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_SESSIONS)
                .whereEqualTo(FIELD_USER_ID, userId)
                .whereEqualTo(FIELD_STATUS, InterviewStatus.COMPLETED.name)
                .get()
                .await()

            val stats = snapshot.documents.mapNotNull { doc ->
                mapToSession(doc.data ?: return@mapNotNull null)
            }.groupBy { it.mode }
                .mapValues { it.value.size }

            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get interview stats for user: $userId", e)
            Result.failure(e)
        }
    }

    override suspend fun getRemainingInterviews(userId: String, mode: InterviewMode): Result<Int> {
        // This method provides basic remaining count calculation
        // CheckInterviewLimitsUseCase handles full tier-based validation
        return try {
            // Get tier from subscription repository
            val tierResult = subscriptionRepository.getSubscriptionTier(userId)
            if (tierResult.isFailure) {
                return Result.success(0)
            }

            val tier = tierResult.getOrNull()?.displayName
            if (tier == null) {
                return Result.success(0)
            }

            // Get limit from centralized constants
            val limit = InterviewLimits.getLimit(tier, mode)

            // Get completed interviews
            val stats = getInterviewStats(userId).getOrDefault(emptyMap())
            val completed = stats[mode] ?: 0

            // Calculate remaining
            val remaining = (limit - completed).coerceAtLeast(0)

            Result.success(remaining)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get remaining interviews for user: $userId, mode: $mode", e)
            Result.failure(e)
        }
    }

    // Mapping functions

    private fun sessionToMap(session: InterviewSession): Map<String, Any?> {
        return mapOf(
            "id" to session.id,
            "userId" to session.userId,
            "mode" to session.mode.name,
            "status" to session.status.name,
            "startedAt" to session.startedAt.toEpochMilli(),
            "completedAt" to session.completedAt?.toEpochMilli(),
            "piqSnapshotId" to session.piqSnapshotId,
            "consentGiven" to session.consentGiven,
            "questionIds" to session.questionIds,
            "currentQuestionIndex" to session.currentQuestionIndex,
            "estimatedDuration" to session.estimatedDuration
        )
    }

    private fun mapToSession(data: Map<String, Any>): InterviewSession {
        @Suppress("UNCHECKED_CAST")
        return InterviewSession(
            id = data["id"] as String,
            userId = data["userId"] as String,
            mode = InterviewMode.valueOf(data["mode"] as String),
            status = InterviewStatus.valueOf(data["status"] as String),
            startedAt = Instant.ofEpochMilli(data["startedAt"] as Long),
            completedAt = (data["completedAt"] as? Long)?.let { Instant.ofEpochMilli(it) },
            piqSnapshotId = data["piqSnapshotId"] as String,
            consentGiven = data["consentGiven"] as Boolean,
            questionIds = data["questionIds"] as List<String>,
            currentQuestionIndex = (data["currentQuestionIndex"] as? Long)?.toInt() ?: 0,
            estimatedDuration = (data["estimatedDuration"] as? Long)?.toInt() ?: 30
        )
    }

    private fun questionToMap(question: InterviewQuestion): Map<String, Any?> {
        return mapOf(
            "id" to question.id,
            "questionText" to question.questionText,
            "expectedOLQs" to question.expectedOLQs.map { it.name },
            "context" to question.context,
            "source" to question.source.name
        )
    }

    private fun mapToQuestion(data: Map<String, Any>): InterviewQuestion {
        @Suppress("UNCHECKED_CAST")
        val expectedOLQNames = data["expectedOLQs"] as? List<String> ?: emptyList()
        val expectedOLQs = expectedOLQNames.mapNotNull { name ->
            OLQ.entries.find { it.name == name }
        }

        return InterviewQuestion(
            id = data["id"] as String,
            questionText = data["questionText"] as String,
            expectedOLQs = expectedOLQs,
            context = data["context"] as? String,
            source = QuestionSource.valueOf(data["source"] as? String ?: "GENERIC_POOL")
        )
    }

    private fun responseToMap(response: InterviewResponse): Map<String, Any?> {
        return mapOf(
            "id" to response.id,
            "sessionId" to response.sessionId,
            "questionId" to response.questionId,
            "responseText" to response.responseText,
            "responseMode" to response.responseMode.name,
            "respondedAt" to response.respondedAt.toEpochMilli(),
            "thinkingTimeSec" to response.thinkingTimeSec,
            "audioUrl" to response.audioUrl,
            "olqScores" to response.olqScores.mapKeys { it.key.name }.mapValues { olqScoreToMap(it.value) },
            "confidenceScore" to response.confidenceScore
        )
    }

    private fun mapToResponse(data: Map<String, Any>): InterviewResponse {
        @Suppress("UNCHECKED_CAST")
        val olqScoresData = data["olqScores"] as? Map<String, Map<String, Any>> ?: emptyMap()
        val olqScores = olqScoresData.mapNotNull { (olqName, scoreData) ->
            val olq = OLQ.entries.find { it.name == olqName } ?: return@mapNotNull null
            olq to mapToOLQScore(scoreData)
        }.toMap()

        return InterviewResponse(
            id = data["id"] as String,
            sessionId = data["sessionId"] as String,
            questionId = data["questionId"] as String,
            responseText = data["responseText"] as String,
            responseMode = InterviewMode.valueOf(data["responseMode"] as String),
            respondedAt = Instant.ofEpochMilli(data["respondedAt"] as Long),
            thinkingTimeSec = (data["thinkingTimeSec"] as Long).toInt(),
            audioUrl = data["audioUrl"] as? String,
            olqScores = olqScores,
            confidenceScore = (data["confidenceScore"] as? Long)?.toInt() ?: 0
        )
    }

    private fun resultToMap(result: InterviewResult): Map<String, Any?> {
        return mapOf(
            "id" to result.id,
            "sessionId" to result.sessionId,
            "userId" to result.userId,
            "mode" to result.mode.name,
            "completedAt" to result.completedAt.toEpochMilli(),
            "durationSec" to result.durationSec,
            "totalQuestions" to result.totalQuestions,
            "totalResponses" to result.totalResponses,
            "overallOLQScores" to result.overallOLQScores.mapKeys { it.key.name }.mapValues { olqScoreToMap(it.value) },
            "categoryScores" to result.categoryScores.mapKeys { it.key.name },
            "overallConfidence" to result.overallConfidence,
            "strengths" to result.strengths.map { it.name },
            "weaknesses" to result.weaknesses.map { it.name },
            "feedback" to result.feedback,
            "overallRating" to result.overallRating
        )
    }

    private fun mapToResult(data: Map<String, Any>): InterviewResult {
        @Suppress("UNCHECKED_CAST")
        val overallOLQScoresData = data["overallOLQScores"] as? Map<String, Map<String, Any>> ?: emptyMap()
        val overallOLQScores = overallOLQScoresData.mapNotNull { (olqName, scoreData) ->
            val olq = OLQ.entries.find { it.name == olqName } ?: return@mapNotNull null
            olq to mapToOLQScore(scoreData)
        }.toMap()

        val categoryScoresData = data["categoryScores"] as? Map<String, Number> ?: emptyMap()
        val categoryScores = categoryScoresData.mapNotNull { (categoryName, score) ->
            val category = OLQCategory.entries.find { it.name == categoryName } ?: return@mapNotNull null
            category to score.toFloat()
        }.toMap()

        val strengthNames = data["strengths"] as? List<String> ?: emptyList()
        val strengths = strengthNames.mapNotNull { name -> OLQ.entries.find { it.name == name } }

        val weaknessNames = data["weaknesses"] as? List<String> ?: emptyList()
        val weaknesses = weaknessNames.mapNotNull { name -> OLQ.entries.find { it.name == name } }

        return InterviewResult(
            id = data["id"] as String,
            sessionId = data["sessionId"] as String,
            userId = data["userId"] as String,
            mode = InterviewMode.valueOf(data["mode"] as String),
            completedAt = Instant.ofEpochMilli(data["completedAt"] as Long),
            durationSec = data["durationSec"] as Long,
            totalQuestions = (data["totalQuestions"] as Long).toInt(),
            totalResponses = (data["totalResponses"] as Long).toInt(),
            overallOLQScores = overallOLQScores,
            categoryScores = categoryScores,
            overallConfidence = (data["overallConfidence"] as Long).toInt(),
            strengths = strengths,
            weaknesses = weaknesses,
            feedback = data["feedback"] as String,
            overallRating = (data["overallRating"] as Long).toInt()
        )
    }

    private fun olqScoreToMap(score: OLQScore): Map<String, Any> {
        return mapOf(
            "score" to score.score,
            "confidence" to score.confidence,
            "reasoning" to score.reasoning
        )
    }

    private fun mapToOLQScore(data: Map<String, Any>): OLQScore {
        return OLQScore(
            score = (data["score"] as Long).toInt(),
            confidence = (data["confidence"] as Long).toInt(),
            reasoning = data["reasoning"] as String
        )
    }
}
