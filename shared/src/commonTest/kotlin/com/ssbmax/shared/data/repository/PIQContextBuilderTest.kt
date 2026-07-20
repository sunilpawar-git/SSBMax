package com.ssbmax.shared.data.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves [PIQContextBuilder.buildComprehensivePIQContext] (KMP port of the Android original's
 * `PIQDataMapper`) actually reflects submitted PIQ fields into the AI-facing context string,
 * with at least one field from each of the three section files
 * ([PIQContextPersonalSections]/[PIQContextLifeSections]/[PIQContextFinalSections]) represented,
 * rather than exhaustively covering all ~60 fields.
 */
class PIQContextBuilderTest {

    @Test
    fun `buildComprehensivePIQContext reflects personal, career, and self-assessment fields`() {
        val piqMap: Map<String, Any> = mapOf(
            "data" to mapOf(
                "fullName" to "Cadet Sharma",
                "age" to "22",
                "gender" to "Male",
                "state" to "Punjab",
                "presentOccupation" to "Software Engineer",
                "hobbies" to "Reading, Trekking",
                "whyDefenseForces" to "To serve the nation",
                "strengths" to "Leadership, Discipline",
                "weaknesses" to "Impatience"
            )
        )

        val context = PIQContextBuilder.buildComprehensivePIQContext(piqMap)

        assertFalse(context.startsWith("Error processing PIQ"))
        assertTrue(context.contains("Cadet Sharma"))
        assertTrue(context.contains("Software Engineer"))
        assertTrue(context.contains("To serve the nation"))
        assertTrue(context.contains("Leadership, Discipline"))
    }

    @Test
    fun `buildComprehensivePIQContext does not error on a mostly-empty submission`() {
        val context = PIQContextBuilder.buildComprehensivePIQContext(emptyMap())

        assertFalse(context.startsWith("Error processing PIQ"))
        assertTrue(context.contains("CANDIDATE PROFILE"))
    }

    @Test
    fun `buildComprehensivePIQContext falls back to the top-level map when there is no nested data field`() {
        val piqMap: Map<String, Any> = mapOf("fullName" to "Direct Field Candidate")

        val context = PIQContextBuilder.buildComprehensivePIQContext(piqMap)

        assertTrue(context.contains("Direct Field Candidate"))
    }
}
