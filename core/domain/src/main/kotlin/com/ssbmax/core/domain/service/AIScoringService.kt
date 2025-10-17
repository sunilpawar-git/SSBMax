package com.ssbmax.core.domain.service

import com.ssbmax.core.domain.model.*

/**
 * AI Scoring Service Interface
 * Defines contract for AI-powered test scoring
 */
interface AIScoringService {
    
    /**
     * Score TAT submission
     */
    suspend fun scoreTAT(submission: TATSubmission): Result<TATAIScore>
    
    /**
     * Score WAT submission
     */
    suspend fun scoreWAT(submission: WATSubmission): Result<WATAIScore>
    
    /**
     * Score SRT submission
     */
    suspend fun scoreSRT(submission: SRTSubmission): Result<SRTAIScore>
    
    /**
     * Get scoring status for a submission
     */
    suspend fun getScoringStatus(submissionId: String): Result<ScoringStatus>
}

/**
 * Scoring status for tracking
 */
data class ScoringStatus(
    val submissionId: String,
    val status: ScoringState,
    val progress: Float = 0f,
    val errorMessage: String? = null
)

/**
 * Scoring state
 */
enum class ScoringState {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

