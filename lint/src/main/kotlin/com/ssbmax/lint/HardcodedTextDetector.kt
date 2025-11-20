package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.getParameterForArgument

/**
 * Custom Lint Detector for SSBMax
 *
 * Detects hardcoded strings in Jetpack Compose Text() composables and suggests
 * using stringResource() instead for proper internationalization and centralized
 * string management.
 *
 * Example violation:
 * ```kotlin
 * Text("Hardcoded string")  // ERROR
 * ```
 *
 * Suggested fix:
 * ```kotlin
 * Text(stringResource(R.string.my_text))  // CORRECT
 * ```
 */
class HardcodedTextDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ISSUE_ID = "ComposeHardcodedText"
        private const val ISSUE_DESCRIPTION = "Hardcoded text in Compose Text composable"
        private const val ISSUE_EXPLANATION = """
            Hardcoded strings in Compose Text composables should be replaced with \
            stringResource() calls for proper internationalization and centralized string management.

            Instead of:
                Text("Hardcoded text")

            Use:
                Text(stringResource(R.string.my_text))
        """

        val ISSUE: Issue = Issue.create(
            id = ISSUE_ID,
            briefDescription = ISSUE_DESCRIPTION,
            explanation = ISSUE_EXPLANATION,
            category = Category.I18N,
            priority = 8,
            severity = Severity.ERROR,
            implementation = Implementation(
                HardcodedTextDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                // Check if this is a Text() composable call
                if (node.methodName != "Text") return

                // Get the first argument (text parameter)
                val textArgument = node.valueArguments.firstOrNull() ?: return

                // Check if it's a hardcoded string literal
                if (textArgument is ULiteralExpression) {
                    val value = textArgument.value

                    // Only flag actual string literals
                    if (value is String) {
                        // Ignore certain exceptions:
                        // 1. Empty strings
                        // 2. Single characters (often used for bullets, separators)
                        // 3. Whitespace-only strings
                        // 4. Format specifiers
                        if (value.isEmpty() ||
                            value.length == 1 ||
                            value.isBlank() ||
                            value.matches(Regex("""%\d*[ds]"""))
                        ) {
                            return
                        }

                        reportViolation(context, node, textArgument, value)
                    }
                }
            }
        }
    }

    private fun reportViolation(
        context: JavaContext,
        node: UCallExpression,
        textArgument: ULiteralExpression,
        hardcodedText: String
    ) {
        val location = context.getLocation(textArgument)
        val message = "Hardcoded text \"$hardcodedText\" should use stringResource()"

        context.report(
            issue = ISSUE,
            location = location,
            message = message,
            quickfixData = createQuickFix(hardcodedText)
        )
    }

    private fun createQuickFix(hardcodedText: String): LintFix {
        // Generate a suggested resource name from the hardcoded text
        val suggestedResourceName = generateResourceName(hardcodedText)

        return LintFix.create()
            .name("Replace with stringResource()")
            .replace()
            .text("\"$hardcodedText\"")
            .with("stringResource(R.string.$suggestedResourceName)")
            .build()
    }

    /**
     * Generates a suggested string resource name from hardcoded text
     * Example: "Submit Test" -> "submit_test"
     */
    private fun generateResourceName(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .take(50) // Limit length
            .ifEmpty { "text" }
    }
}
