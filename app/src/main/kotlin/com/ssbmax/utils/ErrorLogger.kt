package com.ssbmax.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ssbmax.BuildConfig

/**
 * Centralized Error Logging Utility
 *
 * Provides consistent error logging across the app with integration to:
 * - Firebase Crashlytics (production non-fatal errors)
 * - Android Logcat (debug builds)
 *
 * Usage:
 * ```kotlin
 * try {
 *     riskyOperation()
 * } catch (e: Exception) {
 *     ErrorLogger.log(e, "Failed to load user data")
 *     // or with context
 *     ErrorLogger.log(e, "Failed to submit test", mapOf(
 *         "testType" to "TAT",
 *         "userId" to userId
 *     ))
 * }
 * ```
 *
 * Architecture Enforcement:
 * - DO NOT use e.printStackTrace() (caught by PrintStackTraceDetector lint rule)
 * - DO NOT use android.util.Log.e() directly in ViewModels
 * - DO use ErrorLogger.log() for all error logging
 *
 * @see PrintStackTraceDetector Custom lint rule that enforces this pattern
 */
object ErrorLogger {

    /**
     * Error severity levels
     */
    enum class Severity {
        DEBUG,    // Development info only
        INFO,     // Informational, not an error
        WARNING,  // Potential issue, recoverable
        ERROR,    // Error occurred, handled gracefully
        FATAL     // Critical error, app may be unstable
    }

    /**
     * Log an error with optional description and context
     *
     * @param throwable The exception/error to log
     * @param description Human-readable description of what failed
     * @param context Additional context (userId, testType, screenName, etc.)
     * @param severity Error severity level (defaults to ERROR)
     */
    fun log(
        throwable: Throwable,
        description: String,
        context: Map<String, String> = emptyMap(),
        severity: Severity = Severity.ERROR
    ) {
        val tag = inferTag()
        val message = buildMessage(description, throwable, context)

        // Log to Android Logcat
        when (severity) {
            Severity.DEBUG -> Log.d(tag, message, throwable)
            Severity.INFO -> Log.i(tag, message, throwable)
            Severity.WARNING -> Log.w(tag, message, throwable)
            Severity.ERROR -> Log.e(tag, message, throwable)
            Severity.FATAL -> Log.wtf(tag, message, throwable)
        }

        // Send to Crashlytics in production (non-debug builds)
        if (!BuildConfig.DEBUG && severity >= Severity.WARNING) {
            logToCrashlytics(throwable, description, context, severity)
        }
    }

    /**
     * Log a message without an exception
     *
     * @param message The message to log
     * @param context Additional context
     * @param severity Severity level (defaults to INFO)
     */
    fun log(
        message: String,
        context: Map<String, String> = emptyMap(),
        severity: Severity = Severity.INFO
    ) {
        val tag = inferTag()
        val fullMessage = if (context.isEmpty()) {
            message
        } else {
            buildMessage(message, null, context)
        }

        // Log to Android Logcat
        when (severity) {
            Severity.DEBUG -> Log.d(tag, fullMessage)
            Severity.INFO -> Log.i(tag, fullMessage)
            Severity.WARNING -> Log.w(tag, fullMessage)
            Severity.ERROR -> Log.e(tag, fullMessage)
            Severity.FATAL -> Log.wtf(tag, fullMessage)
        }

        // Send to Crashlytics in production for warnings and above
        if (!BuildConfig.DEBUG && severity >= Severity.WARNING) {
            logToCrashlytics(
                throwable = Exception(fullMessage),
                description = message,
                context = context,
                severity = severity
            )
        }
    }

    /**
     * Build a formatted error message with context
     */
    private fun buildMessage(
        description: String,
        throwable: Throwable?,
        context: Map<String, String>
    ): String {
        val builder = StringBuilder()
        builder.append(description)

        if (throwable != null) {
            builder.append("\n")
            builder.append("   Error type: ${throwable.javaClass.simpleName}")
            builder.append("\n")
            builder.append("   Error message: ${throwable.message}")
        }

        if (context.isNotEmpty()) {
            builder.append("\n")
            builder.append("   Context:")
            context.forEach { (key, value) ->
                builder.append("\n")
                builder.append("     - $key: $value")
            }
        }

        return builder.toString()
    }

    /**
     * Send error to Firebase Crashlytics
     */
    private fun logToCrashlytics(
        throwable: Throwable,
        description: String,
        context: Map<String, String>,
        severity: Severity
    ) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()

            // Set custom keys for context
            crashlytics.setCustomKey("error_description", description)
            crashlytics.setCustomKey("severity", severity.name)

            context.forEach { (key, value) ->
                crashlytics.setCustomKey(key, value)
            }

            // Log the exception
            crashlytics.recordException(throwable)
        } catch (e: Exception) {
            // Fallback if Crashlytics fails - just log to console
            Log.e("ErrorLogger", "Failed to log to Crashlytics", e)
        }
    }

    /**
     * Infer the log tag from the call stack
     * Returns the calling class name (e.g., "TATTestViewModel")
     */
    private fun inferTag(): String {
        val stackTrace = Thread.currentThread().stackTrace

        // Find the first stack element that's not ErrorLogger
        for (element in stackTrace) {
            val className = element.className
            if (!className.contains("ErrorLogger") &&
                !className.startsWith("java.lang") &&
                !className.startsWith("android.") &&
                !className.contains("VMStack") &&
                !className.contains("Thread")) {

                // Extract simple class name
                return className.substringAfterLast('.')
            }
        }

        return "SSBMax"
    }

    /**
     * Convenience function for logging with user context
     *
     * @param throwable The exception to log
     * @param description Error description
     * @param userId Current user ID (if authenticated)
     * @param additionalContext Any additional context
     */
    fun logWithUser(
        throwable: Throwable,
        description: String,
        userId: String?,
        additionalContext: Map<String, String> = emptyMap()
    ) {
        val context = if (userId != null) {
            additionalContext + ("userId" to userId)
        } else {
            additionalContext
        }
        log(throwable, description, context)
    }

    /**
     * Convenience function for logging test-related errors
     *
     * @param throwable The exception to log
     * @param description Error description
     * @param testType Type of test (TAT, WAT, SRT, etc.)
     * @param userId Current user ID (if authenticated)
     */
    fun logTestError(
        throwable: Throwable,
        description: String,
        testType: String,
        userId: String? = null
    ) {
        val context = mutableMapOf("testType" to testType)
        if (userId != null) {
            context["userId"] = userId
        }
        log(throwable, description, context)
    }
}
