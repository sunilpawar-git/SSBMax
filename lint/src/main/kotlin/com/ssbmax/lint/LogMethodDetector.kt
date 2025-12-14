package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UQualifiedReferenceExpression

/**
 * Custom Lint Detector - android.util.Log Anti-Pattern
 *
 * Detects direct usage of `android.util.Log` methods which bypass ErrorLogger.
 * This prevents proper crash reporting and consistent error handling.
 *
 * Problems with direct Log usage:
 * - No integration with Firebase Crashlytics (production error tracking)
 * - Bypasses centralized error handling (ErrorLogger)
 * - Missing context information (userId, testType, etc.)
 * - No severity level control
 * - Not searchable/filterable in production
 * - Inconsistent with architecture patterns
 *
 * Example violations:
 * ```kotlin
 * // ❌ WRONG - Direct Log.e() usage
 * try {
 *     submitTest()
 * } catch (e: Exception) {
 *     Log.e(TAG, "Test submission failed", e)  // No Crashlytics integration
 *     android.util.Log.e("MyClass", "Error", e)  // Same issue
 * }
 *
 * // ❌ WRONG - Other Log methods
 * Log.d(TAG, "Debug message")  // Should use proper logging
 * Log.w(TAG, "Warning")        // Should use ErrorLogger
 * ```
 *
 * Correct alternatives:
 * ```kotlin
 * // ✅ CORRECT - Use ErrorLogger for errors/exceptions
 * try {
 *     submitTest()
 * } catch (e: Exception) {
 *     ErrorLogger.log(e, "Test submission failed")
 * }
 *
 * // ✅ CORRECT - With test-specific context
 * try {
 *     loadTest()
 * } catch (e: Exception) {
 *     ErrorLogger.logTestError(
 *         throwable = e,
 *         description = "Failed to load SDT test",
 *         testType = "SDT",
 *         userId = currentUserId
 *     )
 * }
 *
 * // ✅ CORRECT - With additional context
 * try {
 *     trackAnalytics()
 * } catch (e: Exception) {
 *     ErrorLogger.log(
 *         throwable = e,
 *         description = "Failed to track test started",
 *         context = mapOf("testType" to testType, "userId" to userId)
 *     )
 * }
 *
 * // ✅ CORRECT - For informational messages
 * ErrorLogger.log(
 *     message = "User started TAT test",
 *     context = mapOf("userId" to userId, "testId" to testId),
 *     severity = ErrorLogger.Severity.INFO
 * )
 * ```
 *
 * This lint rule has ERROR severity and will fail the build, preventing
 * direct Log usage from being committed.
 */
class LogMethodDetector : Detector(), SourceCodeScanner {

    init {
        println("LogMethodDetector: Initialized")
    }

    companion object {
        private const val ISSUE_ID = "DirectLogUsage"
        private const val ISSUE_DESCRIPTION = "Direct android.util.Log usage is not allowed"
        private const val ISSUE_EXPLANATION = """
            Using `android.util.Log` methods directly is not allowed in SSBMax.

            Problems with direct Log usage:
            • No integration with Firebase Crashlytics (production error tracking)
            • Bypasses centralized error handling (ErrorLogger)
            • Missing context information (userId, testType, screenName, etc.)
            • No severity level control
            • Not searchable/filterable in production logs
            • Inconsistent with architecture patterns
            • No automatic tag inference

            ❌ WRONG - Direct Log usage:
            ```kotlin
            try {
                submitTest()
            } catch (e: Exception) {
                Log.e(TAG, "Test submission failed", e)  // Bad: No Crashlytics
                android.util.Log.e("MyClass", "Error", e)  // Bad: Same issue
            }

            Log.d(TAG, "Debug message")  // Bad: Should use proper logging
            Log.w(TAG, "Warning")        // Bad: Should use ErrorLogger
            ```

            ✅ CORRECT - Use ErrorLogger:
            ```kotlin
            try {
                submitTest()
            } catch (e: Exception) {
                // Logs to Logcat AND sends to Crashlytics
                ErrorLogger.log(e, "Test submission failed")
            }
            ```

            ✅ CORRECT - With context:
            ```kotlin
            try {
                submitTest()
            } catch (e: Exception) {
                ErrorLogger.logTestError(
                    throwable = e,
                    description = "Test submission failed",
                    testType = "TAT",
                    userId = currentUserId
                )
            }
            ```

            ✅ CORRECT - With additional context:
            ```kotlin
            try {
                loadUserData()
            } catch (e: Exception) {
                ErrorLogger.log(
                    throwable = e,
                    description = "Failed to load user data",
                    context = mapOf(
                        "userId" to userId,
                        "screenName" to "ProfileScreen"
                    )
                )
            }
            ```

            ✅ CORRECT - For informational messages:
            ```kotlin
            ErrorLogger.log(
                message = "User started TAT test",
                context = mapOf("userId" to userId, "testId" to testId),
                severity = ErrorLogger.Severity.INFO
            )
            ```

            ErrorLogger benefits:
            • Logs to Android Logcat (visible in adb logcat)
            • Sends errors to Firebase Crashlytics (production monitoring)
            • Supports severity levels (DEBUG, INFO, WARNING, ERROR, FATAL)
            • Attaches context (userId, testType, screenName, etc.)
            • Automatic tag inference from call site
            • Fail-safe: never throws exceptions
            • Works in both debug and release builds

            See: app/src/main/kotlin/com/ssbmax/utils/ErrorLogger.kt
        """

        val ISSUE: Issue = Issue.create(
            id = ISSUE_ID,
            briefDescription = ISSUE_DESCRIPTION,
            explanation = ISSUE_EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 9,  // High priority - serious architecture violation
            severity = Severity.ERROR,  // Fail the build
            implementation = Implementation(
                LogMethodDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        // Log methods that should not be used directly
        private val FORBIDDEN_LOG_METHODS = setOf(
            "v", "d", "i", "w", "e", "wtf"  // verbose, debug, info, warning, error, wtf
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return FORBIDDEN_LOG_METHODS.toList()
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                // TEMP: Report ANY call to method "e" for debugging
                if (node.methodName == "e") {
                    println("LogMethodDetector: Found method 'e' in ${context.file.name}")
                    val receiver = node.receiver
                    val receiverText = receiver?.sourcePsi?.text ?: "null"
                    println("LogMethodDetector: Receiver text: '$receiverText'")
                    val receiverType = receiver?.getExpressionType()?.canonicalText ?: "null"
                    println("LogMethodDetector: Receiver type: '$receiverType'")

                    // Report ANY call to "e" method for now
                    context.report(
                        issue = ISSUE,
                        location = context.getCallLocation(
                            call = node,
                            includeReceiver = true,
                            includeArguments = false
                        ),
                        message = "Found method 'e' call - debugging",
                        quickfixData = null
                    )
                }
                val location = context.getCallLocation(
                    call = node,
                    includeReceiver = true,
                    includeArguments = false
                )

                context.report(
                    issue = ISSUE,
                    location = location,
                    message = "Do not use `android.util.Log.${node.methodName}()`. Use `ErrorLogger.log()` instead.",
                    quickfixData = createQuickFix(node)
                )
            }
        }
    }

    /**
     * Check if the receiver is android.util.Log
     */
    private fun isLogClassReceiver(receiver: org.jetbrains.uast.UExpression?): Boolean {
        if (receiver == null) return false

        // Handle qualified references like android.util.Log.e()
        if (receiver is UQualifiedReferenceExpression) {
            val receiverText = receiver.sourcePsi?.text ?: ""
            return receiverText == "android.util.Log" || receiverText == "Log"
        }

        // Handle simple references (when Log is imported)
        val type = receiver.getExpressionType()?.canonicalText
        return type == "android.util.Log"
    }

    /**
     * Create a quick fix to replace Log method with ErrorLogger
     */
    private fun createQuickFix(node: UCallExpression): LintFix? {
        val methodName = node.methodName
        val args = node.valueArguments

        // Determine the replacement based on the Log method and arguments
        val replacement = when {
            // Log.e(TAG, message, throwable)
            methodName == "e" && args.size >= 2 -> {
                val throwableArg = args.last().sourcePsi?.text ?: "e"
                "ErrorLogger.log($throwableArg, \"TODO: Add description\")"
            }
            // Log.e(TAG, message) - no throwable
            methodName == "e" && args.size >= 1 -> {
                val messageArg = args.last().sourcePsi?.text ?: "\"Error\""
                "ErrorLogger.log(Exception($messageArg), \"TODO: Add description\")"
            }
            // Other Log methods (d, i, w, etc.)
            else -> {
                val messageArg = args.lastOrNull()?.sourcePsi?.text ?: "\"Message\""
                "ErrorLogger.log(message = $messageArg, severity = ErrorLogger.Severity.${methodName?.uppercase() ?: "INFO"})"
            }
        }

        return fix()
            .name("Replace with ErrorLogger")
            .replace()
            .text(node.sourcePsi?.text ?: "Log.${methodName}()")
            .with(replacement)
            .shortenNames()
            .reformat(true)
            .build()
    }
}