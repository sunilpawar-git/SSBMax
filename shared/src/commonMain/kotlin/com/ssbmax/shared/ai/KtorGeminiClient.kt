package com.ssbmax.shared.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

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
    suspend fun generateContent(
        prompt: String,
        temperature: Float = 0.0f,
        maxOutputTokens: Int = 8192
    ): Result<String> {
        return try {
            val response = httpClient.post(
                "$BASE_URL/$modelName:generateContent?key=$apiKey"
            ) {
                contentType(ContentType.Application.Json)
                setBody(
                    GeminiGenerateContentRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
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
