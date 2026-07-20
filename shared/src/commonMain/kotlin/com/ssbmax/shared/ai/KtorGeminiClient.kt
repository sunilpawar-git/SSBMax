package com.ssbmax.shared.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Raw Gemini REST client, replacing the Android-only
 * `com.google.ai.client.generativeai.GenerativeModel` with a plain Ktor HTTP
 * call — the "Gemini -> Ktor" migration item from the KMP plan. Ktor's
 * multiplatform engines (OkHttp on Android, Darwin on iOS, both already
 * configured in shared/build.gradle.kts) make this call identically on both
 * targets, unlike the Google SDK which is JVM/Android-only.
 */
class KtorGeminiClient(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val modelName: String = "gemini-2.5-flash"
) {
    /**
     * @param imageBytes Raw image bytes to attach as multimodal input (empty = text-only,
     *   matching the Android SDK's `content { blob(...); text(...) }` fallback behavior).
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun generateContent(
        prompt: String,
        imageBytes: ByteArray = ByteArray(0),
        imageMimeType: String = "image/jpeg",
        temperature: Float = 0.0f,
        maxOutputTokens: Int = 8192
    ): Result<String> {
        return try {
            val parts = buildList {
                // Image part precedes the text part, mirroring the Android analyzers'
                // `content { blob(...); text(...) }` ordering.
                if (imageBytes.isNotEmpty()) {
                    add(GeminiPart(inlineData = GeminiInlineData(imageMimeType, Base64.encode(imageBytes))))
                }
                add(GeminiPart(text = prompt))
            }
            val response = httpClient.post(
                "$BASE_URL/$modelName:generateContent?key=$apiKey"
            ) {
                contentType(ContentType.Application.Json)
                setBody(
                    GeminiGenerateContentRequest(
                        contents = listOf(GeminiContent(parts = parts)),
                        generationConfig = GeminiGenerationConfig(temperature, maxOutputTokens)
                    )
                )
            }.body<GeminiGenerateContentResponse>()

            val text = response.firstText()
                ?: return Result.failure(IllegalStateException("No response text from Gemini"))
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }
}
