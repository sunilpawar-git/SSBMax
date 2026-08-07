package com.ssbmax.shared.ui.oir

import com.ssbmax.shared.domain.model.CategoryScore
import com.ssbmax.shared.domain.model.DifficultyScore
import com.ssbmax.shared.domain.model.OIRQuestionType
import com.ssbmax.shared.domain.model.QuestionDifficulty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OIRResultPresentationTest {

    @Test
    fun zeroQuestionCategoriesAreHiddenFromResultBreakdown() {
        val scores = OIRQuestionType.values().associateWith { type ->
            CategoryScore(type, if (type == OIRQuestionType.SPATIAL_REASONING) 0 else 10, 2, 20f, 1)
        }

        val visible = visibleOIRCategoryScores(scores)

        assertEquals(
            listOf(
                OIRQuestionType.VERBAL_REASONING,
                OIRQuestionType.NON_VERBAL_REASONING,
                OIRQuestionType.NUMERICAL_ABILITY
            ),
            visible.map { it.category }
        )
    }

    @Test
    fun difficultyBreakdownUsesEasyMediumHardOrder() {
        val scores = mapOf(
            QuestionDifficulty.HARD to difficultyScore(QuestionDifficulty.HARD, 10),
            QuestionDifficulty.EASY to difficultyScore(QuestionDifficulty.EASY, 15),
            QuestionDifficulty.MEDIUM to difficultyScore(QuestionDifficulty.MEDIUM, 25)
        )

        assertEquals(
            listOf(QuestionDifficulty.EASY, QuestionDifficulty.MEDIUM, QuestionDifficulty.HARD),
            orderedOIRDifficultyScores(scores).map { it.difficulty }
        )
    }

    @Test
    fun oneDifficultyDoesNotClaimToProvideBreakdown() {
        val scores = mapOf(
            QuestionDifficulty.EASY to difficultyScore(QuestionDifficulty.EASY, 0),
            QuestionDifficulty.MEDIUM to difficultyScore(QuestionDifficulty.MEDIUM, 50),
            QuestionDifficulty.HARD to difficultyScore(QuestionDifficulty.HARD, 0)
        )

        assertFalse(hasMeaningfulOIRDifficultyBreakdown(scores))
        assertEquals(listOf(QuestionDifficulty.MEDIUM), visibleOIRDifficultyScores(scores).map { it.difficulty })
    }

    @Test
    fun multipleDifficultiesProvideMeaningfulBreakdown() {
        val scores = mapOf(
            QuestionDifficulty.EASY to difficultyScore(QuestionDifficulty.EASY, 20),
            QuestionDifficulty.MEDIUM to difficultyScore(QuestionDifficulty.MEDIUM, 30),
            QuestionDifficulty.HARD to difficultyScore(QuestionDifficulty.HARD, 0)
        )

        assertTrue(hasMeaningfulOIRDifficultyBreakdown(scores))
    }

    private fun difficultyScore(difficulty: QuestionDifficulty, total: Int) = DifficultyScore(
        difficulty = difficulty,
        totalQuestions = total,
        correctAnswers = 0,
        percentage = 0f
    )
}
