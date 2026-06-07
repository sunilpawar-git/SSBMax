package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UCallExpression

/**
 * Custom Lint Detector - Sensitive Data in Logs
 *
 * Detects logging of PII (Personally Identifiable Information) and secrets:
 * - userId, email, authToken, password, phone
 * - Any API keys or credentials
 *
 * Violations detected:
 * - Log.d/i/w/e with PII patterns
 * - println(), System.out.println() with PII
 * - Timber.d/i/w/e with PII
 * - Pattern matching: if log message contains userId, email, authToken, etc.
 *
 * Example violation:
 * ```kotlin
 * Log.d("TAG", "User: $userId")                    // ❌ ERROR - userId leaked
 * println("Auth token: $authToken")                // ❌ ERROR - token leaked
 * Timber.d("Email is $email")                      // ❌ ERROR - PII leaked
 * ```
 *
 * Correct usage:
 * ```kotlin
 * // 1. Use ErrorLogger (automatically sanitizes)
 * ErrorLogger.log(e, "User operation failed")
 *
 * // 2. Generic messages without PII
 * Log.d("TAG", "Operation succeeded")
 *
 * // 3. Non-PII fields only
 * Log.d("TAG", "State: ${state.status}")  // if status is non-sensitive
 * ```
 */
class SensitiveDataInLogsDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ISSUE_ID = "SensitiveDataInLogs"
        private const val ISSUE_DESCRIPTION = "Sensitive data logged"
        private const val ISSUE_EXPLANATION = """
            Logging PII (Personally Identifiable Information) or credentials is a security risk:
            - Logs are often stored in cleartext
            - Can be exposed in crash reports or analytics
            - Violates privacy regulations (GDPR, etc.)
            
            Detected sensitive patterns:
            - userId, user_id, uid
            - email, emailAddress, e_mail
            - authToken, auth_token, token
            - password, passwd, pwd
            - phoneNumber, phone, phone_number
            - apiKey, api_key, secret, credentials
            
            Correct approaches:
            1. Use ErrorLogger.log(exception, "message") — automatically sanitizes
            2. Log generic messages without PII
            3. Log non-sensitive metadata only (e.g., operation status, not user IDs)
            
            If you need to debug with user context, use ErrorLogger's context methods.
        """

        val ISSUE: Issue = Issue.create(
            id = ISSUE_ID,
            briefDescription = ISSUE_DESCRIPTION,
            explanation = ISSUE_EXPLANATION,
            category = Category.SECURITY,
            priority = 10,
            severity = Severity.ERROR,
            implementation = Implementation(
                SensitiveDataInLogsDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    private val sensitivePatternsRegex = listOf(
        Regex("user[_]?id|uid", RegexOption.IGNORE_CASE),
        Regex("email|e[_]?mail|email[_]?address", RegexOption.IGNORE_CASE),
        Regex("auth[_]?token|access[_]?token|bearer", RegexOption.IGNORE_CASE),
        Regex("password|passwd|pwd|pass\\b", RegexOption.IGNORE_CASE),
        Regex("phone|phone[_]?number", RegexOption.IGNORE_CASE),
        Regex("api[_]?key|secret[_]?key|api[_]?secret", RegexOption.IGNORE_CASE),
        Regex("credit[_]?card|ssn|social[_]?security", RegexOption.IGNORE_CASE)
    )

    private val logMethods = listOf(
        "d", "i", "w", "e", "println", "print"  // android.util.Log and System.out
    )

    override fun getApplicableMethodNames() = logMethods

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                val methodName = node.methodName ?: return

                // Check if this is a logging call
                val methodStr = node.asSourceString()
                if (!methodStr.contains("Log\\.|println|print|Timber\\.".toRegex())) return

                // Get all arguments as strings
                node.valueArguments.forEach { arg ->
                    val argStr = arg.asSourceString()

                    // Check if argument contains sensitive patterns
                    val hasSensitiveData = sensitivePatternsRegex.any { pattern ->
                        pattern.containsMatchIn(argStr)
                    }

                    if (hasSensitiveData) {
                        context.report(
                            issue = ISSUE,
                            location = context.getLocation(arg),
                            message = "Sensitive data detected in log message. " +
                                "Do not log PII (userId, email, authToken, password, etc). " +
                                "Use ErrorLogger.log() instead, which automatically sanitizes output."
                        )
                    }
                }
            }
        }
    }
}
