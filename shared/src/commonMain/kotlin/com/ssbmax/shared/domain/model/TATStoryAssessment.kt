@file:OptIn(ExperimentalUuidApi::class)
package com.ssbmax.shared.domain.model

import kotlinx.datetime.Clock

import com.ssbmax.shared.domain.model.interview.OLQ
import com.ssbmax.shared.domain.model.interview.OLQScore
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Per-story AI assessment for a single TAT image+story pair.
 * Cached locally so per-story scores are available without re-analyzing.
 */
data class TATStoryAssessment(
    val id: String = Uuid.random().toString(),
    val submissionId: String,
    val questionId: String,
    val storyIndex: Int,
    val story: String,
    val imageUrl: String,
    val olqScores: Map<OLQ, OLQScore>,
    val overallScore: Float,
    val overallRating: String,
    val aiConfidence: Int,
    val analyzedAt: Long = Clock.System.now().toEpochMilliseconds()
)
