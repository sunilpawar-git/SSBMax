package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UVariable

/**
 * Custom Lint Detector - Hardcoded API Keys
 *
 * Detects hardcoded API keys (Gemini, Sarvam, ElevenLabs, etc.) in source code.
 * API keys must NEVER be hardcoded; they must come from local.properties or Firebase Config.
 *
 * Violations detected:
 * - GEMINI_API_KEY, SARVAM_API_KEY, ELEVENLABS_API_KEY hardcoded
 * - Any string starting with "sk_" or "sk-" (OpenAI-style keys)
 * - Variable names containing "api_key", "secret_key" with string literals
 *
 * Example violation:
 * ```kotlin
 * const val GEMINI_API_KEY = "sk-proj-abc123def456"  // ❌ ERROR
 * val apiKey = "sarvam-key-xyz"                       // ❌ ERROR
 * ```
 *
 * Correct usage:
 * ```kotlin
 * // 1. BuildConfig approach (values from local.properties)
 * val geminiKey = BuildConfig.GEMINI_API_KEY
 *
 * // 2. Firebase Remote Config
 * val apiKey = remoteConfig.getString("gemini_api_key")
 * ```
 */
class HardcodedApiKeyDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ISSUE_ID = "HardcodedApiKey"
        private const val ISSUE_DESCRIPTION = "Hardcoded API key detected"
        private const val ISSUE_EXPLANATION = """
            API keys should never be hardcoded in source code. Instead:
            
            1. Use local.properties (for local development):
               Add to local.properties: GEMINI_API_KEY=your_key_here
               Access via: BuildConfig.GEMINI_API_KEY
            
            2. Use Firebase Remote Config (for production):
               Configure key in Firebase Console
               Access via: remoteConfig.getString("gemini_api_key")
            
            This prevents accidental credential leaks and makes key rotation easier.
            
            Detected patterns:
            - GEMINI_API_KEY, SARVAM_API_KEY, ELEVENLABS_API_KEY = "..."
            - String literals starting with "sk_" or "sk-" (OpenAI-style keys)
            - Variable names containing "api_key" or "secret_key" with string values
        """

        val ISSUE: Issue = Issue.create(
            id = ISSUE_ID,
            briefDescription = ISSUE_DESCRIPTION,
            explanation = ISSUE_EXPLANATION,
            category = Category.SECURITY,
            priority = 10,
            severity = Severity.ERROR,
            implementation = Implementation(
                HardcodedApiKeyDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    private val apiKeyPatterns = listOf(
        "GEMINI_API_KEY",
        "SARVAM_API_KEY",
        "ELEVENLABS_API_KEY",
        "API_KEY",
        "SECRET_KEY",
        "API_SECRET"
    )

    private val secretPrefixes = listOf("sk_", "sk-")

    override fun getApplicableUastTypes() = listOf(UVariable::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitVariable(node: UVariable) {
                val varName = node.name ?: return
                val initializer = node.uastInitializer ?: return

                // Check if variable name matches API key patterns
                val isApiKeyVar = apiKeyPatterns.any { varName.contains(it, ignoreCase = true) }

                // Check if initializer is a string literal
                if (initializer is ULiteralExpression) {
                    val value = initializer.value
                    if (value is String) {
                        // Violation: API key variable with string literal
                        if (isApiKeyVar) {
                            context.report(
                                issue = ISSUE,
                                location = context.getLocation(initializer),
                                message = "Hardcoded API key detected in variable '$varName'. " +
                                    "Use local.properties or Firebase Remote Config instead."
                            )
                            return
                        }

                        // Violation: String literal starting with "sk_" or "sk-"
                        val hasSecretPrefix = secretPrefixes.any { value.startsWith(it) }
                        if (hasSecretPrefix && value.length > 20) {
                            // Only flag if it looks like a real key (length > 20)
                            context.report(
                                issue = ISSUE,
                                location = context.getLocation(initializer),
                                message = "Hardcoded API key pattern detected (${value.take(5)}...). " +
                                    "Store API keys in local.properties or Firebase Remote Config."
                            )
                        }
                    }
                }
            }
        }
    }
}
