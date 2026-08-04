package com.ssbmax.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * `shared/ui/components`'s equivalent of the old `:lint` `ComponentMissingPreviewDetector`,
 * deleted in the KMP-convergence plan's Phase 0f along with `core:designsystem`. AGP Lint
 * never analyzed `shared`'s commonMain (see [HardcodedComposeTextRule]'s doc), so this rule
 * is scoped by package rather than file path -- `compileAndLint` in tests never gives a real
 * file path, but the `package` declaration in the compiled source is preserved either way.
 *
 * Only checks public top-level Composables: `private`/`internal` functions are implementation
 * details of a reusable component, not the reusable surface itself, so they're exempt --
 * mirrors the old detector only firing on designsystem's public API.
 */
class MissingComposePreviewRule(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        id = "MissingComposePreview",
        severity = Severity.Warning,
        description = "Reusable Composable in shared/ui/components should have a @Preview " +
            "for visual validation and documentation.",
        debt = Debt.FIVE_MINS
    )

    private val targetPackagePrefix = "com.ssbmax.shared.ui.components"

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        val ktFile = function.containingKtFile
        if (!ktFile.packageFqName.asString().startsWith(targetPackagePrefix)) return
        if (!function.isPublicComposable()) return

        val componentName = function.name ?: return
        if (ktFile.hasPreviewFor(componentName)) return

        report(
            CodeSmell(
                issue,
                Entity.from(function),
                "Composable '$componentName' in shared/ui/components should have a @Preview. " +
                    "Add: @Preview @Composable private fun ${componentName}Preview() { " +
                    "SSBMaxTheme { $componentName(...) } }"
            )
        )
    }

    private fun KtNamedFunction.isPublicComposable(): Boolean {
        if (hasModifier(KtTokens.PRIVATE_KEYWORD) || hasModifier(KtTokens.INTERNAL_KEYWORD)) return false
        if (annotationEntries.none { it.shortName?.asString() == "Composable" }) return false
        val name = name ?: return false
        return name.firstOrNull()?.isUpperCase() == true
    }

    /** Mirrors the old detector's naming convention: `<Name>Preview` / `<Name>LightPreview`. */
    private fun KtFile.hasPreviewFor(componentName: String): Boolean {
        val content = text
        if (!content.contains("@Preview")) return false
        return content.contains("${componentName}Preview") ||
            content.contains("${componentName}LightPreview") ||
            content.contains("${componentName}DarkPreview")
    }
}
