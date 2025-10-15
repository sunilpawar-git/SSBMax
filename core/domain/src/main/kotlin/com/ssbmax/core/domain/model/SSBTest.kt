package com.ssbmax.core.domain.model

import kotlin.time.Duration

/**
 * SSB Test domain model
 * Represents any test in the SSB assessment process
 */
data class SSBTest(
    val id: String,
    val type: TestType,
    val category: SSBCategory,
    val title: String,
    val description: String,
    val timeLimit: Duration,
    val questionCount: Int,
    val instructions: String,
    val isPremium: Boolean = false
)

/**
 * SSB Assessment Categories
 */
enum class SSBCategory {
    SCREENING,      // Stage I
    PSYCHOLOGY,     // Stage II - Psychological Tests
    GTO,            // Stage II - Group Testing Officer
    INTERVIEW,      // Stage II - Interview Officer
    CONFERENCE      // Stage III - Final Conference
}

/**
 * Test session tracking
 */
data class TestSession(
    val sessionId: String,
    val testId: String,
    val userId: String,
    val startedAt: Long,
    val currentQuestion: Int = 0,
    val responses: Map<String, String> = emptyMap()
)

/**
 * Basic test result model for submissions
 */
data class TestResult(
    val id: String,
    val testId: String,
    val userId: String,
    val score: Float,
    val maxScore: Float = 100f,
    val completedAt: Long,
    val timeSpent: Duration
) {
    /**
     * Whether the test was passed (60% or higher)
     */
    val passed: Boolean
        get() = score >= 60f
}

