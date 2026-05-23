package com.ssbmax.core.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.ssbmax.core.data.ai.prompts.SSBInterviewPrompts
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.OLQ
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiQuestionGenerator @Inject constructor() {

    private companion object {
        const val TAG = "GeminiQuestionGenerator"
        const val QUESTION_GENERATION_TIMEOUT = 45_000L
    }

    suspend fun generatePIQBasedQuestions(
        model: GenerativeModel,
        piqData: String,
        targetOLQs: List<OLQ>?,
        count: Int,
        difficulty: Int
    ): Result<List<InterviewQuestion>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📝 Generating $count PIQ-based questions (difficulty: $difficulty)")
            withTimeout(QUESTION_GENERATION_TIMEOUT) {
                val prompt = SSBInterviewPrompts.buildQuestionGenerationPrompt(
                    piqContext = piqData,
                    count = count,
                    difficulty = difficulty,
                    targetOLQs = targetOLQs
                )
                val response = model.generateContent(prompt)
                parseQuestionResponse(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate PIQ-based questions", e)
            Result.failure(e)
        }
    }

    suspend fun generateAdaptiveQuestions(
        model: GenerativeModel,
        previousQuestions: List<InterviewQuestion>,
        previousResponses: List<String>,
        weakOLQs: List<OLQ>,
        count: Int
    ): Result<List<InterviewQuestion>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Generating $count adaptive follow-ups for weak OLQs: ${weakOLQs.map { it.name }}")
            withTimeout(QUESTION_GENERATION_TIMEOUT) {
                val qaHistory = previousQuestions.zip(previousResponses)
                    .map { (q, a) -> q.questionText to a }
                val prompt = SSBInterviewPrompts.buildAdaptiveQuestionPrompt(
                    piqContext = buildContextFromQA(previousQuestions),
                    previousQA = qaHistory,
                    weakOLQs = weakOLQs,
                    count = count
                )
                val response = model.generateContent(prompt)
                parseQuestionResponse(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate adaptive questions", e)
            Result.failure(e)
        }
    }

    private fun buildContextFromQA(questions: List<InterviewQuestion>): String {
        return """
            (Context derived from previous questions asked)
            
            Topics covered so far:
            ${questions.mapIndexed { index, q -> "- Question ${index + 1}: ${q.questionText.take(100)}..." }.joinToString("\n")}
            
            OLQs assessed:
            ${questions.flatMap { it.expectedOLQs }.distinct().joinToString(", ") { it.displayName }}
        """.trimIndent()
    }

    private fun parseQuestionResponse(response: GenerateContentResponse): Result<List<InterviewQuestion>> {
        return try {
            val jsonText = response.text ?: return Result.failure(IllegalStateException("No response text"))
            val cleanJson = extractJsonFromResponse(jsonText)
            val jsonArray = JSONArray(cleanJson)
            val questions = mutableListOf<InterviewQuestion>()
            for (i in 0 until jsonArray.length()) {
                questions.add(parseQuestion(jsonArray.getJSONObject(i)))
            }
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJsonFromResponse(responseText: String): String {
        return when {
            "```json" in responseText -> responseText.substringAfter("```json").substringBefore("```").trim()
            "```" in responseText -> responseText.substringAfter("```").substringBefore("```").trim()
            else -> {
                val trimmed = responseText.trim()
                when {
                    trimmed.startsWith("[") -> trimmed.substringBefore("\n\n").trim()
                    trimmed.startsWith("{") -> trimmed.substringBefore("\n\n").trim()
                    else -> trimmed
                }
            }
        }
    }

    private fun parseQuestion(json: JSONObject): InterviewQuestion {
        val olqsArray = when {
            json.has("targetOLQs") -> json.getJSONArray("targetOLQs")
            json.has("expectedOLQs") -> json.getJSONArray("expectedOLQs")
            else -> JSONArray()
        }
        val expectedOLQs = mutableListOf<OLQ>()
        for (i in 0 until olqsArray.length()) {
            val olqName = olqsArray.getString(i)
            OLQ.entries.find {
                it.name.equals(olqName, ignoreCase = true) || it.displayName.equals(olqName, ignoreCase = true)
            }?.let { expectedOLQs.add(it) }
        }
        val context = buildString {
            json.optString("reasoning", "").let { if (it.isNotBlank()) append(it) }
            json.optString("piqTouchpoint", "").let { if (it.isNotBlank()) append(" [PIQ: $it]") }
        }.ifBlank { null }

        return InterviewQuestion(
            id = json.optString("id", UUID.randomUUID().toString()),
            questionText = json.getString("questionText"),
            expectedOLQs = expectedOLQs,
            context = context,
            source = com.ssbmax.core.domain.model.interview.QuestionSource.AI_GENERATED
        )
    }
}
