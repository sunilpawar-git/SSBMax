package com.ssbmax.detekt

import io.gitlab.arturbosch.detekt.test.compileAndLint
import kotlin.test.Test
import kotlin.test.assertEquals

class MissingComposePreviewRuleTest {

    private val rule = MissingComposePreviewRule()

    @Test
    fun `flags a public Composable in ui components without a Preview`() {
        val findings = rule.compileAndLint(
            """
            package com.ssbmax.shared.ui.components

            @Composable
            fun SubscriptionBadge(tier: String) {
            }
            """.trimIndent()
        )
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not flag when a matching Preview function exists`() {
        val findings = rule.compileAndLint(
            """
            package com.ssbmax.shared.ui.components

            @Composable
            fun SubscriptionBadge(tier: String) {
            }

            @Preview
            @Composable
            private fun SubscriptionBadgePreview() {
                SubscriptionBadge(tier = "Premium")
            }
            """.trimIndent()
        )
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag private Composables`() {
        val findings = rule.compileAndLint(
            """
            package com.ssbmax.shared.ui.components

            @Composable
            private fun DetailRow(label: String, value: String) {
            }
            """.trimIndent()
        )
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag internal Composables`() {
        val findings = rule.compileAndLint(
            """
            package com.ssbmax.shared.ui.components.drawer

            @Composable
            internal fun DrawerSectionHeader(title: String) {
            }
            """.trimIndent()
        )
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag Composables outside ui components`() {
        val findings = rule.compileAndLint(
            """
            package com.ssbmax.shared.ui.screens.oir

            @Composable
            fun OIRTestScreen(testId: String) {
            }
            """.trimIndent()
        )
        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag non-Composable functions`() {
        val findings = rule.compileAndLint(
            """
            package com.ssbmax.shared.ui.components

            fun formatScore(score: Float): String = score.toString()
            """.trimIndent()
        )
        assertEquals(0, findings.size)
    }
}
