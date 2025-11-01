package com.ssbmax.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching Interview questions locally
 * Follows progressive caching strategy
 * 
 * Interview questions include various categories:
 * - Personal background
 * - Educational background
 * - Current affairs
 * - Leadership scenarios
 * - Decision-making situations
 * - General knowledge
 * - Service-related questions
 */
@Entity(tableName = "cached_interview_questions")
data class CachedInterviewQuestionEntity(
    @PrimaryKey val id: String,
    val question: String,
    val category: String, // personal, educational, current_affairs, leadership, etc.
    val difficulty: String? = null, // easy, medium, hard
    val suggestedAnswer: String? = null, // Guidance for students
    val keyPoints: String? = null, // JSON list of key points to cover
    val commonMistakes: String? = null, // JSON list of common mistakes
    val followUpQuestions: String? = null, // JSON list of possible follow-ups
    val batchId: String,
    val cachedAt: Long,
    val lastUsed: Long?,
    val usageCount: Int = 0
)

