package com.ssbmax.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * commonMain equivalent of `:lint`'s `HardcodedTextDetector` (`ComposeHardcodedText`).
 *
 * AGP's Android Lint only analyzes `androidMain` on a Kotlin Multiplatform android-library
 * module (verified empirically against `shared` during the Phase 0 KMP-convergence plan's 0h
 * spike: adding `lintChecks(project(":lint"))` to `shared/build.gradle.kts` reported zero
 * findings against known `commonMain` violations such as `Text("Phase 1 - Screening")`).
 * Detekt, unlike Lint, already runs over every KMP source set including `commonMain`, so it is
 * the only enforcement point available for `shared`'s UI code — see the plan's Phase 0c/0h.
 */
class HardcodedComposeTextRule(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        id = "HardcodedComposeText",
        severity = Severity.Defect,
        description = "Hardcoded text in a Compose Text() call should use Res.string.* instead.",
        debt = Debt.FIVE_MINS
    )

    private val formatSpecifier = Regex("""%\d*[ds]""")

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (expression.calleeExpression?.text != "Text") return

        val firstArgument = expression.valueArguments.firstOrNull() ?: return
        val literal = firstArgument.asPlainStringLiteral() ?: return

        if (isExempt(literal)) return

        report(
            CodeSmell(
                issue,
                Entity.from(firstArgument),
                "Hardcoded text \"$literal\" should use Res.string.* (commonMain has no " +
                    "stringResource() equivalent — see Compose Resources)."
            )
        )
    }

    /** Mirrors `HardcodedTextDetector`'s exemptions: empty/blank/single-char/format-specifier. */
    private fun isExempt(literal: String): Boolean {
        if (literal.isEmpty() || literal.length == 1 || literal.isBlank()) return true
        return formatSpecifier.matches(literal)
    }

    /**
     * Returns the literal text only for a plain string with no `${...}` interpolation —
     * mirrors `HardcodedTextDetector`'s `ULiteralExpression` check, which likewise never
     * fires on interpolated strings.
     */
    private fun KtValueArgument.asPlainStringLiteral(): String? {
        val template = getArgumentExpression() as? KtStringTemplateExpression ?: return null
        val entries = template.entries
        if (entries.any { it !is KtLiteralStringTemplateEntry }) return null
        return entries.joinToString(separator = "") { it.text }
    }
}
