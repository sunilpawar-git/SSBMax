package com.ssbmax.shared.domain.validation

import com.ssbmax.shared.domain.model.OIROption
import com.ssbmax.shared.domain.model.OIRQuestion
import com.ssbmax.shared.domain.model.OIRQuestionType
import com.ssbmax.shared.domain.model.QuestionDifficulty
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class OIRQuestionValidatorTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun textOption(id: String, text: String) =
        OIROption(id = id, text = text, imageUrl = null)

    private fun imageOption(id: String, imageUrl: String, text: String = "") =
        OIROption(id = id, text = text, imageUrl = imageUrl)

    private fun verbalQuestion(options: List<OIROption> = fourTextOptions()) = OIRQuestion(
        id = "oir_q_0001",
        questionNumber = 1,
        type = OIRQuestionType.VERBAL_REASONING,
        questionText = "Which word is the odd one out?",
        options = options,
        correctAnswerId = "opt_b",
        explanation = "Explanation here.",
        difficulty = QuestionDifficulty.MEDIUM,
        timeSeconds = 60
    )

    private fun nonVerbalQuestion(options: List<OIROption> = fourImageOptions()) = OIRQuestion(
        id = "oir_q_0002",
        questionNumber = 2,
        type = OIRQuestionType.NON_VERBAL_REASONING,
        questionText = "Which figure completes the series?",
        options = options,
        correctAnswerId = "opt_c",
        explanation = "Pattern follows rotation by 90°.",
        difficulty = QuestionDifficulty.MEDIUM,
        timeSeconds = 60,
        questionImageUrl = "https://storage.googleapis.com/bucket/oir/s02_q01.png"
    )

    private fun fourTextOptions() = listOf(
        textOption("opt_a", "Apple"),
        textOption("opt_b", "Banana"),
        textOption("opt_c", "Chair"),
        textOption("opt_d", "Mango")
    )

    private fun fourImageOptions() = listOf(
        imageOption("opt_a", "https://storage.googleapis.com/bucket/oir/s02_q01_a.png"),
        imageOption("opt_b", "https://storage.googleapis.com/bucket/oir/s02_q01_b.png"),
        imageOption("opt_c", "https://storage.googleapis.com/bucket/oir/s02_q01_c.png"),
        imageOption("opt_d", "https://storage.googleapis.com/bucket/oir/s02_q01_d.png")
    )

    // -------------------------------------------------------------------------
    // Image-option questions (the regression that caused 42 instead of 50)
    // -------------------------------------------------------------------------

    @Test
    fun `image-only option with blank text is valid`() {
        val result = OIRQuestionValidator.validate(nonVerbalQuestion())
        assertTrue(result.isValid, "Non-verbal question with image options and blank text should be valid. Errors: ${result.errors}")
    }

    @Test
    fun `image option with blank text does not add an error`() {
        val result = OIRQuestionValidator.validate(nonVerbalQuestion())
        assertFalse(result.errors.any { "empty text" in it || "has no image" in it }, "Expected no 'empty text' error for image option, but got: ${result.errors}")
    }

    @Test
    fun `validateAndFilter keeps image-option questions`() {
        val questions = listOf(verbalQuestion(), nonVerbalQuestion())
        val valid = OIRQuestionValidator.validateAndFilter(questions)
        assertEquals(2, valid.size, "Both text-option and image-option questions should survive filtering")
    }

    @Test
    fun `validateAndFilter does not silently drop non-verbal questions`() {
        val nonVerbals = (1..20).map { i ->
            nonVerbalQuestion().copy(
                id = "oir_q_${i.toString().padStart(4, '0')}",
                questionNumber = i,
                correctAnswerId = "opt_c"
            )
        }
        val valid = OIRQuestionValidator.validateAndFilter(nonVerbals)
        assertEquals(20, valid.size, "All 20 non-verbal image-option questions must pass validation")
    }

    // -------------------------------------------------------------------------
    // Guard: text-only option with no text and no image is still an error
    // -------------------------------------------------------------------------

    @Test
    fun `text option with blank text and no image is invalid`() {
        val badOptions = listOf(
            textOption("opt_a", "Apple"),
            textOption("opt_b", ""),   // blank, no imageUrl
            textOption("opt_c", "Chair"),
            textOption("opt_d", "Mango")
        )
        val result = OIRQuestionValidator.validate(verbalQuestion(badOptions))
        assertFalse(result.isValid, "Question with a blank text-only option should be invalid")
        assertTrue(result.errors.any { "empty text" in it || "no image" in it })
    }

    // -------------------------------------------------------------------------
    // Standard verbal question passes unchanged
    // -------------------------------------------------------------------------

    @Test
    fun `valid verbal question with text options passes`() {
        val result = OIRQuestionValidator.validate(verbalQuestion())
        assertTrue(result.isValid, "Standard verbal question should be valid. Errors: ${result.errors}")
    }

    // -------------------------------------------------------------------------
    // Existing corruption checks still fire
    // -------------------------------------------------------------------------

    @Test
    fun `single-letter correctAnswerId is still an error`() {
        val q = verbalQuestion().copy(correctAnswerId = "b")
        val result = OIRQuestionValidator.validate(q)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "single letter" in it })
    }

    @Test
    fun `embedded-number correctAnswerId is still an error`() {
        val q = verbalQuestion().copy(correctAnswerId = "opt_103_b")
        val result = OIRQuestionValidator.validate(q)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "question number embedded" in it })
    }

    @Test
    fun `correctAnswerId not matching any option is still an error`() {
        val q = verbalQuestion().copy(correctAnswerId = "opt_e")
        val result = OIRQuestionValidator.validate(q)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "does not match any option" in it })
    }

    // -------------------------------------------------------------------------
    // Phase 3: multi-select validation (correctAnswerIds-based, no image-URL workaround)
    // -------------------------------------------------------------------------

    private fun fiveImageOptions() = listOf(
        imageOption("opt_a", "https://storage.googleapis.com/bucket/oir/fig_a.png"),
        imageOption("opt_b", "https://storage.googleapis.com/bucket/oir/fig_b.png"),
        imageOption("opt_c", "https://storage.googleapis.com/bucket/oir/fig_c.png"),
        imageOption("opt_d", "https://storage.googleapis.com/bucket/oir/fig_d.png"),
        imageOption("opt_e", "https://storage.googleapis.com/bucket/oir/fig_e.png")
    )

    private fun multiSelectQuestion() = OIRQuestion(
        id = "oir_q_0101",
        questionNumber = 101,
        type = OIRQuestionType.NON_VERBAL_REASONING,
        questionText = "Which two of the following figures belong to Class A?",
        options = fiveImageOptions(),
        correctAnswerId = "",
        correctAnswerIds = listOf("opt_b", "opt_c"),
        explanation = "Answer: 2 and 3.",
        difficulty = QuestionDifficulty.MEDIUM,
        timeSeconds = 60,
        questionImageUrl = "https://storage.googleapis.com/bucket/oir/class_a.png"
    )

    @Test
    fun `valid multi-select question with two correctAnswerIds passes validation`() {
        val result = OIRQuestionValidator.validate(multiSelectQuestion())
        assertTrue(result.isValid, "Multi-select question with valid correctAnswerIds should pass. Errors: ${result.errors}")
    }

    @Test
    fun `multi-select question with unknown option id in correctAnswerIds fails`() {
        // correctAnswerIds references opt_z which doesn't exist in options
        val q = multiSelectQuestion().copy(correctAnswerIds = listOf("opt_b", "opt_z"))
        val result = OIRQuestionValidator.validate(q)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "unknown option" in it })
    }

    @Test
    fun `multi-select question with only one correctAnswerIds entry fails`() {
        // size > 1 triggers multi-select path, but correctAnswerIds has only 1 entry — invalid
        val q = multiSelectQuestion().copy(correctAnswerIds = listOf("opt_b"))
        val result = OIRQuestionValidator.validate(q)
        // correctAnswerIds.size == 1 → isMultiSelect = false → blank correctAnswerId → error
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "correctAnswerId is empty and no correctAnswerIds provided" in it })
    }

    @Test
    fun `single-answer question with blank correctAnswerId and no correctAnswerIds fails`() {
        // No image-URL workaround: blank correctAnswerId with empty correctAnswerIds is always an error
        val q = verbalQuestion().copy(correctAnswerId = "", correctAnswerIds = emptyList())
        val result = OIRQuestionValidator.validate(q)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { "correctAnswerId is empty and no correctAnswerIds provided" in it })
    }

    @Test
    fun `single-answer question with blank correctAnswerId but questionImageUrl now fails`() {
        // Documents intent: the old image-URL workaround is gone.
        // A question with a figure but no correctAnswerId and no correctAnswerIds must fail validation.
        val q = nonVerbalQuestion().copy(correctAnswerId = "", correctAnswerIds = emptyList())
        val result = OIRQuestionValidator.validate(q)
        assertFalse(result.isValid, "Image-URL workaround must be gone: blank correctAnswerId + no correctAnswerIds should fail even with a questionImageUrl")
        assertTrue(result.errors.any { "correctAnswerId is empty and no correctAnswerIds provided" in it })
    }
}
