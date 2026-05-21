package com.ssbmax.core.data.ai

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.service.AIService
import com.ssbmax.core.domain.service.ResponseAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
 * - User authentication enforced via Firebase Auth guard
 * - Per-user request tracking
 */
@Singleton
class CloudGeminiAIService @Inject constructor(
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth
) : AIService {

    companion object {
        private const val TAG = "CloudGeminiAI"
        private const val FUNCTION_ANALYZE_RESPONSE_INLINE = "analyzeResponseInline"
        private const val FUNCTION_GENERATE_QUESTIONS = "generateInterviewQuestions"
        private const val RESPONSE_ANALYSIS_TIMEOUT = 30_000L
        private const val QUESTION_GENERATION_TIMEOUT = 45_000L
        private const val RESPONSE_MODE_TEXT = "text"
    }

    // ─── Interview / PIQ ─────────────────────────────────────────────────────

    override suspend fun generatePIQBasedQuestions(
        piqData: String,
        targetOLQs: List<OLQ>?,
        count: Int,
        difficulty: Int
    ): Result<List<InterviewQuestion>> = withContext(Dispatchers.IO) {
        try {
            requireAuth() ?: return@withContext Result.failure(
                IllegalStateException("User not authenticated")
            )
            withTimeout(QUESTION_GENERATION_TIMEOUT) {
                val data = hashMapOf("piqSubmissionId" to piqData, "questionCount" to count)
                val result = functions.getHttpsCallable(FUNCTION_GENERATE_QUESTIONS).call(data).await()
                val resultData = result.getData() ?: return@withTimeout Result.failure(
                    IllegalStateException("Cloud function returned null data")
                )
                CloudGeminiParser.parseQuestionsResult(resultData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate PIQ-based questions via cloud function", e)
            Result.failure(e)
        }
    }

    override suspend fun generateAdaptiveQuestions(
        previousQuestions: List<InterviewQuestion>,
        previousResponses: List<String>,
        weakOLQs: List<OLQ>,
        count: Int
    ): Result<List<InterviewQuestion>> = withContext(Dispatchers.IO) {
        Log.w(TAG, "Adaptive questions not yet implemented in cloud - using mock")
        Result.success(CloudGeminiParser.generateMockQuestions(count))
    }

    override suspend fun analyzeResponse(
        question: InterviewQuestion,
        response: String,
        responseMode: String
    ): Result<ResponseAnalysis> = withContext(Dispatchers.IO) {
        try {
            requireAuth() ?: return@withContext Result.failure(
                IllegalStateException("User not authenticated")
            )
            withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                val data = hashMapOf(
                    "questionText" to question.questionText,
                    "responseText" to response,
                    "expectedOLQs" to question.expectedOLQs.map { it.name },
                    "responseMode" to responseMode
                )
                val result = functions.getHttpsCallable(FUNCTION_ANALYZE_RESPONSE_INLINE).call(data).await()
                val resultData = result.getData() ?: return@withTimeout Result.failure(
                    IllegalStateException("Cloud function returned null data")
                )
                CloudGeminiParser.parseAnalysisResult(resultData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze response via cloud function", e)
            Result.failure(e)
        }
    }

    override suspend fun generateFeedback(
        questions: List<InterviewQuestion>,
        responses: List<String>,
        olqScores: Map<OLQ, Float>
    ): Result<String> = withContext(Dispatchers.IO) {
        Log.w(TAG, "Feedback generation not yet implemented in cloud - using mock")
        Result.success("Mock feedback: Your performance was good overall.")
    }

    // ─── Psychology Tests (TAT, WAT, SRT, SD, PPDT) ──────────────────────────

    override suspend fun analyzeTATResponse(prompt: String): Result<ResponseAnalysis> =
        callCloudAnalysis(taskName = "TAT Analysis", prompt = prompt)

    override suspend fun analyzeWATResponse(prompt: String): Result<ResponseAnalysis> =
        callCloudAnalysis(taskName = "WAT Analysis", prompt = prompt)

    override suspend fun analyzeSRTResponse(prompt: String): Result<ResponseAnalysis> =
        callCloudAnalysis(taskName = "SRT Analysis", prompt = prompt)

    override suspend fun analyzeSDResponse(prompt: String): Result<ResponseAnalysis> =
        callCloudAnalysis(taskName = "SD Analysis", prompt = prompt)

    override suspend fun analyzePPDTResponse(prompt: String): Result<ResponseAnalysis> =
        callCloudAnalysis(taskName = "PPDT Analysis", prompt = prompt)

    // ─── GTO Tests ───────────────────────────────────────────────────────────

    override suspend fun analyzeGTOResponse(
        prompt: String,
        testType: GTOTestType
    ): Result<ResponseAnalysis> =
        callCloudAnalysis(taskName = "GTO ${testType.displayName} Analysis", prompt = prompt)

    // ─── Health check ─────────────────────────────────────────────────────────

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        auth.currentUser != null
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Shared secure call to analyzeResponseInline for all psychology/GTO evaluations.
     *
     * Security: authentication is verified before every request.
     * All 15 OLQs are always sent so the Cloud Function scores every quality.
     */
    private suspend fun callCloudAnalysis(
        taskName: String,
        prompt: String
    ): Result<ResponseAnalysis> = withContext(Dispatchers.IO) {
        try {
            requireAuth() ?: return@withContext Result.failure(
                IllegalStateException("User not authenticated")
            )
            Log.d(TAG, "Calling analyzeResponseInline for: $taskName")
            withTimeout(RESPONSE_ANALYSIS_TIMEOUT) {
                val data = hashMapOf(
                    "questionText" to taskName,
                    "responseText" to prompt,
                    "expectedOLQs" to OLQ.entries.map { it.name },
                    "responseMode" to RESPONSE_MODE_TEXT
                )
                val result = functions.getHttpsCallable(FUNCTION_ANALYZE_RESPONSE_INLINE).call(data).await()
                val resultData = result.getData() ?: return@withTimeout Result.failure(
                    IllegalStateException("Cloud function returned null data")
                )
                CloudGeminiParser.parseAnalysisResult(resultData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call cloud analysis for $taskName", e)
            Result.failure(e)
        }
    }

    /** Returns current user or null — callers must check and return failure. */
    private fun requireAuth() = auth.currentUser
}
