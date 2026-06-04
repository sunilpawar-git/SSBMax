package com.ssbmax.core.domain.usecase.oir

import com.ssbmax.core.domain.model.*
import com.ssbmax.core.domain.util.DomainLogger
import javax.inject.Inject

private const val TAG = "OIRScoreCalculator"

/**
 * Calculates the final [OIRTestResult] from a completed [OIRTestSession].
 *
 * Lives in the domain layer — zero Android dependencies.
 * Injected via Hilt so it can be tested independently.
 */
class OIRTestScoreCalculator @Inject constructor(
    private val logger: DomainLogger
) {

    fun calculate(session: OIRTestSession): OIRTestResult {
        val correctAnswers   = session.answers.values.count { it.isCorrect }
        val incorrectAnswers = session.answers.values.count { !it.isCorrect && !it.skipped }
        val skippedQuestions = session.questions.size - session.answers.size

        val rawScore = session.answers.values.filter { it.isCorrect }.sumOf { answer ->
            session.questions.find { it.id == answer.questionId }?.difficulty?.points ?: 1
        }
        val maxScore        = session.questions.sumOf { it.difficulty.points }
        val percentageScore = if (maxScore > 0) (rawScore.toFloat() / maxScore) * 100 else 0f

        val categoryScores = OIRQuestionType.values().associateWith { type ->
            val catQs      = session.questions.filter { it.type == type }
            val catAnswers = catQs.mapNotNull { q -> session.answers[q.id] }
            val correct    = catAnswers.count { it.isCorrect }
            val avgTime    = if (catAnswers.isNotEmpty()) catAnswers.map { it.timeTakenSeconds }.average().toInt() else 0
            CategoryScore(
                category            = type,
                totalQuestions      = catQs.size,
                correctAnswers      = correct,
                percentage          = if (catQs.isNotEmpty()) (correct.toFloat() / catQs.size) * 100 else 0f,
                averageTimeSeconds  = avgTime
            )
        }

        val difficultyScores = QuestionDifficulty.values().associateWith { diff ->
            val diffQs      = session.questions.filter { it.difficulty == diff }
            val diffAnswers = diffQs.mapNotNull { q -> session.answers[q.id] }
            val correct     = diffAnswers.count { it.isCorrect }
            DifficultyScore(
                difficulty     = diff,
                totalQuestions = diffQs.size,
                correctAnswers = correct,
                percentage     = if (diffQs.isNotEmpty()) (correct.toFloat() / diffQs.size) * 100 else 0f
            )
        }

        logger.d(TAG, "📋 Building answered-questions list (${session.questions.size} questions)")
        val answeredQuestions = session.questions.mapNotNull { question ->
            val answer = session.answers[question.id] ?: return@mapNotNull null
            val correctOption = question.options.find { it.id == question.correctAnswerId }
            if (correctOption == null) {
                logger.e(
                    TAG,
                    "OIR scoring: correctAnswerId not found in options for ${question.id}",
                    Exception("Invalid correctAnswerId for ${question.id}")
                )
                return@mapNotNull null
            }
            OIRAnsweredQuestion(
                question       = question,
                userAnswer     = answer,
                isCorrect      = answer.isCorrect,
                correctOption  = correctOption,
                selectedOption = answer.selectedOptionId?.let { id -> question.options.find { it.id == id } }
            )
        }
        logger.d(TAG, "✅ ${answeredQuestions.size} answered questions built")

        return OIRTestResult(
            testId              = session.testId,
            sessionId           = session.sessionId,
            userId              = session.userId,
            totalQuestions      = session.questions.size,
            correctAnswers      = correctAnswers,
            incorrectAnswers    = incorrectAnswers,
            skippedQuestions    = skippedQuestions,
            totalTimeSeconds    = OIRTestConfig().totalTimeMinutes * 60,
            timeTakenSeconds    = ((System.currentTimeMillis() - session.startTime) / 1000).toInt(),
            rawScore            = rawScore,
            percentageScore     = percentageScore,
            categoryScores      = categoryScores,
            difficultyBreakdown = difficultyScores,
            answeredQuestions   = answeredQuestions,
            completedAt         = System.currentTimeMillis()
        )
    }
}
