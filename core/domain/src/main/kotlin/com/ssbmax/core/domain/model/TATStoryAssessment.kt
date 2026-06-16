package com.ssbmax.core.domain.model

import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQScore
import java.util.UUID

/**
 * Per-story AI assessment for a single TAT image+story pair.
 * Cached locally so per-story scores are available without re-analyzing.
 */
data class TATStoryAssessment(
    val id: String = UUID.randomUUID().toString(),
    val submissionId: String,
    val questionId: String,
    val storyIndex: Int,
    val story: String,
    val imageUrl: String,
    val olqScores: Map<OLQ, OLQScore>,
    val overallScore: Float,
    val overallRating: String,
    val aiConfidence: Int,
    val analyzedAt: Long = System.currentTimeMillis()
)
