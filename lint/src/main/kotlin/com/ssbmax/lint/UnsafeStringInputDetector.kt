package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UVariable

/**
 * Custom Lint Detector - Unsafe String Input
 *
 * Detects user input that is used without validation.
 * User input from TextFields, Intent extras, and API responses should be
 * validated (trim, isEmpty check, regex match) before use.
 *
 * Violations detected (WARNING severity):
 * - TextField.value used directly without trim/validation
 * - intent.getStringExtra() used directly
 * - remoteConfig.getString() used directly (untrusted external data)
 *
 * Example violation:
 * ```kotlin
 * val userInput = textFieldValue.text          // ⚠️ No validation
 * val questionAnswer = intent.getStringExtra("answer")  // ⚠️ No validation
 * ```
 *
 * Correct usage:
 * ```kotlin
 * // 1. Trim and check
 * val userInput = textFieldValue.text.trim()
 * if (userInput.isNotEmpty()) { useIt() }
 *
 * // 2. Regex validation
 * val answer = intent.getStringExtra("answer")?.takeIf { it.matches(Regex("\\d+")) }
 *
 * // 3. Explicit validation
 * val input = textFieldValue.text
 * require(input.isNotBlank() && input.length <= 100)
 * ```
 */
class UnsafeStringInputDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ISSUE_ID = "UnsafeStringInput"
        private const val ISSUE_DESCRIPTION = "User input used without validation"
        private const val ISSUE_EXPLANATION = """
            User-provided string input should always be validated before use to prevent:
            - XSS (Cross-Site Scripting) attacks
            - Injection vulnerabilities
            - Data corruption
            - Buffer overflows
            
            Sources of untrusted input:
            - TextFields, EditText values
            - Intent.getStringExtra()
            - API responses / RemoteConfig
            - SharedPreferences (user-settable)
            
            Minimum validation required:
            1. .trim() — remove whitespace
            2. .isNotEmpty() or .isNotBlank() — check non-empty
            3. .matches(Regex(...)) — validate format (emails, numbers, etc.)
            4. .length checks — prevent oversized inputs
            
            Examples:
            ✅ GOOD:
               val input = textField.text.trim()
               if (input.isNotEmpty()) { processInput(input) }
            
            ✅ GOOD:
               val number = intent.getStringExtra("num")?.takeIf { it.matches(Regex("\\d+")) }
            
            ❌ BAD:
               val input = textField.text  // No validation
               val email = remoteConfig.getString("email")  // No validation
        """

        val ISSUE: Issue = Issue.create(
            id = ISSUE_ID,
            briefDescription = ISSUE_DESCRIPTION,
            explanation = ISSUE_EXPLANATION,
            category = Category.SECURITY,
            priority = 8,
            severity = Severity.WARNING,
            implementation = Implementation(
                UnsafeStringInputDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    private val untrustedSources = listOf(
        "getStringExtra",
        "getString",
        "textFieldValue",
        "textValue",
        "text",
        "remoteConfig"
    )

    private val validationMethods = listOf(
        "trim",
        "isNotEmpty",
        "isNotBlank",
        "matches",
        "takeIf",
        "filter",
        "require",
        "check"
    )

    override fun getApplicableUastTypes() = listOf(UVariable::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitVariable(node: UVariable) {
                val initializer = node.uastInitializer ?: return
                val initStr = initializer.asSourceString()

                // Check if initializer uses untrusted source
                val hasUntrustedSource = untrustedSources.any { source ->
                    initStr.contains(source, ignoreCase = true)
                }

                if (!hasUntrustedSource) return

                // Check if it's followed by validation
                val hasValidation = validationMethods.any { validation ->
                    initStr.contains(validation, ignoreCase = true)
                }

                // If untrusted source is used without validation, warn
                if (!hasValidation && initStr.contains("getString|getStringExtra|textValue|textFieldValue".toRegex())) {
                    context.report(
                        issue = ISSUE,
                        location = context.getLocation(initializer),
                        message = "User input should be validated: call .trim(), .isNotEmpty(), or .matches() " +
                            "to prevent XSS and injection attacks."
                    )
                }
            }
        }
    }
}
