package com.ssbmax.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Structured context for a TAT image, produced by the offline enrichment pipeline.
 * Used by per-story multimodal prompt builder to inject per-picture rubric into Gemini.
 * All fields default to empty so pre-enrichment cached images degrade gracefully.
 *
 * `@Serializable` (added in Phase 2's 13th KMP-migration slice): `GitLiveTATImageCacheManager`
 * decodes this directly from the Firestore batch document via kotlinx.serialization (GitLive
 * Firestore has no raw-map decode path), then re-serializes it into the `imageContextJson`
 * string stored per row -- a stricter round-trip than the Android original's raw JSON passthrough.
 */
@Serializable
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
