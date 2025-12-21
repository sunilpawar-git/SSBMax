package com.ssbmax.core.domain.constants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterviewConstantsTest {

    @Test
    fun ratios_sum_to_one() {
        val sum = InterviewConstants.PIQ_QUESTION_RATIO +
            InterviewConstants.GENERIC_QUESTION_RATIO +
            InterviewConstants.ADAPTIVE_QUESTION_RATIO
        assertEquals(1.0f, sum, 0.0001f)
    }

    @Test
    fun target_counts_align_with_ratios() {
        val total = InterviewConstants.TARGET_TOTAL_QUESTIONS
        val piq = InterviewConstants.TARGET_PIQ_QUESTION_COUNT
        val generic = InterviewConstants.TARGET_GENERIC_QUESTION_COUNT

        // Rounded to nearest whole question; allow small rounding delta
        assertEquals(total * InterviewConstants.PIQ_QUESTION_RATIO, piq.toFloat(), 1f)
        assertEquals(total * InterviewConstants.GENERIC_QUESTION_RATIO, generic.toFloat(), 1f)
        assertTrue(piq + generic <= total)
    }

    @Test
    fun fallback_scores_within_bounds() {
        assertTrue(InterviewConstants.FALLBACK_OLQ_SCORE in 1..10)
        assertTrue(InterviewConstants.FALLBACK_CONFIDENCE in 0..100)
    }
}











