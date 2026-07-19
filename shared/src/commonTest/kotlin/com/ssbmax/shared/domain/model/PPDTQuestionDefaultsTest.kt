package com.ssbmax.shared.domain.model

import kotlin.test.assertEquals
import kotlin.test.Test

class PPDTQuestionDefaultsTest {

    @Test
    fun `PPDTQuestion default minCharacters is 200 matching UI enforcement`() {
        val question = PPDTQuestion(id = "test", imageUrl = "url", imageDescription = "desc")
        // WHY: UI enforces 200; domain model must agree to prevent silent contract mismatch
        assertEquals(200, question.minCharacters)
    }

    @Test
    fun `PPDTQuestion default maxCharacters is 1500`() {
        val question = PPDTQuestion(id = "test", imageUrl = "url", imageDescription = "desc")
        assertEquals(1500, question.maxCharacters)
    }
}
