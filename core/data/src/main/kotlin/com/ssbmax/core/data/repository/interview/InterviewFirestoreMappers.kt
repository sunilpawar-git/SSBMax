package com.ssbmax.core.data.repository.interview

import com.ssbmax.core.domain.model.interview.InterviewMode
import com.ssbmax.core.domain.model.interview.InterviewQuestion
import com.ssbmax.core.domain.model.interview.InterviewResponse
import com.ssbmax.core.domain.model.interview.InterviewResult
import com.ssbmax.core.domain.model.interview.InterviewSession
import com.ssbmax.core.domain.model.interview.InterviewStatus
import com.ssbmax.core.domain.model.interview.OLQ
import com.ssbmax.core.domain.model.interview.OLQCategory
import com.ssbmax.core.domain.model.interview.OLQScore
import com.ssbmax.core.domain.model.interview.QuestionSource
import java.time.Instant

/**
 * Firestore mapping utilities for interview entities
 *
 * Provides bidirectional conversion between domain models and Firestore maps.
 * All mapping is type-safe with proper null handling.
 */
object InterviewFirestoreMappers {

    // ============================================
    // SESSION MAPPING
    // ============================================

    fun sessionToMap(session: InterviewSession): Map<String, Any?> {
        return mapOf(
            "id" to session.id,
            "userId" to session.userId,
            "mode" to session.mode.name,
            "status" to session.status.name,
            "startedAt" to session.startedAt.toEpochMilli(),
            "completedAt" to session.completedAt?.toEpochMilli(),
            "piqSnapshotId" to session.piqSnapshotId,
            "consentGiven" to session.consentGiven,
            "questionIds" to session.questionIds,
            "currentQuestionIndex" to session.currentQuestionIndex,
            "estimatedDuration" to session.estimatedDuration
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun mapToSession(data: Map<String, Any>): InterviewSession {
        return InterviewSession(
            id = data["id"] as String,
            userId = data["userId"] as String,
            mode = InterviewMode.valueOf(data["mode"] as String),
            status = InterviewStatus.valueOf(data["status"] as String),
            startedAt = Instant.ofEpochMilli(data["startedAt"] as Long),
            completedAt = (data["completedAt"] as? Long)?.let { Instant.ofEpochMilli(it) },
            piqSnapshotId = data["piqSnapshotId"] as String,
            consentGiven = data["consentGiven"] as Boolean,
            questionIds = data["questionIds"] as List<String>,
            currentQuestionIndex = (data["currentQuestionIndex"] as? Long)?.toInt() ?: 0,
            estimatedDuration = (data["estimatedDuration"] as? Long)?.toInt() ?: 30
        )
    }

    // ============================================
    // QUESTION MAPPING
    // ============================================

    fun questionToMap(question: InterviewQuestion): Map<String, Any?> {
        return mapOf(
            "id" to question.id,
            "questionText" to question.questionText,
            "expectedOLQs" to question.expectedOLQs.map { it.name },
            "context" to question.context,
            "source" to question.source.name
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun mapToQuestion(data: Map<String, Any>): InterviewQuestion {
        val expectedOLQNames = data["expectedOLQs"] as? List<String> ?: emptyList()
        val expectedOLQs = expectedOLQNames.mapNotNull { name ->
            OLQ.entries.find { it.name == name }
        }

        return InterviewQuestion(
            id = data["id"] as String,
            questionText = data["questionText"] as String,
            expectedOLQs = expectedOLQs,
            context = data["context"] as? String,
            source = QuestionSource.valueOf(data["source"] as? String ?: "GENERIC_POOL")
        )
    }

    // ============================================
    // RESPONSE MAPPING
    // ============================================

    fun responseToMap(response: InterviewResponse): Map<String, Any?> {
        return mapOf(
            "id" to response.id,
            "sessionId" to response.sessionId,
            "questionId" to response.questionId,
            "responseText" to response.responseText,
            "responseMode" to response.responseMode.name,
            "respondedAt" to response.respondedAt.toEpochMilli(),
            "thinkingTimeSec" to response.thinkingTimeSec,
            "audioUrl" to response.audioUrl,
            "olqScores" to response.olqScores.mapKeys { it.key.name }.mapValues { olqScoreToMap(it.value) },
            "confidenceScore" to response.confidenceScore
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun mapToResponse(data: Map<String, Any>): InterviewResponse {
        val olqScoresData = data["olqScores"] as? Map<String, Map<String, Any>> ?: emptyMap()
        val olqScores = olqScoresData.mapNotNull { (olqName, scoreData) ->
            val olq = OLQ.entries.find { it.name == olqName } ?: return@mapNotNull null
            olq to mapToOLQScore(scoreData)
        }.toMap()

        return InterviewResponse(
            id = data["id"] as String,
            sessionId = data["sessionId"] as String,
            questionId = data["questionId"] as String,
            responseText = data["responseText"] as String,
            responseMode = InterviewMode.valueOf(data["responseMode"] as String),
            respondedAt = Instant.ofEpochMilli(data["respondedAt"] as Long),
            thinkingTimeSec = (data["thinkingTimeSec"] as Long).toInt(),
            audioUrl = data["audioUrl"] as? String,
            olqScores = olqScores,
            confidenceScore = (data["confidenceScore"] as? Long)?.toInt() ?: 0
        )
    }

    // ============================================
    // RESULT MAPPING
    // ============================================

    fun resultToMap(result: InterviewResult): Map<String, Any?> {
        return mapOf(
            "id" to result.id,
            "sessionId" to result.sessionId,
            "userId" to result.userId,
            "mode" to result.mode.name,
            "completedAt" to result.completedAt.toEpochMilli(),
            "durationSec" to result.durationSec,
            "totalQuestions" to result.totalQuestions,
            "totalResponses" to result.totalResponses,
            "overallOLQScores" to result.overallOLQScores.mapKeys { it.key.name }.mapValues { olqScoreToMap(it.value) },
            "categoryScores" to result.categoryScores.mapKeys { it.key.name },
            "overallConfidence" to result.overallConfidence,
            "strengths" to result.strengths.map { it.name },
            "weaknesses" to result.weaknesses.map { it.name },
            "feedback" to result.feedback,
            "overallRating" to result.overallRating
        )
    }

    @Suppress("UNCHECKED_CAST")
    fun mapToResult(data: Map<String, Any>): InterviewResult {
        val overallOLQScoresData = data["overallOLQScores"] as? Map<String, Map<String, Any>> ?: emptyMap()
        val overallOLQScores = overallOLQScoresData.mapNotNull { (olqName, scoreData) ->
            val olq = OLQ.entries.find { it.name == olqName } ?: return@mapNotNull null
            olq to mapToOLQScore(scoreData)
        }.toMap()

        val categoryScoresData = data["categoryScores"] as? Map<String, Number> ?: emptyMap()
        val categoryScores = categoryScoresData.mapNotNull { (categoryName, score) ->
            val category = OLQCategory.entries.find { it.name == categoryName } ?: return@mapNotNull null
            category to score.toFloat()
        }.toMap()

        val strengthNames = data["strengths"] as? List<String> ?: emptyList()
        val strengths = strengthNames.mapNotNull { name -> OLQ.entries.find { it.name == name } }

        val weaknessNames = data["weaknesses"] as? List<String> ?: emptyList()
        val weaknesses = weaknessNames.mapNotNull { name -> OLQ.entries.find { it.name == name } }

        return InterviewResult(
            id = data["id"] as String,
            sessionId = data["sessionId"] as String,
            userId = data["userId"] as String,
            mode = InterviewMode.valueOf(data["mode"] as String),
            completedAt = Instant.ofEpochMilli(data["completedAt"] as Long),
            durationSec = data["durationSec"] as Long,
            totalQuestions = (data["totalQuestions"] as Long).toInt(),
            totalResponses = (data["totalResponses"] as Long).toInt(),
            overallOLQScores = overallOLQScores,
            categoryScores = categoryScores,
            overallConfidence = (data["overallConfidence"] as Long).toInt(),
            strengths = strengths,
            weaknesses = weaknesses,
            feedback = data["feedback"] as String,
            overallRating = (data["overallRating"] as Long).toInt()
        )
    }

    // ============================================
    // OLQ SCORE MAPPING
    // ============================================

    private fun olqScoreToMap(score: OLQScore): Map<String, Any> {
        return mapOf(
            "score" to score.score,
            "confidence" to score.confidence,
            "reasoning" to score.reasoning
        )
    }

    private fun mapToOLQScore(data: Map<String, Any>): OLQScore {
        return OLQScore(
            score = (data["score"] as Long).toInt(),
            confidence = (data["confidence"] as Long).toInt(),
            reasoning = data["reasoning"] as String
        )
    }
}



















