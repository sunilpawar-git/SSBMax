package com.ssbmax.detekt

import io.gitlab.arturbosch.detekt.test.compileAndLint
import kotlin.test.Test
import kotlin.test.assertEquals

class HardcodedComposeTextRuleTest {

    private val rule = HardcodedComposeTextRule()

    @Test
    fun `flags a plain string literal passed to Text`() {
        val findings = rule.compileAndLint(
            """
            fun screen() {
                Text("Phase 1 - Screening")
            }
            """.trimIndent()
        )
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not flag stringResource usage`() {
        val findings = rule.compileAndLint(
            """
            fun screen() {
                Text(stringResource(Res.string.phase_1_screening))
            }
            """.trimIndent()
        )
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag interpolated strings`() {
        val findings = rule.compileAndLint(
            """
            fun screen() {
                Text("${'$'}{uiState.count}")
            }
            """.trimIndent()
        )
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag single-character separators`() {
        val findings = rule.compileAndLint(
            """
            fun screen() {
                Text("•")
            }
            """.trimIndent()
        )
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag calls to unrelated functions named differently`() {
        val findings = rule.compileAndLint(
            """
            fun screen() {
                Label("Phase 1 - Screening")
            }
            """.trimIndent()
        )
        assertEquals(0, findings.size)
    }
}
