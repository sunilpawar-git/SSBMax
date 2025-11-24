package com.ssbmax.core.data.ai

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.QuestionSource
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.core.domain.service.OLQScoreWithReasoning
import com.ssbmax.core.domain.service.ResponseAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud-based Gemini AI service implementation
 *
 * Uses Firebase Cloud Functions to call Gemini API securely without
 * exposing the API key in the client app.
 *
 * **Production-safe**:
 * - API key never exposed to client
 * - Server-side rate limiting
 * - User authentication enforced
 * - Per-user request tracking
 *
 * **Functions**:
 * - analyzeInterviewResponse: Analyze user's interview response
 * - generateInterviewQuestions: Generate PIQ-based questions
 */
@Singleton
class CloudGeminiAIService @Inject constructor() : AIService {

    companion object {
        private const val TAG = "CloudGeminiAI"

        // Firebase Function names
        private const val FUNCTION_ANALYZE_RESPONSE_INLINE = "analyzeResponseInline"
        private const val FUNCTION_ANALYZE_RESPONSE_STORED = "analyzeInterviewResponse"
        private const val FUNCTION_GENERATE_QUESTIONS = "generateInterviewQuestions"

        // Timeout values (milliseconds)
        private const val RESPONSE_ANALYSIS_TIMEOUT = 30_000L  // 30 seconds
        private const val QUESTION_GENERATION_TIMEOUT = 45_000L  // 45 seconds
    }

    private val functions: FirebaseFunctions = Firebase.functions
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Generate PIQ-based questions via Cloud Function
     */
    override suspend fun generatePIQBasedQuestions(
        piqData: String,
        targetOLQs: List<OLQ>?,
        count: Int,
        difficulty: Int
    ): Result<List<InterviewQuestion>> = withContext(Dispatchers.IO) {
        try {
            // Verify user is authenticated
            val currentUser = auth.currentUser
                ?: return@withContext Result.failure(
                    IllegalStateException("User not authenticated")
                )

            withTimeout(QUESTION_GENERATION_TIMEOUT) {
                // Note: piqData should be a PIQ submission ID, not full JSON
                // The Cloud Function will fetch it from Firestore
                val data = hashMapOf(
                    "piqSubmissionId" to piqData,
                    "questionCount" to count
                )

                val result = functions
                    .getHttpsCallable(FUNCTION_GENERATE_QUESTIONS)
                    .call(data)
                    .await()

                val resultData = result.getData() ?: return@withTimeout Result.failure(
                    IllegalStateException("Cloud function returned null data")
                )
                parseQuestionsResult(resultData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate PIQ-based questions via cloud function", e)
            Result.failure(e)
        }
    }

    /**
     * Generate adaptive questions (not yet implemented in cloud)
     * Falls back to mock implementation for now
     */
    override suspend fun generateAdaptiveQuestions(
        previousQuestions: List<InterviewQuestion>,
        previousResponses: List<String>,
        weakOLQs: List<OLQ>,
        count: Int
    ): Result<List<InterviewQuestion>> = withContext(Dispatchers.IO) {
        // TODO: Implement cloud function for adaptive questions
        Log.w(TAG, "Adaptive questions not yet implemented in cloud - using mock")
        Result.success(generateMockQuestions(count))
    }

    /**
     * Analyze interview response via Cloud Function (Inline)
     *
     * Sends question and response data inline for real-time analysis.
     * No Firestore lookups required.
     *
     * @param question The interview question
     * @param response User's response text
     * @param responseMode How the response was provided (text/voice)
     * @return Analysis with OLQ scores and insights
     */
    override suspend fun analyzeResponse(
        question: InterviewQuestion,
        response: String,
        responseMode: String
    ): Result<ResponseAnalysis> = withContext(Dispatchers.IO) {
        try {
            // Verify user is authenticated
            val currentUser = auth.currentUser
                ?: return@withContext Result.failure(
                    IllegalStateException("User not authenticated")
                )

            withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                // Prepare data for inline cloud function
                val data = hashMapOf(
                    "questionText" to question.questionText,
                    "responseText" to response,
                    "expectedOLQs" to question.expectedOLQs.map { it.name },
                    "responseMode" to responseMode
                )

                Log.d(TAG, "Calling cloud function: $FUNCTION_ANALYZE_RESPONSE_INLINE")

                val result = functions
                    .getHttpsCallable(FUNCTION_ANALYZE_RESPONSE_INLINE)
                    .call(data)
                    .await()

                val resultData = result.getData() ?: return@withTimeout Result.failure(
                    IllegalStateException("Cloud function returned null data")
                )
                parseAnalysisResult(resultData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze response via cloud function", e)
            Result.failure(e)
        }
    }

    /**
     * Generate feedback (not yet implemented in cloud)
     * Falls back to mock implementation for now
     */
    override suspend fun generateFeedback(
        questions: List<InterviewQuestion>,
        responses: List<String>,
        olqScores: Map<OLQ, Float>
    ): Result<String> = withContext(Dispatchers.IO) {
        // TODO: Implement cloud function for comprehensive feedback
        Log.w(TAG, "Feedback generation not yet implemented in cloud - using mock")
        Result.success("Mock feedback: Your performance was good overall.")
    }

    /**
     * Health check (check if user is authenticated)
     */
    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        auth.currentUser != null
    }

    /**
     * Parse questions result from Cloud Function
     */
    private fun parseQuestionsResult(data: Any): Result<List<InterviewQuestion>> {
        return try {
            val map = data as? Map<*, *>
                ?: return Result.failure(IllegalStateException("Invalid response format"))

            if (map["success"] != true) {
                return Result.failure(Exception("Function returned failure"))
            }

            val questionsData = map["questions"] as? List<*>
                ?: return Result.failure(IllegalStateException("Missing questions array"))

            val questions = questionsData.mapNotNull { questionData ->
                parseQuestionData(questionData as? Map<*, *> ?: return@mapNotNull null)
            }

            Result.success(questions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse questions result", e)
            Result.failure(e)
        }
    }

    /**
     * Parse individual question data
     */
    private fun parseQuestionData(data: Map<*, *>): InterviewQuestion? {
        return try {
            val expectedOLQNames: List<*> = data["expectedOLQs"] as? List<*> ?: emptyList<Any>()
            val expectedOLQs = expectedOLQNames.mapNotNull { name ->
                OLQ.entries.find { it.name == name.toString() }
            }

            InterviewQuestion(
                id = data["id"]?.toString() ?: UUID.randomUUID().toString(),
                questionText = data["questionText"]?.toString() ?: return null,
                expectedOLQs = expectedOLQs,
                context = data["context"]?.toString(),
                source = QuestionSource.AI_GENERATED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse question data", e)
            null
        }
    }

    /**
     * Parse analysis result from Cloud Function
     */
    private fun parseAnalysisResult(data: Any): Result<ResponseAnalysis> {
        return try {
            val map = data as? Map<*, *>
                ?: return Result.failure(IllegalStateException("Invalid response format"))

            if (map["success"] != true) {
                return Result.failure(Exception("Function returned failure"))
            }

            val analysisData = map["analysis"] as? Map<*, *>
                ?: return Result.failure(IllegalStateException("Missing analysis data"))

            val olqScoresData = analysisData["olqScores"] as? List<*>
                ?: return Result.failure(IllegalStateException("Missing olqScores"))

            val olqScores = mutableMapOf<OLQ, OLQScoreWithReasoning>()

            olqScoresData.forEach { scoreData ->
                val scoreMap = scoreData as? Map<*, *> ?: return@forEach
                val olqName = scoreMap["olq"]?.toString() ?: return@forEach
                val olq = OLQ.entries.find { it.name == olqName } ?: return@forEach

                val evidenceData: List<*> = scoreMap["evidence"] as? List<*> ?: emptyList<Any>()
                val evidence = evidenceData.mapNotNull { it?.toString() }

                olqScores[olq] = OLQScoreWithReasoning(
                    olq = olq,
                    score = (scoreMap["score"] as? Number)?.toFloat() ?: 5.0f,
                    reasoning = scoreMap["reasoning"]?.toString() ?: "",
                    evidence = evidence
                )
            }

            val insightsData: List<*> = analysisData["keyInsights"] as? List<*> ?: emptyList<Any>()
            val insights = insightsData.mapNotNull { it?.toString() }

            val analysis = ResponseAnalysis(
                olqScores = olqScores,
                overallConfidence = (analysisData["overallConfidence"] as? Number)?.toInt() ?: 50,
                keyInsights = insights,
                suggestedFollowUp = analysisData["suggestedFollowUp"]?.toString()
            )

            Result.success(analysis)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse analysis result", e)
            Result.failure(e)
        }
    }

    /**
     * Generate mock questions as fallback
     */
    private fun generateMockQuestions(count: Int): List<InterviewQuestion> {
        return List(count) { index ->
            InterviewQuestion(
                id = UUID.randomUUID().toString(),
                questionText = "Mock question ${index + 1}",
                expectedOLQs = listOf(OLQ.SELF_CONFIDENCE, OLQ.POWER_OF_EXPRESSION),
                context = "Mock context",
                source = QuestionSource.GENERIC_POOL
            )
        }
    }
}
