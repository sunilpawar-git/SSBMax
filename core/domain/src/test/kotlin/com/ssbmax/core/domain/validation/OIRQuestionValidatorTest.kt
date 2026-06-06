package com.ssbmax.core.domain.validation

import com.ssbmax.core.domain.model.OIROption
import com.ssbmax.core.domain.model.OIRQuestion
import com.ssbmax.core.domain.model.OIRQuestionType
import com.ssbmax.core.domain.model.QuestionDifficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertTrue(
            "Non-verbal question with image options and blank text should be valid. Errors: ${result.errors}",
            result.isValid
        )
    }

    @Test
    fun `image option with blank text does not add an error`() {
        val result = OIRQuestionValidator.validate(nonVerbalQuestion())
        assertFalse(
            "Expected no 'empty text' error for image option, but got: ${result.errors}",
            result.errors.any { "empty text" in it || "has no image" in it }
        )
    }

    @Test
    fun `validateAndFilter keeps image-option questions`() {
        val questions = listOf(verbalQuestion(), nonVerbalQuestion())
        val valid = OIRQuestionValidator.validateAndFilter(questions)
        assertEquals("Both text-option and image-option questions should survive filtering", 2, valid.size)
    }

    @Test
    fun `validateAndFilter does not silently drop non-verbal questions`() {
        val nonVerbals = (1..20).map { i ->
            nonVerbalQuestion().copy(
                id = "oir_q_%04d".format(i),
                questionNumber = i,
                correctAnswerId = "opt_c"
            )
        }
        val valid = OIRQuestionValidator.validateAndFilter(nonVerbals)
        assertEquals("All 20 non-verbal image-option questions must pass validation", 20, valid.size)
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
        assertFalse("Question with a blank text-only option should be invalid", result.isValid)
        assertTrue(result.errors.any { "empty text" in it || "no image" in it })
    }

    // -------------------------------------------------------------------------
    // Standard verbal question passes unchanged
    // -------------------------------------------------------------------------

    @Test
    fun `valid verbal question with text options passes`() {
        val result = OIRQuestionValidator.validate(verbalQuestion())
        assertTrue("Standard verbal question should be valid. Errors: ${result.errors}", result.isValid)
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
}
