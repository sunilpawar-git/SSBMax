package com.ssbmax.shared.domain.model

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class OIRTestModelTest {

    private fun minimalQuestion(correctAnswerIds: List<String> = emptyList()) = OIRQuestion(
        id = "q1",
        questionNumber = 1,
        type = OIRQuestionType.NON_VERBAL_REASONING,
        questionText = "Which two figures belong to Class A?",
        options = listOf(
            OIROption("opt_a", "1"),
            OIROption("opt_b", "2"),
            OIROption("opt_c", "3"),
            OIROption("opt_d", "4"),
            OIROption("opt_e", "5"),
        ),
        correctAnswerId = "",
        explanation = "Answer: 2 and 3.",
        difficulty = QuestionDifficulty.MEDIUM,
        correctAnswerIds = correctAnswerIds,
    )

    @Test
    fun isMultiSelect_trueWhenCorrectAnswerIdsHasTwoEntries() {
        val question = minimalQuestion(listOf("opt_b", "opt_c"))
        assertTrue(question.isMultiSelect)
    }

    @Test
    fun isMultiSelect_falseWhenCorrectAnswerIdsIsEmpty() {
        val question = minimalQuestion(emptyList())
        assertFalse(question.isMultiSelect)
    }

    @Test
    fun isMultiSelect_falseWhenCorrectAnswerIdsHasOneEntry() {
        val question = minimalQuestion(listOf("opt_a"))
        assertFalse(question.isMultiSelect)
    }

    @Test
    fun oirAnswer_selectedOptionIdsDefaultsToEmptySet() {
        val answer = OIRAnswer(questionId = "q1", selectedOptionId = null)
        assertTrue(answer.selectedOptionIds.isEmpty())
    }

    @Test
    fun oirAnsweredQuestion_correctOptionsAndSelectedOptionsDefaultToEmptyList() {
        val option = OIROption("opt_a", "1")
        val question = minimalQuestion()
        val answer = OIRAnswer(questionId = "q1", selectedOptionId = null)
        val answered = OIRAnsweredQuestion(
            question = question,
            userAnswer = answer,
            isCorrect = false,
            correctOption = option,
            selectedOption = null,
        )
        assertTrue(answered.correctOptions.isEmpty())
        assertTrue(answered.selectedOptions.isEmpty())
    }
}
