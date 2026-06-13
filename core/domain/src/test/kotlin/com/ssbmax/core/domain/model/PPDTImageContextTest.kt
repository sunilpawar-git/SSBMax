package com.ssbmax.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PPDTImageContextTest {

    @Test
    fun `PPDTImageContext defaults produce empty lists allowing safe no-context usage`() {
        // WHY: Backward compat — existing pictures without imageContext must not crash the app
        val context = PPDTImageContext()
        assertTrue(context.coreElements.isEmpty())
        assertTrue(context.ambiguousElements.isEmpty())
        assertTrue(context.expectedThemes.isEmpty())
        assertTrue(context.penalizedThemes.isEmpty())
        assertTrue(context.primaryOLQs.isEmpty())
        assertTrue(context.exemplarGoodHints.isEmpty())
        assertTrue(context.exemplarBadHints.isEmpty())
        assertEquals("", context.sceneDescription)
        assertEquals(DeviationTolerance.MEDIUM, context.deviationTolerance)
    }

    @Test
    fun `DeviationTolerance enum has exactly three levels`() {
        // WHY: Prompt injection logic in Phase 8 branches on exactly 3 values; adding one silently
        // would break the branching contract without a compile error
        assertEquals(3, DeviationTolerance.entries.size)
        assertNotNull(DeviationTolerance.valueOf("LOW"))
        assertNotNull(DeviationTolerance.valueOf("MEDIUM"))
        assertNotNull(DeviationTolerance.valueOf("HIGH"))
    }

    @Test
    fun `PPDTQuestion imageContext defaults to empty context for backward compat`() {
        // WHY: Old images cached without imageContext must still produce a valid PPDTQuestion
        // — null imageContext would crash Phase 8 prompt builder
        val question = PPDTQuestion(id = "x", imageUrl = "url", imageDescription = "desc")
        assertNotNull(question.imageContext)
        assertTrue(question.imageContext.coreElements.isEmpty())
        assertEquals(DeviationTolerance.MEDIUM, question.imageContext.deviationTolerance)
    }
}
