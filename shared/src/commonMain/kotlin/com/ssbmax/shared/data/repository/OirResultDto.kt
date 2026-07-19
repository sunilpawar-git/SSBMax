package com.ssbmax.shared.data.repository

import kotlinx.serialization.Serializable

/**
 * Wire-format DTO for reading an OIR submission document via the GitLive
 * Firestore SDK, which requires kotlinx.serialization for typed document
 * reads (`DocumentSnapshot.data<T>(serializer)`), unlike the Android
 * firebase-firestore-ktx SDK which lets the existing ViewModel read a raw
 * `Map<String, Any>` and hand-parse it (see OIRSubmissionResultViewModel in
 * app/). This is a genuine, structural difference between the two SDKs, not
 * a style choice — flagged as a Phase 2 migration cost item.
 *
 * Mirrors the "data.testResult" nested shape of the existing Firestore
 * document (see OIRSubmissionResultViewModel.parseOIRTestResult).
 */
@Serializable
data class OirSubmissionDocumentDto(
    val data: OirSubmissionDataDto? = null
)

@Serializable
data class OirSubmissionDataDto(
    val testResult: OirTestResultDto? = null
)

@Serializable
data class OirTestResultDto(
    val testId: String = "",
    val sessionId: String = "",
    val userId: String = "",
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val skippedQuestions: Int = 0,
    val timeTakenSeconds: Int = 0,
    val rawScore: Int = 0,
    val percentageScore: Float = 0f,
    val categoryScores: Map<String, OirCategoryScoreDto> = emptyMap(),
    val completedAt: Long = 0L
)

@Serializable
data class OirCategoryScoreDto(
    val totalQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val percentage: Float = 0f
)
