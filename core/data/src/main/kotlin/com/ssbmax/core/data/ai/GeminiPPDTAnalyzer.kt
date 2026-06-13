package com.ssbmax.core.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.ssbmax.core.data.ai.prompts.PPDTPrompts
import com.ssbmax.core.domain.model.PPDTImageContext
import com.ssbmax.core.domain.service.ResponseAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Handles all PPDT-specific Gemini analysis.
 * Created lazily by [GeminiAIService] and receives the shared model instance.
 */
internal class GeminiPPDTAnalyzer(
    private val model: GenerativeModel,
    private val responseTimeout: Long = 60_000L
) {

    companion object {
        private const val TAG = "GeminiPPDTAnalyzer"
    }

    @Deprecated(
        "Use analyzePPDTMultimodal for image-aware analysis",
        ReplaceWith("analyzePPDTMultimodal(ByteArray(0), prompt, PPDTImageContext(), \"\")")
    )
    suspend fun analyzePPDTResponse(prompt: String): Result<ResponseAnalysis> =
        withContext(Dispatchers.IO) {
            try {
                withTimeout(responseTimeout) {
                    Log.d(TAG, "📸 Analyzing PPDT (text-only path)")
                    val response = model.generateContent(prompt)
                    GeminiResponseParser.parseGTOAnalysisResponse(response.text ?: "")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ analyzePPDTResponse failed: ${e::class.java.simpleName}", e)
                Result.failure(e)
            }
        }

    suspend fun analyzePPDTMultimodal(
        imageBytes: ByteArray,
        story: String,
        imageContext: PPDTImageContext,
        candidateGender: String
    ): Result<ResponseAnalysis> = withContext(Dispatchers.IO) {
        try {
            withTimeout(responseTimeout) {
                Log.d(TAG, "📸 Analyzing PPDT multimodal (imageBytes=${imageBytes.size})")
                val prompt = PPDTPrompts.generatePPDTMultimodalPrompt(
                    story = story,
                    imageContext = imageContext,
                    candidateGender = candidateGender
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
            Log.e(TAG, "❌ analyzePPDTMultimodal failed: ${e::class.java.simpleName}", e)
            Result.failure(e)
        }
    }
}
