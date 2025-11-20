package com.ssbmax.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression

/**
 * Custom Lint Detector - printStackTrace() Anti-Pattern
 *
 * Detects usage of `e.printStackTrace()` which is an anti-pattern because:
 * - Stack traces go to System.err, not Android Logcat
 * - No integration with crash reporting (Firebase Crashlytics)
 * - Can't filter by severity or tag
 * - Not searchable in production logs
 * - No context information attached
 * - Makes debugging harder in production
 *
 * Example violation:
 * ```kotlin
 * try {
 *     riskyOperation()
 * } catch (e: Exception) {
 *     e.printStackTrace()  // ❌ ERROR
 * }
 * ```
 *
 * Correct alternatives:
 * ```kotlin
 * // 1. Use ErrorLogger for automatic Crashlytics integration
 * try {
 *     riskyOperation()
 * } catch (e: Exception) {
 *     ErrorLogger.log(e, "Failed to perform risky operation")
 * }
 *
 * // 2. With context for better debugging
 * try {
 *     submitTest()
 * } catch (e: Exception) {
 *     ErrorLogger.logTestError(
 *         throwable = e,
 *         description = "Test submission failed",
 *         testType = "TAT",
 *         userId = userId
 *     )
 * }
 * ```
 *
 * This lint rule has ERROR severity and will fail the build, preventing
 * printStackTrace() from being committed.
 */
class PrintStackTraceDetector : Detector(), SourceCodeScanner {

    companion object {
        private const val ISSUE_ID = "PrintStackTraceUsage"
        private const val ISSUE_DESCRIPTION = "Use of printStackTrace() is not allowed"
        private const val ISSUE_EXPLANATION = """
            Using `e.printStackTrace()` is an anti-pattern in Android development.

            Problems with printStackTrace():
            • Writes to System.err instead of Android Logcat
            • Not visible in adb logcat without special setup
            • No integration with crash reporting (Firebase Crashlytics)
            • Can't filter by tag or severity level
            • No context information (userId, testType, etc.)
            • Makes debugging production issues nearly impossible
            • Performance impact (stack trace generation on main thread)

            ❌ WRONG:
            ```kotlin
            try {
                submitTest()
            } catch (e: Exception) {
                e.printStackTrace()  // Bad: Goes to System.err
            }
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

            ErrorLogger benefits:
            • Logs to Android Logcat (visible in adb logcat)
            • Sends non-fatal errors to Firebase Crashlytics (production monitoring)
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
            priority = 9,  // High priority - this is a serious issue
            severity = Severity.ERROR,  // Fail the build
            implementation = Implementation(
                PrintStackTraceDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        private const val PRINT_STACK_TRACE_METHOD = "printStackTrace"
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf(PRINT_STACK_TRACE_METHOD)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                // Check if this is a call to printStackTrace()
                if (node.methodName != PRINT_STACK_TRACE_METHOD) return

                // Check if it's called on an exception/throwable
                val receiver = node.receiver
                if (!isExceptionReceiver(receiver)) return

                // Report the violation
                val location = context.getCallLocation(
                    call = node,
                    includeReceiver = true,
                    includeArguments = false
                )

                context.report(
                    issue = ISSUE,
                    location = location,
                    message = "Do not use `printStackTrace()`. Use `ErrorLogger.log(e, \"description\")` instead.",
                    quickfixData = createQuickFix(node)
                )
            }
        }
    }

    /**
     * Check if the receiver is likely an exception/throwable
     */
    private fun isExceptionReceiver(receiver: UExpression?): Boolean {
        if (receiver == null) return false

        // Check the type of the receiver
        val type = receiver.getExpressionType()?.canonicalText ?: return false

        // Common exception types
        return type.contains("Exception") ||
                type.contains("Error") ||
                type.contains("Throwable") ||
                type == "java.lang.Throwable" ||
                type == "kotlin.Throwable"
    }

    /**
     * Create a quick fix to replace printStackTrace() with ErrorLogger.log()
     */
    private fun createQuickFix(node: UCallExpression): LintFix? {
        val receiver = node.receiver ?: return null
        val receiverText = receiver.sourcePsi?.text ?: "e"

        // Create replacement suggestion
        return fix()
            .name("Replace with ErrorLogger.log()")
            .replace()
            .text(node.sourcePsi?.text ?: "$receiverText.printStackTrace()")
            .with("ErrorLogger.log($receiverText, \"TODO: Add description\")")
            .shortenNames()
            .reformat(true)
            .build()
    }
}
