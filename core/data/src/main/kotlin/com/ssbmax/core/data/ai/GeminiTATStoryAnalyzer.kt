package com.ssbmax.core.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.ssbmax.core.data.ai.prompts.TATStoryAnalysisPrompts
import com.ssbmax.core.domain.model.TATImageContext
import com.ssbmax.core.domain.service.ResponseAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Handles per-story TAT multimodal analysis.
 * Created lazily by [GeminiAIService] and receives the shared model instance.
 */
internal class GeminiTATStoryAnalyzer(
    private val model: GenerativeModel,
    private val responseTimeout: Long = 60_000L
) {

    companion object {
        private const val TAG = "GeminiTATStoryAnalyzer"
    }

    suspend fun analyzeTATStoryMultimodal(
        imageBytes: ByteArray,
        story: String,
        imageContext: TATImageContext,
        candidateGender: String,
        storyIndex: Int,
        totalStories: Int
    ): Result<ResponseAnalysis> = withContext(Dispatchers.IO) {
        try {
            withTimeout(responseTimeout) {
                Log.d(TAG, "📸 Analyzing TAT story $storyIndex/$totalStories multimodal (imageBytes=${imageBytes.size})")
                val prompt = TATStoryAnalysisPrompts.generateTATStoryMultimodalPrompt(
                    story = story,
                    imageContext = imageContext,
                    candidateGender = candidateGender,
                    storyIndex = storyIndex,
                    totalStories = totalStories
                )
                val contentParts = content {
                    if (imageBytes.isNotEmpty()) {
                        blob("image/jpeg", imageBytes)
                    }
                    text(prompt)
                }
                val response = model.generateContent(contentParts)
                GeminiResponseParser.parseGTOAnalysisResponse(response.text ?: "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ analyzeTATStoryMultimodal failed: ${e::class.java.simpleName}", e)
            Result.failure(e)
        }
    }
}
