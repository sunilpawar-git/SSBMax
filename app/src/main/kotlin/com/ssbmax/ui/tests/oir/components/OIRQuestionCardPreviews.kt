package com.ssbmax.ui.tests.oir.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.ssbmax.core.domain.model.OIROption
import com.ssbmax.core.domain.model.OIRQuestion
import com.ssbmax.core.domain.model.OIRQuestionType
import com.ssbmax.core.domain.model.QuestionDifficulty

@Preview(showBackground = true, name = "Multi-Select — 1 of 2 selected")
@Composable
internal fun OIRQuestionCardMultiSelectPreview() {
    MaterialTheme {
        OIRQuestionView(
            question = multiSelectQuestion,
            selectedOptionIds = setOf("opt_b"),
            onOptionSelected = {},
            showFeedback = false,
            isCorrect = false
        )
    }
}

@Preview(showBackground = true, name = "Multi-Select — 2 of 2 selected")
@Composable
internal fun OIRQuestionCardMultiSelectBothSelectedPreview() {
    MaterialTheme {
        OIRQuestionView(
            question = multiSelectQuestion,
            selectedOptionIds = setOf("opt_b", "opt_c"),
            onOptionSelected = {},
            showFeedback = false,
            isCorrect = false
        )
    }
}

@Preview(showBackground = true, name = "Multi-Select — feedback correct")
@Composable
internal fun OIRQuestionCardMultiSelectFeedbackPreview() {
    MaterialTheme {
        OIRQuestionView(
            question = multiSelectQuestion,
            selectedOptionIds = setOf("opt_b", "opt_c"),
            onOptionSelected = {},
            showFeedback = true,
            isCorrect = true
        )
    }
}

@Preview(showBackground = true, name = "Single-Select — option selected")
@Composable
internal fun OIRQuestionCardSingleSelectPreview() {
    MaterialTheme {
        OIRQuestionView(
            question = singleSelectQuestion,
            selectedOptionIds = setOf("opt_a"),
            onOptionSelected = {},
            showFeedback = false,
            isCorrect = false
        )
    }
}

private val options = listOf(
    OIROption("opt_a", "Figure 1"),
    OIROption("opt_b", "Figure 2"),
    OIROption("opt_c", "Figure 3"),
    OIROption("opt_d", "Figure 4"),
    OIROption("opt_e", "Figure 5"),
)

private val multiSelectQuestion = OIRQuestion(
    id = "preview_q1",
    questionNumber = 1,
    type = OIRQuestionType.NON_VERBAL_REASONING,
    questionText = "Which two of the following figures belong to Class A?",
    options = options,
    correctAnswerId = "",
    correctAnswerIds = listOf("opt_b", "opt_c"),
    explanation = "Figures 2 and 3 share the defining property of Class A.",
    difficulty = QuestionDifficulty.MEDIUM
)

private val singleSelectQuestion = OIRQuestion(
    id = "preview_q2",
    questionNumber = 2,
    type = OIRQuestionType.VERBAL_REASONING,
    questionText = "Which word is the odd one out?",
    options = listOf(
        OIROption("opt_a", "Apple"), OIROption("opt_b", "Banana"),
        OIROption("opt_c", "Carrot"), OIROption("opt_d", "Mango")
    ),
    correctAnswerId = "opt_c",
    explanation = "Carrot is a vegetable; the rest are fruits.",
    difficulty = QuestionDifficulty.EASY
)
