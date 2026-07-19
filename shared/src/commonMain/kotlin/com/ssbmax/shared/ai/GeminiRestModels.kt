package com.ssbmax.shared.ai

import kotlinx.serialization.Serializable

/** Request/response wire types for Gemini's generateContent REST endpoint (v1beta). */
@Serializable
data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig
)

@Serializable
data class GeminiContent(val parts: List<GeminiPart>)

@Serializable
data class GeminiPart(val text: String)

@Serializable
data class GeminiGenerationConfig(
    val temperature: Float,
    val maxOutputTokens: Int
)

@Serializable
data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

@Serializable
data class GeminiCandidate(val content: GeminiContent? = null)

/** Extracts the first text part, mirroring GenerateContentResponse.text on the Android SDK. */
fun GeminiGenerateContentResponse.firstText(): String? =
    candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
