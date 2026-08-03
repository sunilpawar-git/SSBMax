package com.ssbmax.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tat_story_assessments",
    indices = [
        Index("submissionId"),
        Index("questionId"),
        Index(value = ["submissionId", "storyIndex"], unique = true)
    ]
)
data class TATStoryAssessmentEntity(
    @PrimaryKey val id: String,
    val submissionId: String,
    val questionId: String,
    val storyIndex: Int,
    val story: String,
    val imageUrl: String,
    val olqScoresJson: String,
    val overallScore: Float,
    val overallRating: String,
    val aiConfidence: Int,
    val analyzedAt: Long
)
