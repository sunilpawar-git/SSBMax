package com.ssbmax.core.domain.model

/**
 * Structured context for a TAT image, produced by the offline enrichment pipeline.
 * Used by per-story multimodal prompt builder to inject per-picture rubric into Gemini.
 * All fields default to empty so pre-enrichment cached images degrade gracefully.
 */
data class TATImageContext(
    val sceneDescription: String = "",
    val coreElements: List<String> = emptyList(),
    val ambiguousElements: List<String> = emptyList(),
    val expectedThemes: List<String> = emptyList(),
    val penalizedThemes: List<String> = emptyList(),
    val primaryOLQs: List<String> = emptyList(),
    val deviationTolerance: DeviationTolerance = DeviationTolerance.MEDIUM,
    val exemplarGoodHints: List<String> = emptyList(),
    val exemplarBadHints: List<String> = emptyList()
)
