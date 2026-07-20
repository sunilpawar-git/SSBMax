package com.ssbmax.shared.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request/response wire types for Gemini's generateContent REST endpoint (v1beta). */
@Serializable
data class GeminiGenerateContentRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig
)

@Serializable
data class GeminiContent(val parts: List<GeminiPart>)

/**
 * A single part of multimodal content: either plain text, or inline binary
 * data (e.g. a JPEG image) via [inlineData]. Field names/casing
 * (`inline_data`, `mime_type`) are verified against Gemini's public REST
 * contract for generateContent, not guessed -- both this Ktor path and the
 * Android app's `com.google.ai.client.generativeai` SDK ultimately serialize
 * to the same `generativelanguage.googleapis.com` wire shape.
 */
@Serializable
data class GeminiPart(
    val text: String? = null,
    @SerialName("inline_data") val inlineData: GeminiInlineData? = null
)

@Serializable
data class GeminiInlineData(
    @SerialName("mime_type") val mimeType: String,
    val data: String
)

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
